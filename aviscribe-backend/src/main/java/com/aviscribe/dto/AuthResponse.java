package com.aviscribe.dto;

import lombok.Data;

@Data
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private long accessTokenExpiresAt;
    private long refreshTokenExpiresAt;
    private UserProfileDTO profile;
}
