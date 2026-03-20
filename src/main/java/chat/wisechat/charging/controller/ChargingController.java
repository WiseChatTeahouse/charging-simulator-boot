package chat.wisechat.charging.controller;

import chat.wisechat.charging.common.Result;
import chat.wisechat.charging.dto.InsertGunRequest;
import chat.wisechat.charging.dto.RemoveGunRequest;
import chat.wisechat.charging.dto.StartChargingRequest;
import chat.wisechat.charging.dto.StopChargingRequest;
import chat.wisechat.charging.service.ChargingService;
import chat.wisechat.charging.vo.ChargingGunSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 充电控制器
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@RestController
@RequestMapping("/api/charging")
public class ChargingController {

    @Autowired
    private ChargingService chargingService;

    /**
     * 插枪
     */
    @PostMapping("/insert-gun")
    public Result<Long> insertGun(@RequestBody InsertGunRequest request) {
        return Result.success(chargingService.insertGun(request.getGunId()));
    }

    /**
     * 启动充电
     */
    @PostMapping("/start")
    public Result<Void> startCharging(@RequestBody StartChargingRequest request) {
        log.info("启动充电请求: {}", request);

        Long gunId = request.getGunId();
        if (gunId == null || gunId <= 0) {
            return Result.error("充电枪ID无效");
        }

        chargingService.startCharging(gunId);
        return Result.success();
    }

    /**
     * 结束充电
     */
    @PostMapping("/stop")
    public Result<Void> stopCharging(@RequestBody StopChargingRequest request) {
        log.info("结束充电请求: {}", request);

        Long gunId = request.getGunId();
        if (gunId == null || gunId <= 0) {
            return Result.error("充电枪ID无效");
        }

        chargingService.stopCharging(gunId);
        return Result.success();
    }

    /**
     * 拔枪
     */
    @PostMapping("/remove-gun")
    public Result<Void> removeGun(@RequestBody RemoveGunRequest request) {
        log.info("拔枪请求: {}", request);

        Long gunId = request.getGunId();
        if (gunId == null || gunId <= 0) {
            return Result.error("充电枪ID无效");
        }

        chargingService.removeGun(gunId);
        return Result.success();
    }

    /**
     * 获取充电会话状态（兼容旧接口）
     */
    @GetMapping("/session/{sessionId}")
    public Result<ChargingGunSessionVO> getSessionStatus(@PathVariable Long sessionId) {
        log.info("查询会话状态: {}", sessionId);

        if (sessionId == null || sessionId <= 0) {
            return Result.error("充电枪ID无效");
        }

        // sessionId现在就是gunId
        ChargingGunSessionVO session = chargingService.getSessionStatus(sessionId);
        return Result.success(session);
    }

    /**
     * 根据充电枪ID获取活跃会话
     */
    @GetMapping("/active-session")
    public Result<ChargingGunSessionVO> getActiveSession(@RequestParam Long gunId) {
        log.info("查询充电枪活跃会话: gunId={}", gunId);

        if (gunId == null || gunId <= 0) {
            return Result.error("充电枪ID无效");
        }

        ChargingGunSessionVO session = chargingService.getActiveSessionByGunId(gunId);
        return Result.success(session);
    }

    /**
     * 重置充电枪状态
     */
    @PostMapping("/reset")
    public Result<Void> resetGun(@RequestParam Long gunId) {
        log.info("重置充电枪请求: gunId={}", gunId);

        if (gunId == null || gunId <= 0) {
            return Result.error("充电枪ID无效");
        }

        chargingService.resetGun(gunId);
        return Result.success();
    }
}