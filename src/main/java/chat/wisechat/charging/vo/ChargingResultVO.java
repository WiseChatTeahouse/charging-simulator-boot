package chat.wisechat.charging.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 充电结果 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class ChargingResultVO {
    
    private Long sessionId;
    
    private BigDecimal totalPower;
    
    private Long chargingDuration;
    
    private String message;
}
