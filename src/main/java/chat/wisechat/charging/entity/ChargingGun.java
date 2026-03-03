package chat.wisechat.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

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
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
