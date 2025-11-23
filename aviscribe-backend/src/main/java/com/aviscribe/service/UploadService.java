package com.aviscribe.service;

import com.aviscribe.dto.UploadUrlRequest;
import com.aviscribe.entity.Task;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {
    Task handleLocalUpload(MultipartFile file, String taskName);
    Task handleUrlUpload(UploadUrlRequest request);
}