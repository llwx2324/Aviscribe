package com.aviscribe.service.impl;

import com.aviscribe.common.utils.FfmpegUtils;
import com.aviscribe.entity.Task;
import com.aviscribe.service.AudioExtractService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class AudioExtractServiceImpl implements AudioExtractService {

    @Value("${aviscribe.file.upload-path}")
    private String uploadPath;

    private final FfmpegUtils ffmpegUtils;

    public AudioExtractServiceImpl(FfmpegUtils ffmpegUtils) {
        this.ffmpegUtils = ffmpegUtils;
    }

    @Override
    public String extractAudio(Task task) {
        String inputVideo = task.getVideoLocalPath();
        if (inputVideo == null || inputVideo.isEmpty()) {
            throw new IllegalArgumentException("任务缺少本地视频路径");
        }
        Path out = Paths.get(uploadPath, "audio-" + task.getId() + ".wav");
        String outputAudio = out.toString();

        boolean success = ffmpegUtils.extractAudio(inputVideo, outputAudio);
        if (!success) {
            throw new RuntimeException("FFmpeg 音频提取失败");
        }
        return outputAudio;
    }
}