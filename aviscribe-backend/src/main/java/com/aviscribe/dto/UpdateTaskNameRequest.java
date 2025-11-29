package com.aviscribe.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateTaskNameRequest {
    @NotBlank(message = "任务名称不能为空")
    private String taskName;
}
