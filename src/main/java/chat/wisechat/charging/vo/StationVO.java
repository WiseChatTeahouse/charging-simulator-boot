package chat.wisechat.charging.vo;

import lombok.Data;

/**
 * 充电站 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class StationVO {
    
    private Long id;
    
    private String name;
    
    private String address;
    
    private Integer status;
    
    private String statusText;
}
