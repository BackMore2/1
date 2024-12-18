package com.backmore2.chatroom.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(length = 50)
    private String nickname;

    @Column(length = 200)
    private String avatar;

    @Column(nullable = false)
    private Integer status = 0;  // 0:离线 1:在线

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "last_login_time")
    private LocalDateTime lastLoginTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
        if (nickname == null || nickname.trim().isEmpty()) {
            nickname = username;
        }
        if (avatar == null || avatar.trim().isEmpty()) {
            avatar = "/img/default-avatar.jpg";  // 默认头像
        }
    }
} 