package chat.wisechat.charging.service;

import chat.wisechat.charging.entity.ChargingGun;
import chat.wisechat.charging.exception.BusinessException;
import chat.wisechat.charging.mapper.ChargingGunMapper;
import chat.wisechat.charging.vo.ChargingDataVO;
import chat.wisechat.charging.vo.ChargingGunSessionVO;
import chat.wisechat.charging.vo.ChargingResultVO;
import chat.wisechat.charging.vo.VehicleBmsDataVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 充电服务类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@Service
public class ChargingService {

    @Autowired
    private ChargingGunMapper gunMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StationService stationService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private Cache<Long, ChargingDataVO> chargingDataCache;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    // gunId -> 推送任务
    private final Map<Long, ScheduledFuture<?>> chargingTasks = new ConcurrentHashMap<>();

    private final Random random = new Random();

    // 本地锁保证操作的原子性 - 每个充电枪一个锁
    private final ConcurrentHashMap<Long, ReentrantLock> gunLocks = new ConcurrentHashMap<>();

    /**
     * 获取充电枪锁
     */
    private ReentrantLock getGunLock(Long gunId) {
        return gunLocks.computeIfAbsent(gunId, k -> new ReentrantLock());
    }

    /**
     * 插枪操作 - 使用乐观锁保证并发安全，只更新枪状态和车辆ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long insertGun(Long gunId) {
        ChargingGun gun = gunMapper.selectById(gunId);
        if (gun == null) {
            throw new BusinessException("充电枪不存在");
        }

        if (gun.getStatus() != 0) {
            throw new BusinessException("充电枪状态异常，无法插枪。当前状态: " + getGunStatusText(gun.getStatus()));
        }

        // 乐观锁条件更新：只修改 status，version 由 @Version 自动处理
        LambdaUpdateWrapper<ChargingGun> wrapper = new LambdaUpdateWrapper<ChargingGun>()
                .eq(ChargingGun::getId, gunId)
                .eq(ChargingGun::getStatus, 0)
                .eq(ChargingGun::getVersion, gun.getVersion())
                .set(ChargingGun::getStatus, 1)
                .set(ChargingGun::getVersion, gun.getVersion() + 1);

        int rows = gunMapper.update(null, wrapper);
        if (rows == 0) {
            throw new BusinessException("插枪失败，充电枪状态已变更，请重试");
        }
        return gunId;
    }

    /**
     * 启动充电 - 更新枪状态为充电中(2)，写入本地缓存，启动 WebSocket 推送任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void startCharging(Long gunId) {
        log.info("启动充电: gunId={}", gunId);

        ReentrantLock lock = getGunLock(gunId);
        lock.lock();
        try {
            ChargingGun gun = gunMapper.selectById(gunId);
            if (gun == null) {
                throw new BusinessException("充电枪不存在");
            }

            if (gun.getStatus() != 1) {
                throw new BusinessException("充电枪状态异常，无法启动充电。当前状态: " + getGunStatusText(gun.getStatus()));
            }

            // 更新枪状态为充电中(2)
            gun.setStatus(2);
            gun.setStartTime(LocalDateTime.now());
            gun.setEndTime(null);
            gun.setTotalPower(null);
            gunMapper.updateById(gun);

            // 写入 Caffeine 本地缓存
            ChargingDataVO initialData = buildChargingData(gunId, 20);
            chargingDataCache.put(gunId, initialData);

            // 更新 Redis 缓存
            String cacheKey = "gun:status:" + gunId;
            redisTemplate.opsForValue().set(cacheKey, gun, 30, TimeUnit.MINUTES);

            // 清除充电桩详情缓存
            stationService.clearPileDetailCache(gun.getPileId());

            // 启动定时推送任务（每2秒推送一次充电数据和BMS数据）
            startChargingPushTask(gunId);

            log.info("充电启动成功: gunId={}", gunId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 启动充电数据推送定时任务
     */
    private void startChargingPushTask(Long gunId) {
        stopChargingPushTask(gunId);
        ScheduledFuture<?> future = taskScheduler.scheduleAtFixedRate(() -> {
            try {
                ChargingDataVO chargingData = buildChargingData(gunId, null);
                chargingDataCache.put(gunId, chargingData);
                webSocketService.pushChargingData(gunId, chargingData);
                webSocketService.pushBmsData(gunId, buildBmsData(gunId));
            } catch (Exception e) {
                log.error("推送充电数据失败: gunId={}", gunId, e);
            }
        }, Duration.ofSeconds(2));
        chargingTasks.put(gunId, future);
        log.info("充电推送任务已启动: gunId={}", gunId);
    }

