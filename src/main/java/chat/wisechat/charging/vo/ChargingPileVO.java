package chat.wisechat.charging.vo;

import lombok.Data;

/**
 * 充电桩 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class ChargingPileVO {
    
    private Long id;
    
    private Long stationId;
    
    private String pileCode;
    
    private Integer status;
    
    private String statusText;
}
