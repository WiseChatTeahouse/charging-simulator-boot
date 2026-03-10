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
    
    // 充电枪ID -> WebSocket会话集合
    private final Map<Long, Set<WebSocketSession>> sessionMap = new ConcurrentHashMap<>();
    
    /**
     * 注册 WebSocket 会话
     */
    public void registerSession(Long gunId, WebSocketSession wsSession) {
        sessionMap.computeIfAbsent(gunId, k -> new CopyOnWriteArraySet<>())
                .add(wsSession);
        log.info("注册 WebSocket 会话: gunId={}, wsSessionId={}, 当前连接数={}", 
                gunId, wsSession.getId(), sessionMap.get(gunId).size());
    }
    
    /**
     * 注销 WebSocket 会话
     */
    public void unregisterSession(Long gunId, WebSocketSession wsSession) {
        Set<WebSocketSession> sessions = sessionMap.get(gunId);
        if (sessions != null) {
            sessions.remove(wsSession);
            if (sessions.isEmpty()) {
                sessionMap.remove(gunId);
            }
            log.info("注销 WebSocket 会话: gunId={}, wsSessionId={}", 
                    gunId, wsSession.getId());
        }
    }
    
    /**
     * 推送充电数据
     */
    public void pushChargingData(Long gunId, ChargingDataVO data) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("CHARGING_DATA");
        message.setData(data);
        
        pushMessage(gunId, message);
    }
    
    /**
     * 推送 BMS 数据
     */
    public void pushBmsData(Long gunId, VehicleBmsDataVO data) {
        WebSocketMessage message = new WebSocketMessage();
        message.setType("BMS_DATA");
        message.setData(data);
        
        pushMessage(gunId, message);
    }
    
    /**
     * 推送消息到指定充电枪的所有 WebSocket 连接
     */
    private void pushMessage(Long gunId, WebSocketMessage message) {
        Set<WebSocketSession> sessions = sessionMap.get(gunId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("没有找到充电枪的 WebSocket 连接: gunId={}", gunId);
            return;
        }
        
        String jsonMessage = JSON.toJSONString(message);
        TextMessage textMessage = new TextMessage(jsonMessage);
        
        // 使用迭代器安全地遍历和移除无效连接
        sessions.removeIf(session -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                    log.debug("推送消息成功: gunId={}, wsSessionId={}, type={}", 
                            gunId, session.getId(), message.getType());
                    return false; // 保留有效连接
                } else {
                    log.debug("移除已关闭的 WebSocket 连接: gunId={}, wsSessionId={}", 
                            gunId, session.getId());
                    return true; // 移除无效连接
                }
            } catch (IOException e) {
                log.error("推送消息失败，移除连接: gunId={}, wsSessionId={}", 
                        gunId, session.getId(), e);
                return true; // 移除异常连接
            }
        });
        
        // 如果所有连接都被移除，清理map
        if (sessions.isEmpty()) {
            sessionMap.remove(gunId);
        }
    }
    
    /**
     * 获取指定充电枪的连接数
     */
    public int getConnectionCount(Long gunId) {
        Set<WebSocketSession> sessions = sessionMap.get(gunId);
        return sessions != null ? sessions.size() : 0;
    }
    
    /**
     * 获取所有活跃连接数
     */
    public int getTotalConnectionCount() {
        return sessionMap.values().stream()
                .mapToInt(Set::size)
                .sum();
    }
    
    /**
     * 清除指定充电枪的所有连接
     */
    public void clearGunSessions(Long gunId) {
        Set<WebSocketSession> sessions = sessionMap.remove(gunId);
        if (sessions != null) {
            sessions.forEach(session -> {
                try {
                    if (session.isOpen()) {
                        session.close();
                    }
                } catch (Exception e) {
                    log.error("关闭 WebSocket 连接失败: gunId={}, wsSessionId={}", 
                            gunId, session.getId(), e);
                }
            });
            log.info("清除充电枪 {} 的所有 WebSocket 连接，数量: {}", gunId, sessions.size());
        }
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
