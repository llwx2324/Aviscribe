package com.aviscribe.service.impl;

import com.aviscribe.common.enums.SourceType;
import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.common.utils.FfmpegUtils;
import com.aviscribe.entity.Task;
import com.aviscribe.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobProcessServiceImpl implements JobProcessService {

    private static final Logger log = LoggerFactory.getLogger(JobProcessServiceImpl.class);

    private final TaskService taskService;
    private final AudioExtractService audioExtractService;
    private final SpeechToTextService speechToTextService;
    private final TextFormatService textFormatService;
    private final DownloadService downloadService;
    private final FfmpegUtils ffmpegUtils;
    private final Set<Long> activeTaskIds = ConcurrentHashMap.newKeySet();

    public JobProcessServiceImpl(TaskService taskService,
                                 AudioExtractService audioExtractService,
                                 SpeechToTextService speechToTextService,
                                 TextFormatService textFormatService,
                                 DownloadService downloadService,
                                 FfmpegUtils ffmpegUtils) {
        this.taskService = taskService;
        this.audioExtractService = audioExtractService;
        this.speechToTextService = speechToTextService;
        this.textFormatService = textFormatService;
        this.downloadService = downloadService;
        this.ffmpegUtils = ffmpegUtils;
    }

    @Override
    @org.springframework.scheduling.annotation.Async("taskExecutor")
    public void processTask(Long taskId) {
        if (taskId == null || !activeTaskIds.add(taskId)) {
            log.info("[Task {}] 已在当前实例处理中，跳过重复调度", taskId);
            return;
        }
        log.info("[Task {}] 开始处理...", taskId);

        try {
            Task task = taskService.getById(taskId);
            if (task == null) {
                log.warn("[Task {}] 任务不存在，停止处理", taskId);
                return;
            }
            if (task.getTaskStatus() != null
                    && (task.getTaskStatus() == TaskStatus.COMPLETED.getCode()
                    || task.getTaskStatus() == TaskStatus.FAILED.getCode())) {
                log.info("[Task {}] 已处于终态，跳过处理", taskId);
                return;
            }
            // 1. 如果是 URL 源，先从网络下载到本地，填充 videoLocalPath
            if (task.getSourceType() == SourceType.URL.getValue() && !isUsableFile(task.getVideoLocalPath())) {
                taskService.updateTaskStatus(taskId, TaskStatus.DOWNLOADING);
                log.info("[Task {}] URL 下载阶段，开始下载远程视频...", taskId);

                String videoUrl = task.getVideoUrl();
                if (videoUrl == null || videoUrl.isBlank()) {
                    throw new IllegalArgumentException("任务缺少视频 URL");
                }

                String localPath = downloadService.download(videoUrl);
                task.setVideoLocalPath(localPath);
                taskService.updateById(task);
            }

            // 2. 音频提取
            String audioPath = task.getAudioLocalPath();
            if (!isUsableFile(audioPath)) {
                taskService.updateTaskStatus(taskId, TaskStatus.EXTRACTING_AUDIO);
                log.info("[Task {}] 正在提取音频...", taskId);
                audioPath = audioExtractService.extractAudio(task);
                task.setAudioLocalPath(audioPath);
                taskService.updateById(task);
            }
            updateDurationIfNeeded(task);

            // 3. 语音转文本
            String rawText = task.getRawText();
            if (!hasText(rawText)) {
                taskService.updateTaskStatus(taskId, TaskStatus.TRANSCRIBING);
                log.info("[Task {}] 正在语音识别...", taskId);
                rawText = speechToTextService.transcribe(task, audioPath);
                task.setRawText(rawText);
                taskService.updateById(task);
            }

            // 4. 文本排版
            String formattedText = task.getFormattedText();
            if (!hasText(formattedText)) {
                taskService.updateTaskStatus(taskId, TaskStatus.FORMATTING);
                log.info("[Task {}] 正在文本排版...", taskId);
                formattedText = textFormatService.format(task, rawText);
                task.setFormattedText(formattedText);
            }
            if (!hasText(task.getTaskName())) {
                String derivedTitle = extractTitleFromFormattedText(formattedText);
                if (hasText(derivedTitle)) {
                    task.setTaskName(derivedTitle);
                }
            }
            taskService.updateById(task);

            // 5. 完成
            taskService.updateTaskStatus(taskId, TaskStatus.COMPLETED);
            log.info("[Task {}] 处理完成。", taskId);

        } catch (Exception e) {
            log.error("[Task {}] 处理失败: {}", taskId, e.getMessage(), e);
            taskService.updateTaskError(taskId, e.getMessage());
        } finally {
            activeTaskIds.remove(taskId);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private boolean isUsableFile(String value) {
        if (!hasText(value)) {
            return false;
        }
        try {
            return Files.isRegularFile(Path.of(value));
        } catch (InvalidPathException ex) {
            return false;
        }
    }

    private String extractTitleFromFormattedText(String formattedText) {
        if (formattedText == null) {
            return null;
        }
        String[] lines = formattedText.split("\\r?\\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.startsWith("#")) {
                String heading = trimmed.replaceFirst("^#+\\s*", "").trim();
                if (!heading.isEmpty()) {
                    return truncate(heading, 200);
                }
            }
        }
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty()) {
                return truncate(trimmed, 200);
            }
        }
        return null;
    }

    private String truncate(String input, int maxLength) {
        if (input == null || input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength);
    }

    private void updateDurationIfNeeded(Task task) {
        if (task == null) {
            return;
        }
        if (task.getDurationSeconds() != null && task.getDurationSeconds() > 0) {
            return;
        }
        String mediaPath = hasText(task.getVideoLocalPath()) ? task.getVideoLocalPath() : task.getAudioLocalPath();
        if (!hasText(mediaPath)) {
            return;
        }
        Integer duration = ffmpegUtils.getMediaDurationSeconds(mediaPath);
        if (duration != null && duration > 0) {
            task.setDurationSeconds(duration);
            taskService.updateById(task);
        }
    }
}
