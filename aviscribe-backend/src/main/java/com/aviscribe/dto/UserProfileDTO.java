package com.aviscribe.dto;

import com.aviscribe.entity.User;
import lombok.Data;

@Data
public class UserProfileDTO {
    private Long id;
    private String username;
    private String phone;
    private String displayName;
    private String role;

    public static UserProfileDTO fromEntity(User user) {
        if (user == null) {
            return null;
        }
        UserProfileDTO dto = new UserProfileDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setPhone(user.getPhone());
        dto.setDisplayName(user.getDisplayName());
        dto.setRole(user.getRole());
        return dto;
    }
}
