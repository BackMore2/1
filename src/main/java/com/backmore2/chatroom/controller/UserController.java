package com.backmore2.chatroom.controller;

import com.backmore2.chatroom.entity.User;
import com.backmore2.chatroom.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/user")
public class UserController {
    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> register(
            @RequestParam String username, 
            @RequestParam String password,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String avatar) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (userService.register(username, password, nickname, avatar)) {
            response.put("success", true);
            response.put("message", "注册成功");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "用户名��存在");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestParam String username, @RequestParam String password) {
        Map<String, Object> response = new HashMap<>();
        
        User user = userService.login(username, password);
        if (user != null) {
            response.put("success", true);
            response.put("message", "登录成功");
            response.put("username", user.getUsername());
            response.put("nickname", user.getNickname());
            response.put("avatar", user.getAvatar());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "用户名或密码错误");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/current")
    public ResponseEntity<?> getCurrentUser(@RequestParam String username) {
        Map<String, Object> response = new HashMap<>();
        User user = userService.findByUsername(username);
        
        if (user != null) {
            response.put("success", true);
            response.put("user", user);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "用户不存在");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateProfile(
            @RequestParam String username,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String avatar) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 调用service层更新用户信息
        User updatedUser = userService.updateProfile(username, nickname, avatar);
        
        if (updatedUser != null) {
            response.put("success", true);
            response.put("message", "个人资料更新成功");
            response.put("user", updatedUser);
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "更新失败，用户不存在");
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/updateAvatar")
    public ResponseEntity<?> updateAvatar(
            @RequestParam("username") String username,
            @RequestParam("avatar") MultipartFile avatar) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 验证参数
        if (username == null || username.trim().isEmpty()) {
            response.put("success", false);
            response.put("message", "用户名不能为空");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (avatar == null || avatar.isEmpty()) {
            response.put("success", false);
            response.put("message", "请选择要上传的头像");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            // 获取项目根目录的绝对路径
            String projectRoot = System.getProperty("user.dir");
            String uploadDir = projectRoot + "/src/main/resources/static/uploads/";
            File dir = new File(uploadDir);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new IOException("创建目录失败");
                }
            }

            // 验证文件类型
            String contentType = avatar.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                response.put("success", false);
                response.put("message", "只能上传图片文件");
                return ResponseEntity.badRequest().body(response);
            }

            // 生成文件名
            String originalFilename = avatar.getOriginalFilename();
            String extension = originalFilename != null ? originalFilename.substring(originalFilename.lastIndexOf(".")) : ".jpg";
            String fileName = UUID.randomUUID().toString() + extension;
            String filePath = uploadDir + fileName;
            
            // 保存文件
            File dest = new File(filePath);
            avatar.transferTo(dest);
            
            // 更新用户头像URL
            String avatarUrl = "/uploads/" + fileName;
            User updatedUser = userService.updateProfile(username, null, avatarUrl);
            
            if (updatedUser != null) {
                response.put("success", true);
                response.put("message", "头像更新成功");
                response.put("avatarUrl", avatarUrl);
                return ResponseEntity.ok(response);
            } else {
                // 如果用户更新失败，删除已上传的文件
                if (dest.exists()) {
                    dest.delete();
                }
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.badRequest().body(response);
            }
            
        } catch (IOException e) {
            response.put("success", false);
            response.put("message", "头像上传失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(@RequestParam String keyword) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<User> users = userService.searchUsers(keyword);
            response.put("success", true);
            response.put("users", users);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/status")
    public ResponseEntity<?> updateUserStatus(@RequestParam String username, @RequestParam Integer status) {
        Map<String, Object> response = new HashMap<>();
        try {
            User user = userService.findByUsername(username);
            if (user != null) {
                userService.updateStatus(user.getId(), status);
                response.put("success", true);
                response.put("message", "状态更新成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
} 