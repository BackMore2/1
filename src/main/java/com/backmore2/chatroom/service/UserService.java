package com.backmore2.chatroom.service;

import com.backmore2.chatroom.entity.User;

import java.util.List;

public interface UserService {
    // 注册用户
    boolean register(String username, String password, String nickname, String avatar);
    
    // 用户登录
    User login(String username, String password);
    
    // 更新用户状态
    void updateStatus(Long userId, Integer status);
    
    // 根据用户名查找用户
    User findByUsername(String username);
    
    // 更新用户资料
    User updateProfile(String username, String nickname, String avatar);
    
    // 搜索用户
    List<User> searchUsers(String keyword);
} 