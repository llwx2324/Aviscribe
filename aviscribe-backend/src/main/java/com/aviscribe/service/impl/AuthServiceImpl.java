package com.aviscribe.service.impl;

import com.aviscribe.common.enums.UserRole;
import com.aviscribe.common.utils.SecurityUtils;
import com.aviscribe.dto.AuthResponse;
import com.aviscribe.dto.ChangePasswordRequest;
import com.aviscribe.dto.LoginRequest;
import com.aviscribe.dto.RefreshTokenRequest;
import com.aviscribe.dto.UpdateProfileRequest;
import com.aviscribe.dto.UserProfileDTO;
import com.aviscribe.dto.UserRegisterRequest;
import com.aviscribe.entity.User;
import com.aviscribe.security.JwtTokenProvider;
import com.aviscribe.security.JwtTokenProvider.TokenPair;
import com.aviscribe.service.AuthService;
import com.aviscribe.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(UserService userService,
                           PasswordEncoder passwordEncoder,
                           JwtTokenProvider jwtTokenProvider) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AuthResponse register(UserRegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (userService.usernameExists(username)) {
            throw new IllegalArgumentException("该用户名已存在");
        }
        String phone = StringUtils.hasText(request.getPhone()) ? request.getPhone().trim() : null;
        if (StringUtils.hasText(phone) && userService.phoneExists(phone)) {
            throw new IllegalArgumentException("该手机号已注册");
        }
        User user = new User();
        user.setUsername(username);
        user.setPhone(phone);
        user.setDisplayName(request.getDisplayName().trim());
        user.setRole(UserRole.USER.name());
        user.setStatus(1);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        userService.save(user);
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        String username = normalizeUsername(request.getUsername());
        User user = userService.findByUsername(username);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("账号不存在或已禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        return buildAuthResponse(user);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String token = request.getRefreshToken();
        if (!jwtTokenProvider.isTokenValid(token) || !jwtTokenProvider.isRefreshToken(token)) {
            throw new IllegalArgumentException("刷新令牌无效");
        }
        Long userId = jwtTokenProvider.extractUserId(token);
        User user = userService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("账号不存在或已禁用");
        }
        return buildAuthResponse(user);
    }

    @Override
    public UserProfileDTO getCurrentProfile() {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        return UserProfileDTO.fromEntity(user);
    }

    @Override
    public void changePassword(ChangePasswordRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        User user = userService.getById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new IllegalArgumentException("账号不存在或已禁用");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        userService.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
    }

    @Override
    public void updateProfile(UpdateProfileRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (StringUtils.hasText(request.getPhone())) {
            User existing = userService.findByPhone(request.getPhone().trim());
            if (existing != null && !existing.getId().equals(userId)) {
                throw new IllegalArgumentException("该手机号已注册");
            }
        }
        userService.updateProfile(userId, request.getDisplayName(), request.getPhone());
    }

    private AuthResponse buildAuthResponse(User user) {
        TokenPair pair = jwtTokenProvider.generateTokenPair(user);
        AuthResponse response = new AuthResponse();
        response.setAccessToken(pair.accessToken());
        response.setRefreshToken(pair.refreshToken());
        response.setAccessTokenExpiresAt(pair.accessExp());
        response.setRefreshTokenExpiresAt(pair.refreshExp());
        response.setProfile(UserProfileDTO.fromEntity(user));
        return response;
    }

    private String normalizeUsername(String username) {
        return username == null ? null : username.trim().toLowerCase();
    }
}
