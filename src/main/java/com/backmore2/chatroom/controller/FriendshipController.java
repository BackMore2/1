package com.backmore2.chatroom.controller;

import com.backmore2.chatroom.entity.Friendship;
import com.backmore2.chatroom.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friendship")
public class FriendshipController {
    @Autowired
    private FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(
            @RequestParam String username,
            @RequestParam String friendUsername) {
        Map<String, Object> response = new HashMap<>();
        try {
            Friendship friendship = friendshipService.sendFriendRequest(username, friendUsername);
            response.put("success", true);
            response.put("message", "好友请求已发送");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/handle")
    public ResponseEntity<?> handleFriendRequest(
            @RequestParam Long friendshipId,
            @RequestParam Integer status) {
        Map<String, Object> response = new HashMap<>();
        try {
            Friendship friendship = friendshipService.handleFriendRequest(friendshipId, status);
            response.put("success", true);
            response.put("message", status == 1 ? "已接受好友请求" : "已拒绝好友请求");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<?> getFriendList(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Friendship> friends = friendshipService.getFriendList(username);
            response.put("success", true);
            response.put("friends", friends);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/requests")
    public ResponseEntity<?> getPendingRequests(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Friendship> requests = friendshipService.getPendingFriendRequests(username);
            response.put("success", true);
            response.put("requests", requests);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 