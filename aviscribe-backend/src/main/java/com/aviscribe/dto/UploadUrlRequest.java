package com.aviscribe.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.URL;

@Data
public class UploadUrlRequest {
    @NotNull(message = "URL 不能为空")
    @URL(message = "无效的 URL 格式")
    private String url;
    
    private String taskName; // 可选的任务名
}