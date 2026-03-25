package chat.wisechat.charging.service;

import chat.wisechat.charging.core.ChargingCacheManager;
import chat.wisechat.charging.entity.EmulatorMessage;
import chat.wisechat.charging.vo.VehicleBmsDataVO;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
    private ChargingCacheManager chargingCacheManager;

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
            log.debug("收到 MQTT 消息: topic={}", topic);

            if (topic.matches("charging/station/\\d+/pile/\\d+/gun/\\d+/data")) {
                handleChargingData(topic, payload);
            } else if (topic.matches("vehicle/.+/bms/data")) {
                handleBmsData(topic, payload);
            } else {
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
     * 处理充电数据 - 从 topic 解析 gunId，直接推送到 WebSocket
     */
    private void handleChargingData(String topic, String payload) {
        try {
            Long gunId = parseGunIdFromTopic(topic);
            if (gunId == null) {
                log.warn("无法从主题解析 gunId: {}", topic);
                return;
            }

            // 校验充电枪是否处于充电中（本地缓存）
            if (!chargingCacheManager.isCharging(gunId)) {
                log.warn("充电枪 {} 未处于充电状态，跳过推送", gunId);
                return;
            }

            EmulatorMessage msg = JSON.parseObject(payload, EmulatorMessage.class);
            webSocketService.buildPushMsg(gunId, msg);
            log.info("充电数据已推送: gunId={} payload={}", gunId, payload);

        } catch (Exception e) {
            log.error("处理充电数据失败: topic={}", topic, e);
        }
    }

    /**
     * 处理 BMS 数据 - 通过 vehicleId 从本地缓存找到 gunId，再推送到 WebSocket
     */
    private void handleBmsData(String topic, String payload) {
        try {
            String vehicleId = parseVehicleIdFromTopic(topic);
            if (vehicleId == null) {
                log.warn("无法从主题解析 vehicleId: {}", topic);
                return;
            }

            Long gunId = chargingCacheManager.getGunIdByVehicleId(vehicleId);
            if (gunId == null) {
                log.warn("车辆 {} 没有活跃的充电枪，跳过推送", vehicleId);
                return;
            }

            VehicleBmsDataVO data = JSON.parseObject(payload, VehicleBmsDataVO.class);
            webSocketService.pushBmsData(gunId, data);
            log.debug("BMS 数据已推送: vehicleId={} gunId={}", vehicleId, gunId);

        } catch (Exception e) {
            log.error("处理 BMS 数据失败: topic={}", topic, e);
        }
    }

    private Long parseGunIdFromTopic(String topic) {
        Matcher matcher = CHARGING_TOPIC_PATTERN.matcher(topic);
        return matcher.find() ? Long.parseLong(matcher.group(3)) : null;
    }

    private String parseVehicleIdFromTopic(String topic) {
        Matcher matcher = VEHICLE_TOPIC_PATTERN.matcher(topic);
        return matcher.find() ? matcher.group(1) : null;
    }
}
