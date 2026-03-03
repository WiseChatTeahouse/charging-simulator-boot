package chat.wisechat.charging.dto;

import lombok.Data;

/**
 * 插枪请求
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class InsertGunRequest {
    
    private Long gunId;
    
    private String vehicleId;
}
