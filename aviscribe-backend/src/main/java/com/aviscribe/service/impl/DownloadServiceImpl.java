package com.aviscribe.service.impl;

import com.aviscribe.security.UrlSafetyValidator;
import com.aviscribe.security.UrlSafetyValidator.ValidatedUrl;
import com.aviscribe.service.DownloadService;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.DnsResolver;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class DownloadServiceImpl implements DownloadService {

    private static final Logger log = LoggerFactory.getLogger(DownloadServiceImpl.class);
    private static final int MAX_REDIRECTS = 3;
    private static final Set<String> YT_DLP_DOMAINS = Set.of(
            "bilibili.com", "youtube.com", "youtu.be", "v.qq.com",
            "ixigua.com", "douyin.com", "tiktok.com"
    );

    @Value("${aviscribe.file.upload-path}")
    private String uploadRootPath;
    @Value("${aviscribe.downloader.yt-dlp-path:yt-dlp}")
    private String ytDlpPath;
    @Value("${aviscribe.downloader.max-download-size-bytes:524288000}")
    private long maxDownloadSizeBytes;
    @Value("${aviscribe.downloader.connect-timeout-ms:10000}")
    private int connectTimeoutMs;
    @Value("${aviscribe.downloader.read-timeout-ms:30000}")
    private int readTimeoutMs;
    @Value("${aviscribe.downloader.process-timeout-seconds:1800}")
    private long processTimeoutSeconds;

    private final UrlSafetyValidator urlSafetyValidator;

    public DownloadServiceImpl(UrlSafetyValidator urlSafetyValidator) {
        this.urlSafetyValidator = urlSafetyValidator;
    }

    @Override
    public String download(String url) throws Exception {
        ValidatedUrl validatedUrl = urlSafetyValidator.validateAndResolve(url);
        URI validatedUri = validatedUrl.uri();
        log.info("开始下载远程媒体: scheme={}, host={}", validatedUri.getScheme(), validatedUri.getHost());
        if (isSupportedPlatform(validatedUri)) {
            return downloadWithYtDlp(validatedUri);
        }
        return downloadDirect(validatedUrl);
    }

    private String downloadDirect(ValidatedUrl initialUrl) throws Exception {
        Path root = ensureStorageRoot();
        Path target = root.resolve("url-media-" + UUID.randomUUID() + ".mp4");
        ValidatedUrl current = initialUrl;

        try {
            for (int redirect = 0; redirect <= MAX_REDIRECTS; redirect++) {
                try (CloseableHttpClient client = createPinnedHttpClient(current);
                     CloseableHttpResponse response = client.execute(new HttpGet(current.uri()))) {
                    int status = response.getCode();
                    if (isRedirect(status)) {
                        String location = response.getFirstHeader("Location") == null
                                ? null : response.getFirstHeader("Location").getValue();
                        if (location == null || redirect == MAX_REDIRECTS) {
                            throw new IllegalStateException("下载重定向次数过多或缺少 Location");
                        }
                        URI redirected = current.uri().resolve(location);
                        current = urlSafetyValidator.validateAndResolve(redirected.toString());
                        continue;
                    }
                    if (status != 200) {
                        throw new IllegalStateException("下载失败, HTTP 状态码: " + status);
                    }

                    HttpEntity entity = response.getEntity();
                    if (entity == null) {
                        throw new IllegalStateException("下载响应内容为空");
                    }
                    validateResponseHeaders(response, entity);
                    try (InputStream in = entity.getContent();
                         OutputStream out = Files.newOutputStream(target)) {
                        copyWithLimit(in, out, maxDownloadSizeBytes);
                    }
                    return target.toAbsolutePath().toString();
                }
            }
            throw new IllegalStateException("下载重定向次数过多");
        } catch (Exception ex) {
            Files.deleteIfExists(target);
            throw ex;
        }
    }

    private CloseableHttpClient createPinnedHttpClient(ValidatedUrl validatedUrl) {
        String expectedHost = validatedUrl.uri().getHost();
        InetAddress[] pinnedAddresses = validatedUrl.addresses().toArray(InetAddress[]::new);
        DnsResolver pinnedResolver = new DnsResolver() {
            @Override
            public InetAddress[] resolve(String host) throws UnknownHostException {
                ensureExpectedHost(host);
                return pinnedAddresses.clone();
            }

            @Override
            public String resolveCanonicalHostname(String host) throws UnknownHostException {
                ensureExpectedHost(host);
                return expectedHost;
            }

            private void ensureExpectedHost(String host) throws UnknownHostException {
                if (!expectedHost.equalsIgnoreCase(host)) {
                    throw new UnknownHostException("未校验的下载主机: " + host);
                }
            }
        };
        var connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDnsResolver(pinnedResolver)
                .build();
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
                .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
                .build();
        return HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig)
                .disableRedirectHandling()
                .build();
    }

    private void validateResponseHeaders(CloseableHttpResponse response, HttpEntity entity) {
        String contentType = response.getFirstHeader("Content-Type") == null
                ? null : response.getFirstHeader("Content-Type").getValue();
        if (contentType != null) {
            String normalized = contentType.toLowerCase(Locale.ROOT);
            if (!normalized.startsWith("video/") && !normalized.startsWith("audio/")
                    && !normalized.startsWith("application/octet-stream")) {
                throw new IllegalStateException("URL 响应不是受支持的音视频类型");
            }
        }
        long contentLength = entity.getContentLength();
        if (contentLength > maxDownloadSizeBytes) {
            throw new IllegalStateException("远程文件超过下载大小限制");
        }
    }

    private void copyWithLimit(InputStream in, OutputStream out, long limit) throws IOException {
        byte[] buffer = new byte[8192];
        long total = 0;
        int read;
        while ((read = in.read(buffer)) != -1) {
            total += read;
            if (total > limit) {
                throw new IOException("远程文件超过下载大小限制");
            }
            out.write(buffer, 0, read);
        }
    }

    private String downloadWithYtDlp(URI uri) throws Exception {
        Path root = ensureStorageRoot();
        String baseName = "yt-" + UUID.randomUUID();
        Path outTemplate = root.resolve(baseName + ".%(ext)s");
        long maxMegabytes = Math.max(1, maxDownloadSizeBytes / 1024 / 1024);
        int socketTimeoutSeconds = Math.max(1, readTimeoutMs / 1000);

        ProcessBuilder pb = new ProcessBuilder(
                ytDlpPath,
                "--no-playlist",
                "--playlist-items", "1",
                "--socket-timeout", String.valueOf(socketTimeoutSeconds),
                "--max-filesize", maxMegabytes + "M",
                "-o", outTemplate.toString(),
                uri.toString()
        );
        pb.redirectErrorStream(true);
        Process process;
        try {
            process = pb.start();
        } catch (IOException ex) {
            throw new IllegalStateException("启动 yt-dlp 失败，请确认程序已安装并正确配置", ex);
        }

        StringBuilder output = new StringBuilder();
        Thread outputReader = new Thread(() -> readProcessOutput(process, output), "yt-dlp-output");
        outputReader.setDaemon(true);
        outputReader.start();
        boolean completed = process.waitFor(processTimeoutSeconds, TimeUnit.SECONDS);
        if (!completed) {
            process.destroyForcibly();
            outputReader.join(1000);
            deleteMatchingFiles(root, baseName);
            throw new IllegalStateException("yt-dlp 下载超时");
        }
        outputReader.join(1000);
        if (process.exitValue() != 0) {
            deleteMatchingFiles(root, baseName);
            throw new IllegalStateException("yt-dlp 下载失败, exitCode=" + process.exitValue());
        }

        try (var stream = Files.list(root)) {
            Path downloaded = stream
                    .filter(path -> path.getFileName().toString().startsWith(baseName + "."))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("yt-dlp 执行成功但未找到输出文件"));
            if (Files.size(downloaded) > maxDownloadSizeBytes) {
                Files.deleteIfExists(downloaded);
                throw new IllegalStateException("远程文件超过下载大小限制");
            }
            return downloaded.toAbsolutePath().toString();
        }
    }

    private void readProcessOutput(Process process, StringBuilder output) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (output.length() < 16_384) {
                    output.append(line).append(System.lineSeparator());
                }
                log.debug("yt-dlp> {}", line);
            }
        } catch (IOException ex) {
            log.debug("读取 yt-dlp 输出失败", ex);
        }
    }

    private Path ensureStorageRoot() throws IOException {
        Path root = Paths.get(uploadRootPath).toAbsolutePath().normalize();
        Files.createDirectories(root);
        return root;
    }

    private void deleteMatchingFiles(Path root, String prefix) {
        try (var stream = Files.list(root)) {
            stream.filter(path -> path.getFileName().toString().startsWith(prefix + "."))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ignored) {
                            log.warn("清理下载临时文件失败: {}", path);
                        }
                    });
        } catch (IOException ignored) {
            log.warn("扫描下载临时文件失败: {}", root);
        }
    }

    private boolean isSupportedPlatform(URI uri) {
        String host = uri.getHost().toLowerCase(Locale.ROOT);
        return YT_DLP_DOMAINS.stream().anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
    }

    private boolean isRedirect(int status) {
        return status == 301 || status == 302 || status == 303 || status == 307 || status == 308;
    }
}
