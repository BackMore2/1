package com.backmore2.chatroom.mapper;

import com.backmore2.chatroom.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {
    void updateUser(User user);
    User findByUsername(String username);
}