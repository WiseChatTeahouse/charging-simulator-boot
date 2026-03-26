package chat.wisechat.charging.service;

import chat.wisechat.charging.entity.EmulatorMessage;
import chat.wisechat.charging.vo.ChargingDataVO;
import chat.wisechat.charging.vo.VehicleBmsDataVO;
import com.alibaba.fastjson2.JSON;
import lombok.Getter;
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

    public void buildPushMsg(Long gunId, EmulatorMessage msg) {
        WebSocketMessage message = new WebSocketMessage();
        String messageType = ChargingPayloadType.getMessagePayload(msg.getType());
        message.setType(messageType);
        if ("BMS_DATA".equals(messageType)) {
            com.alibaba.fastjson2.JSONObject raw = JSON.parseObject(msg.getPayload());
            VehicleBmsDataVO bmsData = new VehicleBmsDataVO();
            bmsData.setCif(raw.getInteger("cif"));
            bmsData.setMsg(raw.getString("msg"));
            bmsData.setSoc(raw.getInteger("soc"));
            bmsData.setMode(raw.getInteger("mode"));
            bmsData.setMCur(raw.getDouble("m_cur"));
            bmsData.setMVol(raw.getDouble("m_vol"));
            bmsData.setRCur(raw.getDouble("r_cur"));
            bmsData.setRVol(raw.getDouble("r_vol"));
            bmsData.setSCur(raw.getDouble("s_cur"));
            bmsData.setSVol(raw.getDouble("s_vol"));
            bmsData.setMaxTemp(raw.getInteger("maxTemp"));
            bmsData.setMinTemp(raw.getInteger("minTemp"));
            bmsData.setTradeID(raw.getLong("tradeID"));
            bmsData.setCellMaxVol(raw.getDouble("cellMaxVol"));
            bmsData.setRemainTime(raw.getInteger("remainTime"));
            message.setData(bmsData);
        } else if ("ycMeas".equals(msg.getType())) {
            com.alibaba.fastjson2.JSONObject raw = JSON.parseObject(msg.getPayload());
            ChargingDataVO chargingData = new ChargingDataVO();
            chargingData.setEnergy(raw.getBigDecimal("energy"));
            message.setData(chargingData);
        } else {
            message.setData(msg.getPayload());
        }
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

    @Getter
    private enum ChargingPayloadType {
        BMS("ycBMS", "BMS_DATA"),
        START_CHARGING("start", "CHARGING_DATA"),
        MEAS("ycMeas", "CHARGING_DATA");


        private final String payloadType;
        private final String messagePayload;

        ChargingPayloadType(String payloadType, String messagePayload) {
            this.payloadType = payloadType;
            this.messagePayload = messagePayload;
        }

        public static String getMessagePayload(String payloadType) {
            for (ChargingPayloadType type : values()) {
                if (type.getPayloadType().equals(payloadType)) {
                    return type.getMessagePayload();
                }
            }
            return null;
        }
    }
}
