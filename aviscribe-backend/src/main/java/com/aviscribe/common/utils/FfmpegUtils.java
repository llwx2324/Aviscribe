package com.aviscribe.common.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class FfmpegUtils {
    private static final Logger log = LoggerFactory.getLogger(FfmpegUtils.class);

    @Value("${aviscribe.ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${aviscribe.ffprobe.path:ffprobe}")
    private String ffprobePath;

    /**
     * 使用 ffmpeg 从视频中抽取音频（例如生成 16kHz 单声道 wav）
     */
    public boolean extractAudio(String inputVideoPath, String outputAudioPath) {
        log.info("Starting audio extraction for: {} -> {}", inputVideoPath, outputAudioPath);

        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y"); // 覆盖输出
        command.add("-i");
        command.add(inputVideoPath);
        command.add("-vn"); // 不要视频
        command.add("-acodec");
        command.add("pcm_s16le"); // 原始 PCM
        command.add("-ar");
        command.add("16000"); // 16k 采样率
        command.add("-ac");
        command.add("1"); // 单声道
        command.add(outputAudioPath);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true); // 将 stderr 合并到 stdout

        try {
            Process process = pb.start();
            // 读取输出日志，方便调试
            logProcessOutput(process.getInputStream());

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("ffmpeg exited with code {}", exitCode);
                return false;
            }
            log.info("Audio extraction completed successfully.");
            return true;
        } catch (IOException | InterruptedException e) {
            log.error("Error running ffmpeg", e);
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private void logProcessOutput(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("ffmpeg> {}", line);
            }
        }
    }

    /**
     * 通过 ffprobe 读取媒体总时长（秒）
     */
    public Integer getMediaDurationSeconds(String mediaPath) {
        if (mediaPath == null || mediaPath.isBlank()) {
            return null;
        }

        List<String> command = new ArrayList<>();
        command.add(ffprobePath);
        command.add("-v");
        command.add("error");
        command.add("-show_entries");
        command.add("format=duration");
        command.add("-of");
        command.add("default=noprint_wrappers=1:nokey=1");
        command.add(mediaPath);

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);

        try {
            Process process = pb.start();
            List<String> lines = collectProcessOutput(process.getInputStream());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.warn("ffprobe exited with code {} when probing {}", exitCode, mediaPath);
                return null;
            }
            for (String line : lines) {
                if (line == null) {
                    continue;
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    double seconds = Double.parseDouble(trimmed);
                    return (int) Math.round(seconds);
                } catch (NumberFormatException ex) {
                    log.debug("Unable to parse ffprobe output '{}' for {}", trimmed, mediaPath);
                }
            }
        } catch (IOException e) {
            log.error("Failed to probe media duration for {}", mediaPath, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("ffprobe duration process interrupted for {}", mediaPath, e);
        }
        return null;
    }

    private List<String> collectProcessOutput(InputStream inputStream) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}