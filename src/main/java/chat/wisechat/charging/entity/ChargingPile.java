package chat.wisechat.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 充电桩实体类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
@TableName("t_charging_pile")
public class ChargingPile {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long stationId;
    
    private String pileCode;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
