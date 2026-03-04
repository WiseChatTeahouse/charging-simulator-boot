package chat.wisechat.charging.service;

import chat.wisechat.charging.config.MqttConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * MQTT 服务类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@Service
public class MqttService {
    
    @Autowired
    private MqttConfig mqttConfig;
    
    @Autowired
    private MqttMessageHandler messageHandler;
    
    private MqttClient mqttClient;
    
    @PostConstruct
    public void init() {
        try {
            log.info("初始化 MQTT 客户端: {}", mqttConfig.getBrokerUrl());
            
            mqttClient = new MqttClient(mqttConfig.getBrokerUrl(), mqttConfig.getClientId());
            
            MqttConnectOptions options = new MqttConnectOptions();
            options.setUserName(mqttConfig.getUsername());
            options.setPassword(mqttConfig.getPassword().toCharArray());
            options.setCleanSession(false); // 改为 false 以保持订阅
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(60);
            
            // 设置回调
            mqttClient.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.error("MQTT 连接丢失，等待自动重连...", cause);
                    messageHandler.connectionLost(cause);
                }
                
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    messageHandler.messageArrived(topic, message);
                }
                
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    messageHandler.deliveryComplete(token);
                }
            });
            
            // 连接并在连接成功后订阅
            mqttClient.connect(options);
            
            log.info("MQTT 客户端连接成功");
            
            // 订阅主题
            subscribeTopics();
            
        } catch (MqttException e) {
            log.error("MQTT 客户端初始化失败", e);
        }
    }
    
    /**
     * 订阅主题
     */
    public void subscribeTopics() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                // 订阅充电数据主题
                String chargingTopic = mqttConfig.getTopics().getChargingData();
                mqttClient.subscribe(chargingTopic, mqttConfig.getQos());
                log.info("✓ 成功订阅充电数据主题: {} (QoS: {})", chargingTopic, mqttConfig.getQos());
                
                // 订阅 BMS 数据主题
                String bmsTopic = mqttConfig.getTopics().getBmsData();
                mqttClient.subscribe(bmsTopic, mqttConfig.getQos());
                log.info("✓ 成功订阅 BMS 数据主题: {} (QoS: {})", bmsTopic, mqttConfig.getQos());
                
                log.info("所有 MQTT 主题订阅完成，等待消息...");
            } else {
                log.error("MQTT 客户端未连接，无法订阅主题");
            }
        } catch (MqttException e) {
            log.error("订阅主题失败", e);
        }
    }
    
    /**
     * 订阅充电数据
     */
    public void subscribeChargingData(Long gunId) {
        try {
            String topic = "charging/+/pile/+/gun/" + gunId + "/data";
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic, mqttConfig.getQos());
                log.info("订阅充电枪数据: {}", topic);
            }
        } catch (MqttException e) {
            log.error("订阅充电枪数据失败: gunId={}", gunId, e);
        }
    }
    
    /**
     * 订阅 BMS 数据
     */
    public void subscribeBmsData(String vehicleId) {
        try {
            String topic = "vehicle/" + vehicleId + "/bms/data";
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.subscribe(topic, mqttConfig.getQos());
                log.info("订阅车辆 BMS 数据: {}", topic);
            }
        } catch (MqttException e) {
            log.error("订阅车辆 BMS 数据失败: vehicleId={}", vehicleId, e);
        }
    }
    
    /**
     * 发布控制命令
     */
    public void publishControlCommand(Long gunId, String command) {
        try {
            String topic = "charging/+/pile/+/gun/" + gunId + "/control";
            if (mqttClient != null && mqttClient.isConnected()) {
                MqttMessage message = new MqttMessage(command.getBytes());
                message.setQos(mqttConfig.getQos());
                mqttClient.publish(topic, message);
                log.info("发布控制命令: topic={}, command={}", topic, command);
            }
        } catch (MqttException e) {
            log.error("发布控制命令失败: gunId={}, command={}", gunId, command, e);
        }
    }
    
    @PreDestroy
    public void destroy() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
                mqttClient.close();
                log.info("MQTT 客户端已关闭");
            }
        } catch (MqttException e) {
            log.error("关闭 MQTT 客户端失败", e);
        }
    }
}
