package chat.wisechat.charging.websocket;

import chat.wisechat.charging.service.WebSocketService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 充电 WebSocket 处理器
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@Component
public class ChargingWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private WebSocketService webSocketService;
    
    private static final Pattern SESSION_ID_PATTERN = Pattern.compile("/ws/charging/(\\d+)");
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        try {
            Long sessionId = parseSessionId(session);
            if (sessionId != null) {
                webSocketService.registerSession(sessionId, session);
                log.info("WebSocket 连接建立: sessionId={}, wsSessionId={}", sessionId, session.getId());
            } else {
                log.warn("无法解析会话ID，关闭连接: {}", session.getUri());
                session.close();
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 连接失败", e);
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        try {
            Long sessionId = parseSessionId(session);
            if (sessionId != null) {
                webSocketService.unregisterSession(sessionId, session);
                log.info("WebSocket 连接关闭: sessionId={}, wsSessionId={}, status={}", 
                        sessionId, session.getId(), status);
            }
        } catch (Exception e) {
            log.error("处理 WebSocket 关闭失败", e);
        }
    }
    
    private Long parseSessionId(WebSocketSession session) {
        String uri = session.getUri().toString();
        Matcher matcher = SESSION_ID_PATTERN.matcher(uri);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(1));
        }
        return null;
    }
}
