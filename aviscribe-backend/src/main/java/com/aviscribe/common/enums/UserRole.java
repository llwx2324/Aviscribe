package com.aviscribe.common.enums;

public enum UserRole {
    ADMIN,
    USER;

    public static UserRole fromString(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        for (UserRole role : values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        return USER;
    }
}
