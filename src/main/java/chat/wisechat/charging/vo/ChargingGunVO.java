package chat.wisechat.charging.vo;

import lombok.Data;

/**
 * 充电枪 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class ChargingGunVO {
    
    private Long id;
    
    private Long pileId;
    
    private String gunCode;
    
    private Integer type;
    
    private String typeText;
    
    private Integer status;
    
    private String statusText;
}
