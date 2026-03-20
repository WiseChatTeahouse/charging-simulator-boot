package chat.wisechat.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 充电枪实体类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
@TableName("t_charging_gun")
public class ChargingGun {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long pileId;
    
    private String gunCode;
    
    private Integer type;
    
    /**
     * 状态：0-空闲，1-已插枪，2-充电中，3-故障
     */
    private Integer status;
    
    /**
     * 车辆ID（插枪时设置）
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
     * 乐观锁版本号
     */
    @Version
    private Integer version;

    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
