package chat.wisechat.charging.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 车辆 BMS 数据 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class VehicleBmsDataVO {
    
    private BigDecimal batteryVoltage;
    
    private BigDecimal batteryCurrent;
    
    private BigDecimal batteryTemp;
    
    private Integer soc;
    
    private LocalDateTime timestamp;
}
