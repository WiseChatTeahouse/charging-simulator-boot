package chat.wisechat.charging.service;

import chat.wisechat.charging.core.TaskScheduledManager;
import chat.wisechat.charging.entity.ChargingGun;
import chat.wisechat.charging.entity.EmulatorMessage;
import chat.wisechat.charging.exception.BusinessException;
import chat.wisechat.charging.mapper.ChargingGunMapper;
import chat.wisechat.charging.vo.ChargingGunSessionVO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
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

    @Resource
    private EmulatorMessageService emulatorMessageService;

    @Resource
    private TaskScheduledManager taskScheduledManager;

    @Resource
    private ThreadPoolTaskScheduler chargingTaskScheduler;

    private Cache<String, Object> chargingLocalCache;

    // 本地锁保证操作的原子性 - 每个充电枪一个锁
    private static final ConcurrentHashMap<Long, ReentrantLock> gunLocks = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        chargingLocalCache = Caffeine.newBuilder()
                .maximumSize(50)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .build();
    }

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
     * 启动充电 - 乐观锁更新枪状态为充电中(2)，写入 Caffeine 本地缓存并启动调度任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void startCharging(Long gunId) {
        log.info("启动充电: gunId={}", gunId);

        ChargingGun gun = gunMapper.selectById(gunId);
        if (gun == null) {
            throw new BusinessException("充电枪不存在");
        }
        if (gun.getStatus() != 1) {
            throw new BusinessException("充电枪状态异常，无法启动充电。当前状态: " + getGunStatusText(gun.getStatus()));
        }

        LambdaUpdateWrapper<ChargingGun> wrapper = new LambdaUpdateWrapper<ChargingGun>()
                .eq(ChargingGun::getId, gunId)
                .eq(ChargingGun::getStatus, 1)
                .eq(ChargingGun::getVersion, gun.getVersion())
                .set(ChargingGun::getStatus, 2)
                .set(ChargingGun::getStartTime, LocalDateTime.now())
                .set(ChargingGun::getVersion, gun.getVersion() + 1);

        int rows = gunMapper.update(null, wrapper);
        if (rows == 0) {
            throw new BusinessException("启动充电失败，充电枪状态已变更，请重试");
        }

        // 随机选取一组报文并写入本地缓存
        List<Long> groupIds = emulatorMessageService.getAllGroupIds();
        if (groupIds.isEmpty()) {
            log.warn("无可用报文分组，gunId={} 将跳过报文调度", gunId);
            return;
        }

        Long randomGroupId = groupIds.get(ThreadLocalRandom.current().nextInt(groupIds.size()));
        List<EmulatorMessage> messages = emulatorMessageService.getByGroupId(randomGroupId);
        if (messages == null || messages.isEmpty()) {
            log.warn("分组 {} 下无报文数据，gunId={} 将跳过报文调度", randomGroupId, gunId);
            return;
        }
        chargingLocalCache.put(String.valueOf(randomGroupId), messages);

        // 启动调度任务，每5秒消费一条报文
        ScheduledFuture<?> future = chargingTaskScheduler.scheduleAtFixedRate(() -> {
            try {
                @SuppressWarnings("unchecked")
                List<EmulatorMessage> cached = (List<EmulatorMessage>)
                        chargingLocalCache.getIfPresent(String.valueOf(randomGroupId));
                if (cached == null || cached.isEmpty()) {
                    log.info("gunId={} 报文已全部消费，任务继续等待", gunId);
                    return;
                }
                EmulatorMessage message = cached.remove(cached.size() - 1);
                log.info("gunId={} 消费报文: {}", gunId, message);
            } catch (Exception e) {
                log.error("gunId={} 报文调度异常", gunId, e);
            }
        }, Duration.ofSeconds(2));

        String taskId = taskScheduledManager.registerTask(future, String.valueOf(gunId));
        chargingLocalCache.put(String.valueOf(gunId), taskId);
        log.info("充电启动成功: gunId={} taskId={}", gunId, taskId);
    }

    /**
     * 结束充电 - 乐观锁更新枪状态为已插枪(1)
     */
    @Transactional(rollbackFor = Exception.class)
    public void stopCharging(Long gunId) {
        log.info("结束充电: gunId={}", gunId);

        ChargingGun gun = gunMapper.selectById(gunId);
        if (gun == null) {
            throw new BusinessException("充电枪不存在");
        }
        if (gun.getStatus() != 2) {
            throw new BusinessException("充电枪状态异常，无法结束充电。当前状态: " + getGunStatusText(gun.getStatus()));
        }

        LambdaUpdateWrapper<ChargingGun> wrapper = new LambdaUpdateWrapper<ChargingGun>()
                .eq(ChargingGun::getId, gunId)
                .eq(ChargingGun::getStatus, 2)
                .eq(ChargingGun::getVersion, gun.getVersion())
                .set(ChargingGun::getStatus, 1)
                .set(ChargingGun::getEndTime, LocalDateTime.now())
                .set(ChargingGun::getVersion, gun.getVersion() + 1);

        int rows = gunMapper.update(null, wrapper);
        if (rows == 0) {
            throw new BusinessException("结束充电失败，充电枪状态已变更，请重试");
        }

        Object taskId = chargingLocalCache.getIfPresent(String.valueOf(gunId));
        taskScheduledManager.stopTask(taskId.toString());
        log.info("充电结束成功: gunId={}", gunId);
    }

    /**
     * 拔枪 - 乐观锁更新枪状态为空闲(0)
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeGun(Long gunId) {
        log.info("拔枪操作: gunId={}", gunId);

        ChargingGun gun = gunMapper.selectById(gunId);
        if (gun == null) {
            throw new BusinessException("充电枪不存在");
        }
        if (gun.getStatus() != 1) {
            throw new BusinessException("充电枪状态异常，无法拔枪。当前状态: " + getGunStatusText(gun.getStatus()));
        }

        LambdaUpdateWrapper<ChargingGun> wrapper = new LambdaUpdateWrapper<ChargingGun>()
                .eq(ChargingGun::getId, gunId)
                .eq(ChargingGun::getStatus, 1)
                .eq(ChargingGun::getVersion, gun.getVersion())
                .set(ChargingGun::getStatus, 0)
                .set(ChargingGun::getVersion, gun.getVersion() + 1);

        int rows = gunMapper.update(null, wrapper);
        if (rows == 0) {
            throw new BusinessException("拔枪失败，充电枪状态已变更，请重试");
        }

        Object taskId = chargingLocalCache.getIfPresent(String.valueOf(gunId));
        taskScheduledManager.stopTask(taskId.toString());
        log.info("拔枪成功: gunId={}", gunId);
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
