package chat.wisechat.charging.entity;


import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_emulator_messages")
public class EmulatorMessage {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long groupId;

    private Integer stepOrder;

    private String payload; // 对应数据库的 JSON 字段

}