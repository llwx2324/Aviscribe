package com.aviscribe.service;

import com.aviscribe.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

public interface UserService extends IService<User> {
    User findByUsername(String username);
    User findByPhone(String phone);
    boolean usernameExists(String username);
    boolean phoneExists(String phone);
    void updateProfile(Long id, String displayName, String phone);
    void updatePassword(Long id, String passwordHash);
}
