package chat.wisechat.charging.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电枪会话 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class ChargingGunSessionVO {
    
    private Long gunId;
    
    private String gunCode;
    
    private Integer type;
    
    private String typeText;
    
    /**
     * 状态：0-空闲，1-已插枪，2-充电中，3-故障
     */
    private Integer status;
    
    private String statusText;
    
    /**
     * 车辆ID
     */
    private String vehicleId;
    
    /**
     * 充电开始时间
     */
    private LocalDateTime startTime;
    
    /**
     * 充电结束时间
     */
    private LocalDateTime endTime;
    
    /**
     * 总电量(kWh)
     */
    private BigDecimal totalPower;
    
    /**
     * 充电时长（分钟）
     */
    private Long chargingDuration;
}