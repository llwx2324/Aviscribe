package com.aviscribe.common.utils;

import com.aviscribe.common.enums.UserRole;
import com.aviscribe.security.LoginUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUser getLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof LoginUser loginUser) {
            return loginUser;
        }
        return null;
    }

    public static Long getCurrentUserId() {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null) {
            throw new IllegalStateException("未登录或凭证失效");
        }
        return loginUser.getId();
    }

    public static boolean hasRole(UserRole role) {
        LoginUser loginUser = getLoginUser();
        if (loginUser == null || role == null) {
            return false;
        }
        return loginUser.getRole() == role;
    }
}
