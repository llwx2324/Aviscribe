package com.aviscribe.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    @NotBlank(message = "昵称不能为空")
    private String displayName;

    private String phone;
}
