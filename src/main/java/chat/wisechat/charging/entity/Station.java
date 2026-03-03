package chat.wisechat.charging.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 充电站实体类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
@TableName("t_station")
public class Station {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String address;
    
    private Integer status;
    
    private LocalDateTime createTime;
    
    private LocalDateTime updateTime;
}
