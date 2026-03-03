package chat.wisechat.charging.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * MQTT 配置类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "mqtt")
public class MqttConfig {
    
    private String brokerUrl;
    
    private String clientId;
    
    private String username;
    
    private String password;
    
    private Topics topics;
    
    private Integer qos;
    
    @Data
    public static class Topics {
        private String chargingData;
        private String bmsData;
    }
}
