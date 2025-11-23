package com.aviscribe.controller;

import com.aviscribe.dto.TaskInfoDTO;
import com.aviscribe.service.TaskService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}