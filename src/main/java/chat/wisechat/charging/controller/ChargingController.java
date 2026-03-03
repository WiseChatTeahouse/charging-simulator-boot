package chat.wisechat.charging.controller;

import chat.wisechat.charging.common.Result;
import chat.wisechat.charging.dto.InsertGunRequest;
import chat.wisechat.charging.dto.RemoveGunRequest;
import chat.wisechat.charging.dto.StartChargingRequest;
import chat.wisechat.charging.dto.StopChargingRequest;
import chat.wisechat.charging.service.ChargingService;
import chat.wisechat.charging.vo.ChargingResultVO;
import chat.wisechat.charging.vo.ChargingSessionVO;
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
        log.info("插枪请求: {}", request);
        
        if (request.getGunId() == null || request.getGunId() <= 0) {
            return Result.error("充电枪ID无效");
        }
        
        if (request.getVehicleId() == null || request.getVehicleId().trim().isEmpty()) {
            return Result.error("车辆ID不能为空");
        }
        
        Long sessionId = chargingService.insertGun(request.getGunId(), request.getVehicleId());
        return Result.success(sessionId);
    }
    
    /**
     * 启动充电
     */
    @PostMapping("/start")
    public Result<Void> startCharging(@RequestBody StartChargingRequest request) {
        log.info("启动充电请求: {}", request);
        
        if (request.getSessionId() == null || request.getSessionId() <= 0) {
            return Result.error("会话ID无效");
        }
        
        chargingService.startCharging(request.getSessionId());
        return Result.success();
    }
    
    /**
     * 结束充电
     */
    @PostMapping("/stop")
    public Result<ChargingResultVO> stopCharging(@RequestBody StopChargingRequest request) {
        log.info("结束充电请求: {}", request);
        
        if (request.getSessionId() == null || request.getSessionId() <= 0) {
            return Result.error("会话ID无效");
        }
        
        ChargingResultVO result = chargingService.stopCharging(request.getSessionId());
        return Result.success(result);
    }
    
    /**
     * 拔枪
     */
    @PostMapping("/remove-gun")
    public Result<Void> removeGun(@RequestBody RemoveGunRequest request) {
        log.info("拔枪请求: {}", request);
        
        if (request.getSessionId() == null || request.getSessionId() <= 0) {
            return Result.error("会话ID无效");
        }
        
        chargingService.removeGun(request.getSessionId());
        return Result.success();
    }
    
    /**
     * 获取充电会话状态
     */
    @GetMapping("/session/{sessionId}")
    public Result<ChargingSessionVO> getSessionStatus(@PathVariable Long sessionId) {
        log.info("查询会话状态: {}", sessionId);
        
        if (sessionId == null || sessionId <= 0) {
            return Result.error("会话ID无效");
        }
        
        ChargingSessionVO session = chargingService.getSessionStatus(sessionId);
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