package com.backmore2.chatroom.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ServerEndpoint("/myWs1")
@Component
public class WebSocketServer {
    private static final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private static final Map<Session, String> usernames = new ConcurrentHashMap<>();
    private static final Map<String, ScheduledFuture<?>> heartbeatFutures = new ConcurrentHashMap<>();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final long HEARTBEAT_TIMEOUT = 40000; // 40秒超时
    private String sessionId;
    private static final Logger log = LoggerFactory.getLogger(WebSocketServer.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    @OnOpen
    public void onOpen(Session session) {
        this.sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("新的连接: {}", sessionId);
        
        // 启动心跳检查
        startHeartbeatCheck(session);
    }

    private void startHeartbeatCheck(Session session) {
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            if (!sessions.containsKey(sessionId)) {
                // 如果会话已经不存在，取消定时任务
                ScheduledFuture<?> scheduledFuture = heartbeatFutures.remove(sessionId);
                if (scheduledFuture != null) {
                    scheduledFuture.cancel(true);
                }
                return;
            }
            
            try {
                // 发送心跳响应
                sendMessageToSession(session, createHeartbeatMessage());
            } catch (Exception e) {
                log.error("发送心跳失败: {}", e.getMessage());
                closeSession(session);
            }
        }, 0, 30, TimeUnit.SECONDS);
        
        heartbeatFutures.put(sessionId, future);
    }

    private String createHeartbeatMessage() {
        ObjectNode heartbeat = objectMapper.createObjectNode();
        heartbeat.put("type", "heartbeat");
        return heartbeat.toString();
    }

    private void sendMessageToSession(Session session, String message) {
        if (session == null || !session.isOpen()) {
            log.warn("尝试向已关闭的会话发送消息");
            return;
        }

        try {
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            log.error("发送消息失败: {}", e.getMessage());
            closeSession(session);
        }
    }

    private void closeSession(Session session) {
        if (session == null) return;
        
        try {
            String sid = session.getId();
            ScheduledFuture<?> future = heartbeatFutures.remove(sid);
            if (future != null) {
                future.cancel(true);
            }
            
            sessions.remove(sid);
            usernames.remove(session);
            
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            log.error("关闭会话失败: {}", e.getMessage());
        }
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        if (session == null || !session.isOpen()) {
            log.warn("收到来自已关闭会话的消息");
            return;
        }

        try {
            JsonNode jsonNode = objectMapper.readTree(message);
            String type = jsonNode.get("type").asText();
            
            // 处理心跳消息
            if ("heartbeat".equals(type)) {
                return;
            }
            
            String username = jsonNode.get("username").asText();
            String target = jsonNode.has("target") ? jsonNode.get("target").asText() : "all";
            
            // 更新用户名映射
            usernames.put(session, username);
            
            if ("chat".equals(type) || "file".equals(type)) {
                handleMessage(type, username, target, message);
            }
        } catch (Exception e) {
            log.error("处理消息错误: {}", e.getMessage());
            sendErrorMessage(session, "消息处理失败：" + e.getMessage());
        }
    }

    private void handleMessage(String type, String username, String target, String message) {
        if ("all".equals(target)) {
            broadcastMessage(message, username);
        } else {
            sendPrivateMessage(target, message);
        }
    }

    private void broadcastMessage(String message, String excludeUsername) {
        for (Map.Entry<Session, String> entry : usernames.entrySet()) {
            if (!entry.getValue().equals(excludeUsername)) {
                Session recipientSession = entry.getKey();
                if (recipientSession.isOpen()) {
                    sendMessageToSession(recipientSession, message);
                }
            }
        }
    }

    private void sendPrivateMessage(String targetUsername, String message) {
        Session targetSession = null;
        for (Map.Entry<Session, String> entry : usernames.entrySet()) {
            if (entry.getValue().equals(targetUsername)) {
                targetSession = entry.getKey();
                break;
            }
        }

        if (targetSession != null && targetSession.isOpen()) {
            sendMessageToSession(targetSession, message);
            log.info("私聊消息已发送至用户: {}", targetUsername);
        } else {
            log.warn("目标用户 {} 不在线或会话已关闭", targetUsername);
        }
    }

    private void sendErrorMessage(Session session, String errorMessage) {
        try {
            ObjectNode errorNode = objectMapper.createObjectNode();
            errorNode.put("type", "error");
            errorNode.put("message", errorMessage);
            sendMessageToSession(session, errorNode.toString());
        } catch (Exception e) {
            log.error("发送错误消息失败: {}", e.getMessage());
        }
    }

    @OnClose
    public void onClose() {
        ScheduledFuture<?> future = heartbeatFutures.remove(sessionId);
        if (future != null) {
            future.cancel(true);
        }
        sessions.remove(sessionId);
        log.info("连接关闭: {}", sessionId);
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket错误: {}", error.getMessage());
        closeSession(session);
    }
    
    @PreDestroy
    public void destroy() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
} 