package chat.wisechat.charging.vo;

import lombok.Data;

import java.util.List;

/**
 * 充电站详情 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class StationDetailVO {
    
    private Long id;
    
    private String name;
    
    private String address;
    
    private Integer status;
    
    private String statusText;
    
    private List<ChargingPileDetailVO> piles;
}
