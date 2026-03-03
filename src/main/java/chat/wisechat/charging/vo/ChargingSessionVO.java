package chat.wisechat.charging.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电会话 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class ChargingSessionVO {
    
    private Long id;
    
    private Long gunId;
    
    private String vehicleId;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Integer status;
    
    private String statusText;
    
    private BigDecimal totalPower;
}
