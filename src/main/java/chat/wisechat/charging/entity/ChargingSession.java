package chat.wisechat.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电会话实体类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
@TableName("t_charging_session")
public class ChargingSession {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long gunId;
    
    private String vehicleId;
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Integer status;
    
    private BigDecimal totalPower;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
