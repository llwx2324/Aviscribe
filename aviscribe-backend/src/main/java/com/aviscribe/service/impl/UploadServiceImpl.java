package com.aviscribe.service.impl;

import com.aviscribe.common.enums.SourceType;
import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.dto.UploadUrlRequest;
import com.aviscribe.entity.Task;
import com.aviscribe.service.JobProcessService;
import com.aviscribe.service.TaskService;
import com.aviscribe.service.UploadService;
import com.aviscribe.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UploadServiceImpl implements UploadService {

    private static final Logger log = LoggerFactory.getLogger(UploadServiceImpl.class);

    @Value("${aviscribe.file.upload-path}")
    private String uploadPath;

    private final TaskService taskService;
    private final JobProcessService jobProcessService;

    public UploadServiceImpl(TaskService taskService, JobProcessService jobProcessService) {
        this.taskService = taskService;
        this.jobProcessService = jobProcessService;
    }

    @Override
    public Task handleLocalUpload(MultipartFile file, String taskName) {
        if (file.isEmpty()) {
            throw new RuntimeException("上传文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = UUID.randomUUID().toString() + fileExtension;
        
        Path storageDir = Paths.get(uploadPath);
        try {
            if (!storageDir.toFile().exists()) {
                storageDir.toFile().mkdirs();
            }
            Path destination = Paths.get(uploadPath, newFilename);
            file.transferTo(destination);

            Task task = createTask(normalizeTaskName(taskName), SourceType.LOCAL, null, destination.toString());
            // 触发异步处理
            jobProcessService.processTask(task.getId());
            return task;

        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new RuntimeException("文件上传失败", e);
        }
    }

    @Override
    public Task handleUrlUpload(UploadUrlRequest request) {
        String taskName = normalizeTaskName(request.getTaskName());
        Task task = createTask(taskName, SourceType.URL, request.getUrl(), null);
        
        // 状态设置为下载中
        taskService.updateTaskStatus(task.getId(), TaskStatus.DOWNLOADING);
        
        // 触发异步处理
        jobProcessService.processTask(task.getId());
        return task;
    }

    private Task createTask(String taskName, SourceType type, String url, String localPath) {
        Long userId = SecurityUtils.getCurrentUserId();
        Task task = new Task();
        task.setUserId(userId);
        task.setTaskName(ensureTaskName(taskName));
        task.setSourceType(type.getValue());
        task.setVideoUrl(url);
        task.setVideoLocalPath(localPath);
        task.setTaskStatus(TaskStatus.PENDING.getCode());
        task.setCreateTime(LocalDateTime.now());
        task.setUpdateTime(LocalDateTime.now());
        taskService.save(task);
        return task;
    }
    
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".tmp";
        }
        return filename.substring(filename.lastIndexOf("."));
    }

    private String normalizeTaskName(String taskName) {
        if (taskName == null) {
            return null;
        }
        String trimmed = taskName.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.length() > 200 ? trimmed.substring(0, 200) : trimmed;
    }

    private String ensureTaskName(String taskName) {
        if (taskName == null) {
            return "";
        }
        return taskName.trim();
    }
}