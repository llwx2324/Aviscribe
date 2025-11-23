package com.aviscribe.controller;

import com.aviscribe.dto.TaskInfoDTO;
import com.aviscribe.dto.UploadUrlRequest;
import com.aviscribe.entity.Task;
import com.aviscribe.service.UploadService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/v1/upload")
public class UploadController {

    private final UploadService uploadService;

    public UploadController(UploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/local")
    public ResponseEntity<TaskInfoDTO> uploadLocal(@RequestParam("file") MultipartFile file,
                                                   @RequestParam(value = "taskName", required = false) String taskName) {
        if (file.isEmpty()) {
            // 使用全局异常处理来返回统一的错误响应
            throw new IllegalArgumentException("上传文件不能为空");
        }
        Task task = uploadService.handleLocalUpload(file, taskName);
        return ResponseEntity.accepted().body(TaskInfoDTO.fromEntity(task));
    }

    @PostMapping("/url")
    public ResponseEntity<TaskInfoDTO> uploadUrl(@Validated @RequestBody UploadUrlRequest request) {
        Task task = uploadService.handleUrlUpload(request);
        return ResponseEntity.accepted().body(TaskInfoDTO.fromEntity(task));
    }
}