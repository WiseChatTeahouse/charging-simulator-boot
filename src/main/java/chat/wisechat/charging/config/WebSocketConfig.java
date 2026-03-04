package chat.wisechat.charging.config;

import chat.wisechat.charging.websocket.ChargingWebSocketHandler;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.websockets.jsr.WebSocketDeploymentInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Autowired
    private ChargingWebSocketHandler chargingWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(chargingWebSocketHandler, "/ws/charging/{sessionId}")
                .setAllowedOrigins("*");
    }
    
    /**
     * 配置 Undertow WebSocket 缓冲池
     */
    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowWebSocketCustomizer() {
        return factory -> factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            WebSocketDeploymentInfo webSocketDeploymentInfo = new WebSocketDeploymentInfo();
            // 配置缓冲池：直接缓冲区，每个缓冲区1024字节，最多100个缓冲区
            webSocketDeploymentInfo.setBuffers(new DefaultByteBufferPool(true, 1024, 100, 12));
            deploymentInfo.addServletContextAttribute(
                    WebSocketDeploymentInfo.ATTRIBUTE_NAME,
                    webSocketDeploymentInfo
            );
        });
    }
}
