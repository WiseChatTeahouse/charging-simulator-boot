package chat.wisechat.charging.controller;

import chat.wisechat.charging.common.Result;
import chat.wisechat.charging.service.StationService;
import chat.wisechat.charging.vo.ChargingPileDetailVO;
import chat.wisechat.charging.vo.StationDetailVO;
import chat.wisechat.charging.vo.StationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 站点控制器
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@RestController
@RequestMapping("/api/stations")
public class StationController {
    
    @Autowired
    private StationService stationService;
    
    /**
     * 获取站点列表
     */
    @GetMapping
    public Result<List<StationVO>> listStations() {
        log.info("查询站点列表");
        List<StationVO> stations = stationService.listStations();
        return Result.success(stations);
    }
    
    /**
     * 获取站点详情
     */
    @GetMapping("/{stationId}")
    public Result<StationDetailVO> getStationDetail(@PathVariable Long stationId) {
        log.info("查询站点详情: {}", stationId);
        if (stationId == null || stationId <= 0) {
            return Result.error("站点ID无效");
        }
        StationDetailVO detail = stationService.getStationDetail(stationId);
        return Result.success(detail);
    }
}
