package com.aviscribe.service;

import com.aviscribe.dto.TaskInfoDTO;
import com.aviscribe.entity.Task;
import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.domain.Pageable; // Spring Data Pageable

public interface TaskService extends IService<Task> {
    TaskInfoDTO getTaskInfo(Long id);
    Page<TaskInfoDTO> listTasks(Pageable pageable);
    void deleteTask(Long id); // 需删除文件
    void updateTaskStatus(Long taskId, com.aviscribe.common.enums.TaskStatus status);
    void updateTaskError(Long taskId, String error);
    void updateTaskName(Long taskId, String taskName);
}