package chat.wisechat.charging.vo;

import com.alibaba.fastjson2.annotation.JSONField;
import lombok.Data;

/**
 * 车辆 BMS 数据 VO
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Data
public class VehicleBmsDataVO {

    private Integer cif;

    private String msg;

    private Integer soc;

    private Integer mode;

    @JSONField(name = "mCur")
    private Double mCur;

    @JSONField(name = "mVol")
    private Double mVol;

    @JSONField(name = "rCur")
    private Double rCur;

    @JSONField(name = "rVol")
    private Double rVol;

    @JSONField(name = "sCur")
    private Double sCur;

    @JSONField(name = "sVol")
    private Double sVol;

    private Integer maxTemp;

    private Integer minTemp;

    private Long tradeID;

    private Double cellMaxVol;

    private Integer remainTime;
}
