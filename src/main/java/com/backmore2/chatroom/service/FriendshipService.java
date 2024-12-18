package com.backmore2.chatroom.service;

import com.backmore2.chatroom.entity.Friendship;
import com.backmore2.chatroom.entity.User;
import com.backmore2.chatroom.repository.FriendshipRepository;
import com.backmore2.chatroom.repository.UserRepository;
import com.backmore2.chatroom.spring.MyWsHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class FriendshipService {
    @Autowired
    private FriendshipRepository friendshipRepository;
    
    @Autowired
    private UserRepository userRepository;

    // 发送好友请求
    @Transactional
    public Friendship sendFriendRequest(String username, String friendUsername) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        User friend = userRepository.findByUsername(friendUsername)
            .orElseThrow(() -> new RuntimeException("好友用户不存在"));
            
        // 检查是否已经是好友
        Optional<Friendship> existingFriendship = friendshipRepository.findByUserAndFriend(user, friend);
        if (existingFriendship.isPresent()) {
            Friendship friendship = existingFriendship.get();
            if (friendship.getStatus() == 1) {
                throw new RuntimeException("已经是好友关系");
            } else if (friendship.getStatus() == 0) {
                throw new RuntimeException("已发送过好友请求");
            }
        }

        // 检查对方是否已经向自己发送了好友请求
        Optional<Friendship> reverseRequest = friendshipRepository.findByUserAndFriend(friend, user);
        if (reverseRequest.isPresent() && reverseRequest.get().getStatus() == 0) {
            throw new RuntimeException("对方已经向你发送了好友请求");
        }

        // 创建新的好友请求
        Friendship friendship = new Friendship();
        friendship.setUser(user);
        friendship.setFriend(friend);
        friendship.setStatus(0);  // 0: 待确认
        
        // 保存好友请求
        Friendship savedFriendship = friendshipRepository.save(friendship);
        
        // 发送WebSocket通知
        String notificationMessage = String.format("FRIEND_REQUEST:%s:%s", username, friendUsername);
        MyWsHandler.sendNotification(notificationMessage);
        
        return savedFriendship;
    }

    // 处理好友请求
    @Transactional
    public Friendship handleFriendRequest(Long friendshipId, Integer status) {
        Friendship friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("好友请求不存在"));
        friendship.setStatus(status);
        return friendshipRepository.save(friendship);
    }

    // 获取好友列表
    public List<Friendship> getFriendList(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return friendshipRepository.findAllFriendships(user);
    }

    // 获取待处理的好友请求
    public List<Friendship> getPendingFriendRequests(String username) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("用户不存在"));
        return friendshipRepository.findByFriendAndStatus(user, 0);
    }
} 