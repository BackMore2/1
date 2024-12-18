package com.backmore2.chatroom.repository;

import com.backmore2.chatroom.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    @Query("SELECT m FROM ChatMessage m ORDER BY m.sendTime DESC")
    List<ChatMessage> findLast100Messages(Pageable pageable);
    
    List<ChatMessage> findBySenderId(Long senderId);
} 