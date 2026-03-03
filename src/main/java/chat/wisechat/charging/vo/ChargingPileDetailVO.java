package chat.wisechat.charging.vo;

import lombok.Data;

import java.util.List;

/**
 * 充电桩详情 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class ChargingPileDetailVO {
    
    private Long id;
    
    private Long stationId;
    
    private String pileCode;
    
    private Integer status;
    
    private String statusText;
    
    private List<ChargingGunVO> guns;
}
