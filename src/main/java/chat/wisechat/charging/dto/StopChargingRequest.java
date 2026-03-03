package chat.wisechat.charging.dto;

import lombok.Data;

/**
 * 结束充电请求
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class StopChargingRequest {
    
    private Long sessionId;
}
