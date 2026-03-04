package chat.wisechat.charging.service;

import chat.wisechat.charging.vo.ChargingDataVO;
import chat.wisechat.charging.vo.VehicleBmsDataVO;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * MQTT 消息处理器
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@Component
public class MqttMessageHandler implements MqttCallback {
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final Pattern CHARGING_TOPIC_PATTERN = 
            Pattern.compile("charging/station/(\\d+)/pile/(\\d+)/gun/(\\d+)/data");
    
    private static final Pattern VEHICLE_TOPIC_PATTERN = 
            Pattern.compile("vehicle/([^/]+)/bms/data");
    
    @Override
    public void connectionLost(Throwable cause) {
        log.error("MQTT 连接丢失，等待自动重连...", cause);
    }
    
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        try {
            String payload = new String(message.getPayload());
            log.info("收到 MQTT 消息: topic={}, payload={}", topic, payload);
            
            // 匹配充电数据主题: charging/station/{stationId}/pile/{pileId}/gun/{gunId}/data
            if (topic.matches("charging/station/\\d+/pile/\\d+/gun/\\d+/data")) {
                handleChargingData(topic, payload);
            } 
            // 匹配车辆 BMS 数据主题: vehicle/{vehicleId}/bms/data
            else if (topic.matches("vehicle/.+/bms/data")) {
                handleBmsData(topic, payload);
            }
            else {
                log.warn("未知的主题格式: {}", topic);
            }
        } catch (Exception e) {
            log.error("处理 MQTT 消息失败: topic={}", topic, e);
        }
    }
    
    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        log.debug("消息发送完成");
    }
    
    /**
     * 处理充电数据
     */
    private void handleChargingData(String topic, String payload) {
        try {
            log.info("开始处理充电数据: topic={}", topic);
            
            // 解析主题获取 gunId
            Long gunId = parseGunIdFromTopic(topic);
            if (gunId == null) {
                log.warn("无法从主题解析 gunId: {}", topic);
                return;
            }
            
            log.info("解析到 gunId: {}", gunId);
            
            // 解析充电数据
            ChargingDataVO data = JSON.parseObject(payload, ChargingDataVO.class);
            log.info("解析充电数据成功: {}", data);
            
            // 获取充电会话ID
            Long sessionId = getActiveSessionByGunId(gunId);
            if (sessionId == null) {
                log.warn("充电枪 {} 没有活跃会话，跳过推送", gunId);
                return;
            }
            
            log.info("找到活跃会话: sessionId={}", sessionId);
            
            // 推送到 WebSocket
            webSocketService.pushChargingData(sessionId, data);
            
            // 缓存到 Redis
            String cacheKey = "charging:data:" + sessionId;
            redisTemplate.opsForValue().set(cacheKey, data, Duration.ofMinutes(5));
            
            log.info("充电数据已推送: sessionId={}, data={}", sessionId, data);
            
        } catch (Exception e) {
            log.error("处理充电数据失败: topic={}, payload={}", topic, payload, e);
        }
    }
    
    /**
     * 处理 BMS 数据
     */
    private void handleBmsData(String topic, String payload) {
        try {
            log.info("开始处理 BMS 数据: topic={}", topic);
            
            // 解析主题获取 vehicleId
            String vehicleId = parseVehicleIdFromTopic(topic);
            if (vehicleId == null) {
                log.warn("无法从主题解析 vehicleId: {}", topic);
                return;
            }
            
            log.info("解析到 vehicleId: {}", vehicleId);
            
            // 解析 BMS 数据
            VehicleBmsDataVO data = JSON.parseObject(payload, VehicleBmsDataVO.class);
            log.info("解析 BMS 数据成功: {}", data);
            
            // 获取充电会话ID
            Long sessionId = getActiveSessionByVehicleId(vehicleId);
            if (sessionId == null) {
                log.warn("车辆 {} 没有活跃会话，跳过推送", vehicleId);
                return;
            }
            
            log.info("找到活跃会话: sessionId={}", sessionId);
            
            // 推送到 WebSocket
            webSocketService.pushBmsData(sessionId, data);
            
            // 缓存到 Redis
            String cacheKey = "bms:data:" + sessionId;
            redisTemplate.opsForValue().set(cacheKey, data, Duration.ofMinutes(5));
            
            log.info("BMS 数据已推送: sessionId={}, data={}", sessionId, data);
            
        } catch (Exception e) {
            log.error("处理 BMS 数据失败: topic={}, payload={}", topic, payload, e);
        }
    }
    
    /**
     * 从主题解析 gunId
     */
    private Long parseGunIdFromTopic(String topic) {
        Matcher matcher = CHARGING_TOPIC_PATTERN.matcher(topic);
        if (matcher.find()) {
            return Long.parseLong(matcher.group(3));
        }
        return null;
    }
    
    /**
     * 从主题解析 vehicleId
     */
    private String parseVehicleIdFromTopic(String topic) {
        Matcher matcher = VEHICLE_TOPIC_PATTERN.matcher(topic);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * 根据 gunId 获取活跃会话ID
     */
    private Long getActiveSessionByGunId(Long gunId) {
        String key = "gun:session:" + gunId;
        Object sessionId = redisTemplate.opsForValue().get(key);
        return sessionId != null ? Long.parseLong(sessionId.toString()) : null;
    }
    
    /**
     * 根据 vehicleId 获取活跃会话ID
     */
    private Long getActiveSessionByVehicleId(String vehicleId) {
        String key = "vehicle:session:" + vehicleId;
        Object sessionId = redisTemplate.opsForValue().get(key);
        return sessionId != null ? Long.parseLong(sessionId.toString()) : null;
    }
}
