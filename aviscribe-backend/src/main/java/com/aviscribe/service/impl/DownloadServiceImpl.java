package com.aviscribe.service.impl;

import com.aviscribe.service.DownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class DownloadServiceImpl implements DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadServiceImpl.class);

    /**
     * 视频本地保存根路径，可与上传目录共用或单独配置，如：D:/aviscribe/uploads
     */
    @Value("${aviscribe.file.upload-path}")
    private String uploadRootPath;

    /**
     * yt-dlp 可执行路径（命令或绝对路径），用于解析主流视频网站网页链接
     */
    @Value("${aviscribe.downloader.yt-dlp-path:yt-dlp}")
    private String ytDlpPath;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    @Override
    public String download(String url) throws Exception {
        log.info("开始下载远程视频: {}", url);

        if (isTypicalWebPageUrl(url)) {
            // 对主流视频平台网页链接，使用 yt-dlp 下载真实视频文件
            return downloadWithYtDlp(url);
        }

        return downloadDirect(url);
    }

    /**
     * 直接通过 HTTP GET 下载视频直链
     */
    private String downloadDirect(String url) throws Exception {
        // 确保根目录存在
        Path root = Paths.get(uploadRootPath);
        if (Files.notExists(root)) {
            Files.createDirectories(root);
        }

        // 简单根据 URL 生成文件名，默认 mp4 后缀
        String fileName = "url-video-" + System.currentTimeMillis() + ".mp4";
        Path target = root.resolve(fileName);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<InputStream> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

        int status = response.statusCode();
        if (status != 200) {
            throw new IllegalStateException("下载失败, HTTP 状态码: " + status);
        }

        // 基础校验：Content-Type 必须是视频类型，避免把 HTML 网页当成视频保存
        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("")
                .toLowerCase();
        if (!contentType.isEmpty() && !contentType.startsWith("video/")) {
            // 常见情况: text/html; charset=utf-8 等，表示其实是网页/错误页
            throw new IllegalStateException("URL 响应的 Content-Type 非视频类型: " + contentType);
        }

        // 可选: 粗略检查体积，过小的响应大概率不是正常视频文件
        long contentLength = response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1L);
        if (contentLength > 0 && contentLength < 1024) { // <1KB
            throw new IllegalStateException("URL 响应体过小，疑似不是有效视频，Content-Length=" + contentLength);
        }

        try (InputStream in = response.body();
             OutputStream out = Files.newOutputStream(target)) {
            in.transferTo(out);
        } catch (IOException e) {
            // 失败时清理半成品文件
            try {
                Files.deleteIfExists(target);
            } catch (IOException ignore) { }
            throw e;
        }

        String localPath = target.toAbsolutePath().toString();
        log.info("远程视频下载完成(直链), 本地路径: {}", localPath);
        return localPath;
    }

    /**
     * 使用本机 yt-dlp 程序解析并下载主流视频网站网页链接
     */
    private String downloadWithYtDlp(String url) throws Exception {
        // 确保根目录存在
        Path root = Paths.get(uploadRootPath);
        if (Files.notExists(root)) {
            Files.createDirectories(root);
        }

        // 生成随机文件名前缀，避免并发冲突
        String baseName = "yt-" + UUID.randomUUID();
        // yt-dlp 的 -o 模板支持 %(ext)s 作为后缀
        Path outTemplate = root.resolve(baseName + ".%(ext)s");

        // 加上 --proxy ""，与用户在命令行中的测试行为保持一致
        ProcessBuilder pb = new ProcessBuilder(
                ytDlpPath,
                "--proxy", "",
                "-o", outTemplate.toString(),
                url
        );
        pb.redirectErrorStream(true);

        log.info("使用 yt-dlp 下载视频, 命令: {} --proxy {} -o {} {}", ytDlpPath, "", outTemplate, url);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new IllegalStateException("启动 yt-dlp 失败，请确认已安装并在 PATH 中，或在配置中正确设置 aviscribe.downloader.yt-dlp-path", e);
        }

        // 读取输出日志，便于调试
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append(System.lineSeparator());
                log.info("yt-dlp> {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("yt-dlp 退出码: {}, 输出:\n{}", exitCode, output);
            throw new IllegalStateException("yt-dlp 下载失败, exitCode=" + exitCode + ", output=" + output);
        }

        // yt-dlp 完成后，需要找到实际生成的文件
        try {
            // 由于我们使用了固定前缀 baseName，可以在目录下按此前缀查找
            try (var stream = Files.list(root)) {
                return stream
                        .filter(p -> p.getFileName().toString().startsWith(baseName + "."))
                        .findFirst()
                        .map(p -> {
                            String lp = p.toAbsolutePath().toString();
                            log.info("yt-dlp 下载完成, 本地路径: {}", lp);
                            return lp;
                        })
                        .orElseThrow(() -> new IllegalStateException("yt-dlp 执行成功但未找到输出文件"));
            }
        } catch (IOException e) {
            throw new IllegalStateException("扫描 yt-dlp 输出文件失败", e);
        }
    }

    /**
     * 粗略判断是否为常见“网页播放链接”，而不是直接可下载的视频文件 URL。
     * 这些站点的视频实际地址通常需要专门解析库（例如 yt-dlp）才能拿到。
     */
    private boolean isTypicalWebPageUrl(String url) {
        if (url == null) {
            return false;
        }
        String lower = url.toLowerCase();
        return lower.contains("bilibili.com/video/")
                || lower.contains("youtube.com/watch")
                || lower.contains("youtu.be/")
                || lower.contains("v.qq.com/")
                || lower.contains("ixigua.com/")
                || lower.contains("douyin.com/")
                || lower.contains("tiktok.com/");
    }
}
