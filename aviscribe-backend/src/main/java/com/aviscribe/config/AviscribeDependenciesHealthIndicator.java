package com.aviscribe.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("aviscribeDependencies")
public class AviscribeDependenciesHealthIndicator implements HealthIndicator {

    @Value("${aviscribe.file.upload-path}")
    private String uploadPath;
    @Value("${aviscribe.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;
    @Value("${aviscribe.downloader.yt-dlp-path:yt-dlp}")
    private String ytDlpPath;
    @Value("${aviscribe.stt.app-key:}")
    private String sttAppKey;
    @Value("${aviscribe.stt.access-key-id:}")
    private String aliyunAccessKeyId;
    @Value("${aviscribe.stt.access-key-secret:}")
    private String aliyunAccessKeySecret;
    @Value("${aviscribe.llm.api-key:}")
    private String llmApiKey;

    @Override
    public Health health() {
        List<String> unavailable = new ArrayList<>();
        if (!isWritableDirectory(uploadPath)) unavailable.add("upload-directory");
        if (!commandWorks(ffmpegPath, "-version")) unavailable.add("ffmpeg");
        if (!commandWorks(ytDlpPath, "--version")) unavailable.add("yt-dlp");
        if (!StringUtils.hasText(sttAppKey)
                || !StringUtils.hasText(aliyunAccessKeyId)
                || !StringUtils.hasText(aliyunAccessKeySecret)) {
            unavailable.add("aliyun-stt-credentials");
        }
        if (!StringUtils.hasText(llmApiKey)) unavailable.add("llm-credentials");

        if (unavailable.isEmpty()) {
            return Health.up().build();
        }
        return Health.down().withDetail("unavailable", unavailable).build();
    }

    private boolean isWritableDirectory(String configuredPath) {
        try {
            Path path = Path.of(configuredPath).toAbsolutePath().normalize();
            Files.createDirectories(path);
            return Files.isDirectory(path) && Files.isWritable(path);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean commandWorks(String command, String versionArgument) {
        if (!StringUtils.hasText(command)) {
            return false;
        }
        try {
            Process process = new ProcessBuilder(command, versionArgument)
                    .redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD)
                    .start();
            if (!process.waitFor(3, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (IOException ex) {
            return false;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
