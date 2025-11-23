package com.aviscribe.service;

import com.aviscribe.dto.AuthResponse;
import com.aviscribe.dto.ChangePasswordRequest;
import com.aviscribe.dto.LoginRequest;
import com.aviscribe.dto.RefreshTokenRequest;
import com.aviscribe.dto.UpdateProfileRequest;
import com.aviscribe.dto.UserProfileDTO;
import com.aviscribe.dto.UserRegisterRequest;

public interface AuthService {
    AuthResponse register(UserRegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    UserProfileDTO getCurrentProfile();
    void changePassword(ChangePasswordRequest request);
    void updateProfile(UpdateProfileRequest request);
}
