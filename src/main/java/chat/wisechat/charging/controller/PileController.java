package chat.wisechat.charging.controller;

import chat.wisechat.charging.common.Result;
import chat.wisechat.charging.service.StationService;
import chat.wisechat.charging.vo.ChargingPileDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 充电桩控制器
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@RestController
@RequestMapping("/api/piles")
public class PileController {
    
    @Autowired
    private StationService stationService;
    
    /**
     * 获取充电桩详情
     */
    @GetMapping("/{pileId}")
    public Result<ChargingPileDetailVO> getPileDetail(@PathVariable Long pileId) {
        log.info("查询充电桩详情: {}", pileId);
        if (pileId == null || pileId <= 0) {
            return Result.error("充电桩ID无效");
        }
        ChargingPileDetailVO detail = stationService.getPileDetail(pileId);
        return Result.success(detail);
    }
}
