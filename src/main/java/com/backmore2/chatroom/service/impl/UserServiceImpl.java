package com.backmore2.chatroom.service.impl;

import com.backmore2.chatroom.entity.User;
import com.backmore2.chatroom.mapper.UserMapper;
import com.backmore2.chatroom.repository.UserRepository;
import com.backmore2.chatroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Override
    public boolean register(String username, String password, String nickname, String avatar) {
        if (userRepository.existsByUsername(username)) {
            return false;
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setNickname(nickname);
        user.setAvatar(avatar);
        user.setStatus(0);
        userRepository.save(user);
        return true;
    }

    @Override
    public User login(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> passwordEncoder.matches(password, user.getPassword()))
                .map(user -> {
                    user.setLastLoginTime(LocalDateTime.now());
                    user.setStatus(1);
                    return userRepository.save(user);
                })
                .orElse(null);
    }

    @Override
    public void updateStatus(Long userId, Integer status) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setStatus(status);
            userRepository.save(user);
        });
    }

    @Override
    public User findByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    @Override
    public User updateProfile(String username, String nickname, String avatar) {
        return userRepository.findByUsername(username)
                .map(user -> {
                    if (nickname != null) {
                        user.setNickname(nickname);
                    }
                    if (avatar != null) {
                        user.setAvatar(avatar);
                    }
                    return userRepository.save(user);
                })
                .orElse(null);
    }

    @Override
    public List<User> searchUsers(String keyword) {
        return userRepository.findByUsernameContainingOrNicknameContaining(keyword, keyword);
    }
} 