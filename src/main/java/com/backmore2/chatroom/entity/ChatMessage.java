package com.backmore2.chatroom.entity;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_messages")
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "send_time", nullable = false)
    private LocalDateTime sendTime;

    @Column(name = "message_type")
    private String messageType = "text";  // text, image, file等

    @PrePersist
    public void prePersist() {
        sendTime = LocalDateTime.now();
    }
} 