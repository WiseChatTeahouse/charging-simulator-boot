package chat.wisechat.charging.controller;

import chat.wisechat.charging.common.Result;
import chat.wisechat.charging.config.MqttConfig;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * MQTT 测试控制器
 *
 * @author siberia.hu
 * @date 2026/3/4
 */
@Slf4j
@RestController
@RequestMapping("/api/mqtt/test")
public class MqttTestController {
    
    @Autowired
    private MqttConfig mqttConfig;
    
    /**
     * 发送测试充电数据
     */
    @PostMapping("/charging-data")
    public Result<String> sendTestChargingData(
            @RequestParam(defaultValue = "1") Long stationId,
            @RequestParam(defaultValue = "1") Long pileId,
            @RequestParam(defaultValue = "1") Long gunId) {
        try {
            String topic = String.format("charging/station/%d/pile/%d/gun/%d/data", stationId, pileId, gunId);
            String payload = String.format(
                "{\"voltage\":220.5,\"current\":32.0,\"power\":7.056,\"soc\":45.5,\"chargedEnergy\":12.5,\"chargingTime\":3600,\"temperature\":25.0}");
            
            MqttClient client = new MqttClient(mqttConfig.getBrokerUrl(), "test-publisher-" + System.currentTimeMillis());
            client.connect();
            
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            client.publish(topic, message);
            
            client.disconnect();
            client.close();
            
            log.info("发送测试充电数据成功: topic={}, payload={}", topic, payload);
            return Result.success("发送成功: " + topic);
        } catch (Exception e) {
            log.error("发送测试充电数据失败", e);
            return Result.error("发送失败: " + e.getMessage());
        }
    }
    
    /**
     * 发送测试 BMS 数据
     */
    @PostMapping("/bms-data")
    public Result<String> sendTestBmsData(@RequestParam(defaultValue = "TEST001") String vehicleId) {
        try {
            String topic = String.format("vehicle/%s/bms/data", vehicleId);
            String payload = String.format(
                "{\"batteryVoltage\":400.0,\"batteryCurrent\":30.0,\"soc\":50.0,\"soh\":95.0,\"batteryTemp\":28.0,\"cellMaxVoltage\":4.2,\"cellMinVoltage\":3.8}");
            
            MqttClient client = new MqttClient(mqttConfig.getBrokerUrl(), "test-publisher-" + System.currentTimeMillis());
            client.connect();
            
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            client.publish(topic, message);
            
            client.disconnect();
            client.close();
            
            log.info("发送测试 BMS 数据成功: topic={}, payload={}", topic, payload);
            return Result.success("发送成功: " + topic);
        } catch (Exception e) {
            log.error("发送测试 BMS 数据失败", e);
            return Result.error("发送失败: " + e.getMessage());
        }
    }
}
