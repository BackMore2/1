package com.backmore2.chatroom.service;

import com.backmore2.chatroom.entity.ChatMessage;
import com.backmore2.chatroom.entity.User;
import com.backmore2.chatroom.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatService {
    @Autowired
    private ChatMessageRepository chatMessageRepository;

    public ChatMessage saveMessage(User sender, String content, String messageType) {
        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setContent(content);
        message.setMessageType(messageType);
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getLast100Messages() {
        PageRequest pageRequest = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "sendTime"));
        return chatMessageRepository.findLast100Messages(pageRequest);
    }
} 