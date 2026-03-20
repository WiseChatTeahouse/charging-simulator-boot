package chat.wisechat.charging.controller;

import chat.wisechat.charging.common.Result;
import chat.wisechat.charging.service.StationService;
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
        return Result.success(stationService.listStations());
    }

    /**
     * 获取站点详情
     */
    @GetMapping("/{stationId}")
    public Result<StationDetailVO> getStationDetail(@PathVariable Long stationId) {
        return Result.success(stationService.getStationDetail(stationId));
    }
}
