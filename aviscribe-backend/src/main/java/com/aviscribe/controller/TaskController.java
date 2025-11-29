package com.aviscribe.controller;

import com.aviscribe.dto.TaskInfoDTO;
import com.aviscribe.dto.UpdateTaskNameRequest;
import com.aviscribe.service.TaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/v1/task")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskInfoDTO> getTaskById(@PathVariable Long id) {
        TaskInfoDTO taskInfo = taskService.getTaskInfo(id);
        if (taskInfo == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(taskInfo);
    }

    @GetMapping("/list")
    public ResponseEntity<Page<TaskInfoDTO>> listTasks(@PageableDefault(size = 10, sort = "createTime") Pageable pageable) {
        Page<TaskInfoDTO> page = taskService.listTasks(pageable);
        return ResponseEntity.ok(page);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/name")
    public ResponseEntity<Void> updateTaskName(@PathVariable Long id,
                                               @Valid @RequestBody UpdateTaskNameRequest request) {
        taskService.updateTaskName(id, request.getTaskName());
        return ResponseEntity.noContent().build();
    }
}