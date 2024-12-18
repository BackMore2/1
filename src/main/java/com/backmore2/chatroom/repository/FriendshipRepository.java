package com.backmore2.chatroom.repository;

import com.backmore2.chatroom.entity.Friendship;
import com.backmore2.chatroom.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {
    // 查找两个用户之间的好友关系
    Optional<Friendship> findByUserAndFriend(User user, User friend);
    
    // 获取用户的所有好友关系（包括发起的和接收的）
    @Query("SELECT f FROM Friendship f WHERE (f.user = :user OR f.friend = :user) AND f.status = 1")
    List<Friendship> findAllFriendships(@Param("user") User user);
    
    // 获取用户收到的好友请求
    List<Friendship> findByFriendAndStatus(User friend, Integer status);
    
    // 获取用户发送的好友请求
    List<Friendship> findByUserAndStatus(User user, Integer status);
} 