    /**
     * 停止充电数据推送定时任务
     */
    private void stopChargingPushTask(Long gunId) {
        ScheduledFuture<?> future = chargingTasks.remove(gunId);
        if (future != null) {
            future.cancel(false);
            log.info("充电推送任务已停止: gunId={}", gunId);
        }
    }

    /**
     * 构建模拟充电数据，soc 每次递增
     */
    private ChargingDataVO buildChargingData(Long gunId, Integer fixedSoc) {
        ChargingDataVO existing = chargingDataCache.getIfPresent(gunId);
        int soc = fixedSoc != null ? fixedSoc
                : (existing != null ? Math.min(existing.getSoc() + random.nextInt(2), 100) : 20);
        ChargingDataVO data = new ChargingDataVO();
        data.setVoltage(BigDecimal.valueOf(380 + random.nextInt(20)).setScale(1, RoundingMode.HALF_UP));
        data.setCurrent(BigDecimal.valueOf(50 + random.nextInt(30)).setScale(1, RoundingMode.HALF_UP));
        data.setPower(data.getVoltage().multiply(data.getCurrent())
                .divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP));
        data.setSoc(soc);
        data.setTimestamp(LocalDateTime.now());
        return data;
    }

    /**
     * 构建模拟 BMS 数据
     */
    private VehicleBmsDataVO buildBmsData(Long gunId) {
        ChargingDataVO charging = chargingDataCache.getIfPresent(gunId);
        VehicleBmsDataVO data = new VehicleBmsDataVO();
        data.setBatteryVoltage(BigDecimal.valueOf(350 + random.nextInt(30)).setScale(1, RoundingMode.HALF_UP));
        data.setBatteryCurrent(BigDecimal.valueOf(40 + random.nextInt(20)).setScale(1, RoundingMode.HALF_UP));
        data.setBatteryTemp(BigDecimal.valueOf(25 + random.nextInt(15)).setScale(1, RoundingMode.HALF_UP));
        data.setSoc(charging != null ? charging.getSoc() : 20);
        data.setTimestamp(LocalDateTime.now());
        return data;
    }

    /**
     * 结束充电
     */
    @Transactional(rollbackFor = Exception.class)
    public ChargingResultVO stopCharging(Long gunId) {
        log.info("结束充电: gunId={}", gunId);

        ReentrantLock lock = getGunLock(gunId);
        lock.lock();
        try {
            ChargingGun gun = gunMapper.selectById(gunId);
            if (gun == null) {
                throw new BusinessException("充电枪不存在");
            }

            if (gun.getStatus() != 2) {
                throw new BusinessException("充电枪状态异常，无法结束充电。当前状态: " + getGunStatusText(gun.getStatus()));
            }

            // 更新充电枪状态为已插枪（结束充电但未拔枪）
            gun.setStatus(1); // 已插枪
            gun.setEndTime(LocalDateTime.now());

            // 计算总电量（模拟值）
            if (gun.getStartTime() != null) {
                Duration duration = Duration.between(gun.getStartTime(), gun.getEndTime());
                long minutes = duration.toMinutes();
                // 假设平均功率 50kW
                BigDecimal totalPower = BigDecimal.valueOf(minutes * 50.0 / 60.0);
                gun.setTotalPower(totalPower);
            } else {
                gun.setTotalPower(BigDecimal.ZERO);
            }

            gunMapper.updateById(gun);

            // 停止推送任务，清除 Caffeine 缓存
            stopChargingPushTask(gunId);
            chargingDataCache.invalidate(gunId);

            // 更新缓存
            String cacheKey = "gun:status:" + gunId;
            redisTemplate.opsForValue().set(cacheKey, gun, 30, TimeUnit.MINUTES);

            // 清除充电桩详情缓存
            stationService.clearPileDetailCache(gun.getPileId());

            // 构建返回结果
            ChargingResultVO result = new ChargingResultVO();
            result.setSessionId(gunId); // 使用gunId作为sessionId
            result.setTotalPower(gun.getTotalPower());
            if (gun.getStartTime() != null && gun.getEndTime() != null) {
                result.setChargingDuration(Duration.between(gun.getStartTime(), gun.getEndTime()).toMinutes());
            }
            result.setMessage("充电结束");

            log.info("充电结束成功，gunId: {}, 总电量: {} kWh", gunId, gun.getTotalPower());
            return result;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 拔枪操作
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeGun(Long gunId) {
        log.info("拔枪操作: gunId={}", gunId);

        ReentrantLock lock = getGunLock(gunId);
        lock.lock();
        try {
            ChargingGun gun = gunMapper.selectById(gunId);
            if (gun == null) {
                throw new BusinessException("充电枪不存在");
            }

            if (gun.getStatus() == 2) {
                throw new BusinessException("充电进行中，请先结束充电");
            }

            if (gun.getStatus() != 1) {
                throw new BusinessException("充电枪状态异常，无法拔枪。当前状态: " + getGunStatusText(gun.getStatus()));
            }

            // 恢复充电枪状态为空闲
            gun.setStatus(0); // 空闲
            gun.setVehicleId(null);
            gun.setStartTime(null);
            gun.setEndTime(null);
            gun.setTotalPower(null);
            gunMapper.updateById(gun);

            // 清除缓存
            String cacheKey = "gun:status:" + gunId;
            redisTemplate.delete(cacheKey);

            // 清除充电桩详情缓存
            stationService.clearPileDetailCache(gun.getPileId());

            log.info("拔枪成功: gunId={}", gunId);
        } finally {
            lock.unlock();
        }
    }

    /**
     * 获取充电枪会话状态
     */
    public ChargingGunSessionVO getSessionStatus(Long gunId) {
        String cacheKey = "gun:status:" + gunId;

        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null && cached instanceof ChargingGun) {
                return convertToVO((ChargingGun) cached);
            }
        } catch (Exception e) {
            log.error("读取缓存失败，清除缓存: gunId={}", gunId, e);
            redisTemplate.delete(cacheKey);
        }

        // 从数据库查询
        ChargingGun gun = gunMapper.selectById(gunId);
        if (gun == null) {
            throw new BusinessException("充电枪不存在");
        }

        return convertToVO(gun);
    }

    /**
     * 根据充电枪ID获取活跃会话
     */
    public ChargingGunSessionVO getActiveSessionByGunId(Long gunId) {
        log.info("查询充电枪活跃会话: gunId={}", gunId);

        ChargingGun gun = gunMapper.selectById(gunId);
        if (gun == null) {
            throw new BusinessException("充电枪不存在");
        }

        // 如果状态为已插枪或充电中，则返回会话信息
        if (gun.getStatus() == 1 || gun.getStatus() == 2) {
            return convertToVO(gun);
        }

        log.debug("充电枪 {} 没有活跃会话，当前状态: {}", gunId, getGunStatusText(gun.getStatus()));
        return null;
    }

    /**
     * 重置充电枪状态
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetGun(Long gunId) {
        log.info("重置充电枪状态: gunId={}", gunId);

        ReentrantLock lock = getGunLock(gunId);
        lock.lock();
        try {
            // 校验充电枪
            ChargingGun gun = gunMapper.selectById(gunId);
            if (gun == null) {
                throw new BusinessException("充电枪不存在");
            }

            // 重置充电枪状态为空闲
            gun.setStatus(0);
            gun.setVehicleId(null);
            gun.setStartTime(null);
            gun.setEndTime(null);
            gun.setTotalPower(null);
            gunMapper.updateById(gun);

            // 清除相关缓存
            String cacheKey = "gun:status:" + gunId;
            redisTemplate.delete(cacheKey);

            // 清除充电桩详情缓存
            stationService.clearPileDetailCache(gun.getPileId());

            log.info("充电枪状态重置成功: gunId={}", gunId);
        } finally {
            lock.unlock();
        }
    }

    private ChargingGunSessionVO convertToVO(ChargingGun gun) {
        ChargingGunSessionVO vo = new ChargingGunSessionVO();
        vo.setGunId(gun.getId());
        vo.setGunCode(gun.getGunCode());
        vo.setType(gun.getType());
        vo.setTypeText(getGunTypeText(gun.getType()));
        vo.setStatus(gun.getStatus());
        vo.setStatusText(getGunStatusText(gun.getStatus()));
        vo.setVehicleId(gun.getVehicleId());
        vo.setStartTime(gun.getStartTime());
        vo.setEndTime(gun.getEndTime());
        vo.setTotalPower(gun.getTotalPower());

        // 计算充电时长
        if (gun.getStartTime() != null && gun.getEndTime() != null) {
            vo.setChargingDuration(Duration.between(gun.getStartTime(), gun.getEndTime()).toMinutes());
        }

        return vo;
    }

    private String getGunTypeText(Integer type) {
        return type == 1 ? "快充" : "慢充";
    }

    private String getGunStatusText(Integer status) {
        switch (status) {
            case 0:
                return "空闲";
            case 1:
                return "已插枪";
            case 2:
                return "充电中";
            case 3:
                return "故障";
            default:
                return "未知";
        }
    }
}
