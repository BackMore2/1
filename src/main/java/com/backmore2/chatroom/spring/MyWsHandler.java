package com.backmore2.chatroom.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * web socket 主处理程序
 */
@Slf4j
@Component
public class MyWsHandler extends AbstractWebSocketHandler {
    private static Map<String,SessionBean> sessionBeanMap;
    private static AtomicInteger clientIdMaker;
    private static StringBuffer stringBuffer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    static {
        sessionBeanMap = new ConcurrentHashMap<>();
        clientIdMaker = new AtomicInteger(0);
        stringBuffer = new StringBuffer();
    }

    //连接建立
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        super.afterConnectionEstablished(session);
        SessionBean sessionBean = new SessionBean(session,clientIdMaker.getAndIncrement());
        sessionBeanMap.put(session.getId(),sessionBean);
        log.info("客户端 {} 建立了连接", sessionBean.getClientId());
        stringBuffer.append(sessionBeanMap.get(session.getId()).getClientId()+"进入了群聊<br/>");
        sendMessage(sessionBeanMap);
    }

    //收到消息
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        super.handleTextMessage(session, message);
        
        try {
            // 解析消息
            Map<String, Object> messageMap = objectMapper.readValue(message.getPayload(), Map.class);
            String messageType = (String) messageMap.get("type");
            
            // 处理心跳消息
            if ("heartbeat".equals(messageType)) {
                handleHeartbeat(session);
                return;
            }
            
            // 处理其他类型的消息
            log.info("客户端 {} 发送消息: {}", sessionBeanMap.get(session.getId()).getClientId(), message.getPayload());
            stringBuffer.append(sessionBeanMap.get(session.getId()).getClientId()+":"+message.getPayload()+"<br/>");
            sendMessage(sessionBeanMap);
            
        } catch (Exception e) {
            log.error("处理消息失败: {}", e.getMessage());
            // 如果消息解析失败，按照普通文本处理
            stringBuffer.append(sessionBeanMap.get(session.getId()).getClientId()+":"+message.getPayload()+"<br/>");
            sendMessage(sessionBeanMap);
        }
    }

    // 处理心跳消息
    private void handleHeartbeat(WebSocketSession session) {
        try {
            Map<String, Object> heartbeatResponse = Map.of(
                "type", "heartbeat",
                "timestamp", System.currentTimeMillis()
            );
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(heartbeatResponse)));
        } catch (IOException e) {
            log.error("发送心跳响应失败: {}", e.getMessage());
        }
    }

    //传输异常
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("客户端 {} 传输异常: {}", sessionBeanMap.get(session.getId()).getClientId(), exception.getMessage());
        if(session.isOpen()){
            session.close();
        }
        removeSession(session);
    }

    //连接关闭
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        super.afterConnectionClosed(session, status);
        SessionBean sessionBean = sessionBeanMap.get(session.getId());
        if (sessionBean != null) {
            int clientId = sessionBean.getClientId();
            removeSession(session);
            log.info("客户端 {} 关闭了连接, 状态码: {}", clientId, status.getCode());
            stringBuffer.append(clientId+"退出了群聊<br/>");
            sendMessage(sessionBeanMap);
        }
    }

    // 移除会话
    private void removeSession(WebSocketSession session) {
        sessionBeanMap.remove(session.getId());
    }

    // 发送消息给所有客户端
    public void sendMessage(Map<String,SessionBean> sessionBeanMap) {
        for(Map.Entry<String, SessionBean> entry : sessionBeanMap.entrySet()) {
            try {
                WebSocketSession session = entry.getValue().getWebSocketSession();
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(stringBuffer.toString()));
                }
            } catch (IOException e) {
                log.error("发送消息失败: {}", e.getMessage());
                // 如果发送失败，考虑移除无效的会话
                removeSession(entry.getValue().getWebSocketSession());
            }
        }
    }

    // 发送通知
    public static void sendNotification(String message) {
        for(Map.Entry<String, SessionBean> entry : sessionBeanMap.entrySet()) {
            try {
                WebSocketSession session = entry.getValue().getWebSocketSession();
                if (session != null && session.isOpen()) {
                    session.sendMessage(new TextMessage(message));
                }
            } catch (IOException e) {
                log.error("发送通知失败: {}", e.getMessage());
            }
        }
    }

    // 定时清理无效的会话
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void cleanInvalidSessions() {
        sessionBeanMap.entrySet().removeIf(entry -> {
            WebSocketSession session = entry.getValue().getWebSocketSession();
            return session == null || !session.isOpen();
        });
    }
}
