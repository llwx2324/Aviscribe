package com.aviscribe.service.impl;

import com.aviscribe.common.enums.TaskStatus;
import com.aviscribe.common.enums.UserRole;
import com.aviscribe.common.utils.FileUtils;
import com.aviscribe.common.utils.FfmpegUtils;
import com.aviscribe.common.utils.SecurityUtils;
import com.aviscribe.dto.TaskInfoDTO;
import com.aviscribe.entity.Task;
import com.aviscribe.mapper.TaskMapper;
import com.aviscribe.service.TaskService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.domain.Pageable; // Spring Data
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private final FileUtils fileUtils;
    private final FfmpegUtils ffmpegUtils;

    public TaskServiceImpl(FileUtils fileUtils, FfmpegUtils ffmpegUtils) {
        this.fileUtils = fileUtils;
        this.ffmpegUtils = ffmpegUtils;
    }

    @Override
    public TaskInfoDTO getTaskInfo(Long id) {
        Task task = this.getById(id);
        if (task == null) {
            return null;
        }
        ensureTaskAccessible(task);
        return TaskInfoDTO.fromEntity(task);
    }

    @Override
    public Page<TaskInfoDTO> listTasks(Pageable pageable) {
        Page<Task> mpPage = new Page<>(pageable.getPageNumber() + 1L, pageable.getPageSize());

        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        boolean isAdmin = SecurityUtils.hasRole(UserRole.ADMIN);
        if (!isAdmin) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            wrapper.eq(Task::getUserId, currentUserId);
        }
        wrapper.orderByDesc(Task::getCreateTime); // 按创建时间倒序

        Page<Task> resultPage = this.page(mpPage, wrapper);
        resultPage.getRecords().forEach(this::ensureDurationPresent);
        return (Page<TaskInfoDTO>) resultPage.convert(TaskInfoDTO::fromEntity);
    }

    @Override
    public void deleteTask(Long id) {
        Task task = this.getById(id);
        if (task != null) {
            ensureTaskAccessible(task);
            // 删除关联的本地视频、音频文件
            fileUtils.deleteFileQuietly(task.getVideoLocalPath());
            fileUtils.deleteFileQuietly(task.getAudioLocalPath());
            this.removeById(id);
        }
    }

    @Override
    public void updateTaskStatus(Long taskId, TaskStatus status) {
        Task update = new Task();
        update.setId(taskId);
        update.setTaskStatus(status.getCode());
        if (status == TaskStatus.COMPLETED || status == TaskStatus.FAILED) {
            update.setFinishTime(LocalDateTime.now());
        }
        this.updateById(update);
    }

    @Override
    public void updateTaskError(Long taskId, String error) {
        Task update = new Task();
        update.setId(taskId);
        update.setTaskStatus(TaskStatus.FAILED.getCode());
        update.setErrorLog(error);
        update.setFinishTime(LocalDateTime.now());
        this.updateById(update);
    }

    @Override
    public void updateTaskName(Long taskId, String taskName) {
        if (taskName == null || taskName.trim().isEmpty()) {
            throw new IllegalArgumentException("任务名称不能为空");
        }
        Task task = this.getById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("任务不存在");
        }
        ensureTaskAccessible(task);
        if (task.getTaskStatus() == null || task.getTaskStatus() != TaskStatus.COMPLETED.getCode()) {
            throw new IllegalStateException("任务未完成，暂不可修改名称");
        }
        Task update = new Task();
        update.setId(taskId);
        update.setTaskName(taskName.trim());
        this.updateById(update);
    }

    private void ensureDurationPresent(Task task) {
        if (task == null) {
            return;
        }
        if (task.getDurationSeconds() != null && task.getDurationSeconds() > 0) {
            return;
        }
        String mediaPath = firstNonBlank(task.getVideoLocalPath(), task.getAudioLocalPath());
        if (mediaPath == null) {
            return;
        }
        Integer duration = ffmpegUtils.getMediaDurationSeconds(mediaPath);
        if (duration != null && duration > 0) {
            task.setDurationSeconds(duration);
            this.updateById(task);
        }
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value;
            }
        }
        return null;
    }

    private void ensureTaskAccessible(Task task) {
        boolean isAdmin = SecurityUtils.hasRole(UserRole.ADMIN);
        Long currentUserId = SecurityUtils.getCurrentUserId();
        if (!isAdmin && (task.getUserId() == null || !task.getUserId().equals(currentUserId))) {
            throw new AccessDeniedException("无权访问该任务");
        }
    }
}