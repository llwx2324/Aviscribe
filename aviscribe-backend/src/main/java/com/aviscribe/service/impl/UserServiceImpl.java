package com.aviscribe.service.impl;

import com.aviscribe.entity.User;
import com.aviscribe.mapper.UserMapper;
import com.aviscribe.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Override
    public User findByUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>().eq(User::getUsername, username).last("LIMIT 1"));
    }

    @Override
    public User findByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone).last("LIMIT 1"));
    }

    @Override
    public boolean usernameExists(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }
        return lambdaQuery().eq(User::getUsername, username).exists();
    }

    @Override
    public boolean phoneExists(String phone) {
        if (!StringUtils.hasText(phone)) {
            return false;
        }
        return lambdaQuery().eq(User::getPhone, phone).exists();
    }

    @Override
    public void updateProfile(Long id, String displayName, String phone) {
        if (id == null) {
            return;
        }
        User user = getById(id);
        if (user == null) {
            return;
        }
        if (StringUtils.hasText(displayName)) {
            user.setDisplayName(displayName.trim());
        }
        if (StringUtils.hasText(phone)) {
            user.setPhone(phone.trim());
        } else {
            user.setPhone(null);
        }
        updateById(user);
    }

    @Override
    public void updatePassword(Long id, String passwordHash) {
        if (id == null || !StringUtils.hasText(passwordHash)) {
            return;
        }
        User user = getById(id);
        if (user == null) {
            return;
        }
        user.setPasswordHash(passwordHash);
        updateById(user);
    }
}
