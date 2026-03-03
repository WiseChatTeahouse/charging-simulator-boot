package chat.wisechat.charging.service;

import chat.wisechat.charging.vo.ChargingDataVO;
import chat.wisechat.charging.vo.VehicleBmsDataVO;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket 服务类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@Service
public class WebSocketService {
    
    // 充电会话ID -> WebSocket会话集合
    private final Map<Long, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();
    
    /**
     * 注册 WebSocket 会话
     */
    public void registerSession(Long chargingSessionId, WebSocketSession wsSession) {
        sessionMap.computeIfAbsent(chargingSessionId, k -> new CopyOnWriteArraySet<>())
                .add(wsSession);
        log.info("注册 WebSocket 会话: chargingSessionId={}, wsSessionId={}, 当前连接数={}", 
                chargingSessionId, wsSession.getId(), sessionMap.get(chargingSessionId).size());
    }
    
    /**
     * 注销 WebSocket 会话
     */
    public void unregisterSession(Long chargingSessionId, WebSocketSession wsSession) {
        Set<WebSocketSession> sessions = sessionMap.get(chargingSessionId);
        if (sessions != null) {
            sessions.remove(wsSession);
            if (sessions.isEmpty()) {
                sessionMap.remove(chargingSessionId);
            }
            log.info("注销 WebSocket 会话: chargingSessionId={}, wsSessionId={}", 
                    chargingSessionId, wsSession.getId());
        }
    }
    
    /**
     * 推送充电数据
     */
    public void pushChargingData(Long chargingSessionId, ChargingDataVO data) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("CHARGING_DATA");
        message.setData(data);
        
        pushMessage(chargingSessionId, message);
    }
    
    /**
     * 推送 BMS 数据
     */
    public void pushBmsData(Long chargingSessionId, VehicleBmsDataVO data) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("BMS_DATA");
        message.setData(data);
        
        pushMessage(chargingSessionId, message);
    }
    
    /**
     * 推送消息到指定会话的所有 WebSocket 连接
     */
    private void pushMessage(Long chargingSessionId, WebSocketMessage message) {
        Set<WebSocketSession> sessions = sessionMap.get(chargingSessionId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("没有找到会话的 WebSocket 连接: chargingSessionId={}", chargingSessionId);
            return;
        }
        
        String jsonMessage = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(jsonMessage);
        
        sessions.forEach(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                    log.debug("推送消息成功: chargingSessionId={}, wsSessionId={}, type={}", 
                            chargingSessionId, session.getId(), message.getType());
                }
            } catch (IOException e) {
                log.error("推送消息失败: chargingSessionId={}, wsSessionId={}", 
                        chargingSessionId, session.getId(), e);
            }
        });
    }
    
    /**
     * WebSocket 消息封装类
     */
    private static class WebSocketMessage {
        private String type;
        private Object data;
        
        public String getType() {
            return type;
        }
        
        public void setType(String type) {
            this.type = type;
        }
        
        public Object getData() {
            return data;
        }
        
        public void setData(Object data) {
            this.data = data;
        }
    }
}
