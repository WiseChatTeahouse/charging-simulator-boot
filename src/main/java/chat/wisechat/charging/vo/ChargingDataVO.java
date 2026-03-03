package chat.wisechat.charging.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电数据 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class ChargingDataVO {
    
    private BigDecimal voltage;
    
    private BigDecimal current;
    
    private BigDecimal power;
    
    private Integer soc;
    
    private LocalDateTime timestamp;
}
