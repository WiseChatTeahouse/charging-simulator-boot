package chat.wisechat.charging.service;

import chat.wisechat.charging.entity.ChargingGun;
import chat.wisechat.charging.entity.ChargingSession;
import chat.wisechat.charging.exception.BusinessException;
import chat.wisechat.charging.mapper.ChargingGunMapper;
import chat.wisechat.charging.mapper.ChargingSessionMapper;
import chat.wisechat.charging.vo.ChargingResultVO;
import chat.wisechat.charging.vo.ChargingSessionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

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
    private ChargingSessionMapper sessionMapper;
    
    @Autowired
    private ChargingGunMapper gunMapper;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 插枪操作
     */
    @Transactional(rollbackFor = Exception.class)
    public Long insertGun(Long gunId, String vehicleId) {
        log.info("插枪操作: gunId={}, vehicleId={}", gunId, vehicleId);
        
        // 校验充电枪
        ChargingGun gun = gunMapper.selectById(gunId);
        if (gun == null) {
            throw new BusinessException("充电枪不存在");
        }
        
        if (gun.getStatus() != 0) {
            throw new BusinessException("充电枪状态异常，无法插枪");
        }
        
        // 检查是否已有活跃会话
        Long activeCount = sessionMapper.selectCount(
                new LambdaQueryWrapper<ChargingSession>()
                        .eq(ChargingSession::getGunId, gunId)
                        .in(ChargingSession::getStatus, 0, 1)
        );
        
        if (activeCount > 0) {
            throw new BusinessException("该充电枪已有活跃会话");
        }
        
        // 创建充电会话
        ChargingSession session = new ChargingSession();
        session.setGunId(gunId);
        session.setVehicleId(vehicleId);
        session.setStatus(0); // 已插枪
        sessionMapper.insert(session);
        
        // 更新充电枪状态
        gun.setStatus(1); // 充电中
        gunMapper.updateById(gun);
        
        // 缓存会话状态
        String cacheKey = "session:status:" + session.getId();
        redisTemplate.opsForValue().set(cacheKey, session, 30, TimeUnit.MINUTES);
        
        log.info("插枪成功，会话ID: {}", session.getId());
        return session.getId();
    }
    
    /**
     * 启动充电
     */
    @Transactional(rollbackFor = Exception.class)
    public void startCharging(Long sessionId) {
        log.info("启动充电: sessionId={}", sessionId);
        
        ChargingSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("充电会话不存在");
        }
        
        if (session.getStatus() != 0) {
            throw new BusinessException("会话状态异常，无法启动充电");
        }
        
        // 更新会话状态
        session.setStatus(1); // 充电中
        session.setStartTime(LocalDateTime.now());
        sessionMapper.updateById(session);
        
        // 更新缓存
        String cacheKey = "session:status:" + sessionId;
        redisTemplate.opsForValue().set(cacheKey, session, 30, TimeUnit.MINUTES);
        
        // 缓存 gunId -> sessionId 映射
        String gunSessionKey = "gun:session:" + session.getGunId();
        redisTemplate.opsForValue().set(gunSessionKey, sessionId, 30, TimeUnit.MINUTES);
        
        // 缓存 vehicleId -> sessionId 映射
        String vehicleSessionKey = "vehicle:session:" + session.getVehicleId();
        redisTemplate.opsForValue().set(vehicleSessionKey, sessionId, 30, TimeUnit.MINUTES);
        
        log.info("充电启动成功");
    }
    
    /**
     * 结束充电
     */
    @Transactional(rollbackFor = Exception.class)
    public ChargingResultVO stopCharging(Long sessionId) {
        log.info("结束充电: sessionId={}", sessionId);
        
        ChargingSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("充电会话不存在");
        }
        
        if (session.getStatus() != 1) {
            throw new BusinessException("会话状态异常，无法结束充电");
        }
        
        // 更新会话状态
        session.setStatus(2); // 已结束
        session.setEndTime(LocalDateTime.now());
        
        // 计算总电量（模拟值）
        if (session.getStartTime() != null) {
            Duration duration = Duration.between(session.getStartTime(), session.getEndTime());
            long minutes = duration.toMinutes();
            // 假设平均功率 50kW
            BigDecimal totalPower = BigDecimal.valueOf(minutes * 50.0 / 60.0);
            session.setTotalPower(totalPower);
        } else {
            session.setTotalPower(BigDecimal.ZERO);
        }
        
        sessionMapper.updateById(session);
        
        // 更新缓存
        String cacheKey = "session:status:" + sessionId;
        redisTemplate.opsForValue().set(cacheKey, session, 30, TimeUnit.MINUTES);
        
        // 构建返回结果
        ChargingResultVO result = new ChargingResultVO();
        result.setSessionId(sessionId);
        result.setTotalPower(session.getTotalPower());
        if (session.getStartTime() != null && session.getEndTime() != null) {
            result.setChargingDuration(Duration.between(session.getStartTime(), session.getEndTime()).toMinutes());
        }
        result.setMessage("充电结束");
        
        log.info("充电结束成功，总电量: {} kWh", session.getTotalPower());
        return result;
    }
    
    /**
     * 拔枪操作
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeGun(Long sessionId) {
        log.info("拔枪操作: sessionId={}", sessionId);
        
        ChargingSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("充电会话不存在");
        }
        
        if (session.getStatus() != 2) {
            throw new BusinessException("会话状态异常，请先结束充电");
        }
        
        // 恢复充电枪状态
        ChargingGun gun = gunMapper.selectById(session.getGunId());
        if (gun != null) {
            gun.setStatus(0); // 空闲
            gunMapper.updateById(gun);
        }
        
        // 清除缓存
        String cacheKey = "session:status:" + sessionId;
        redisTemplate.delete(cacheKey);
        
        String gunSessionKey = "gun:session:" + session.getGunId();
        redisTemplate.delete(gunSessionKey);
        
        String vehicleSessionKey = "vehicle:session:" + session.getVehicleId();
        redisTemplate.delete(vehicleSessionKey);
        
        log.info("拔枪成功");
    }
    
    /**
     * 获取充电会话状态
     */
    public ChargingSessionVO getSessionStatus(Long sessionId) {
        String cacheKey = "session:status:" + sessionId;
        
        ChargingSession cached = (ChargingSession) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return convertToVO(cached);
        }
        
        ChargingSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("充电会话不存在");
        }
        
        return convertToVO(session);
    }
    
    private ChargingSessionVO convertToVO(ChargingSession session) {
        ChargingSessionVO vo = new ChargingSessionVO();
        BeanUtils.copyProperties(session, vo);
        vo.setStatusText(getSessionStatusText(session.getStatus()));
        return vo;
    }
    
    private String getSessionStatusText(Integer status) {
        switch (status) {
            case 0: return "已插枪";
            case 1: return "充电中";
            case 2: return "已结束";
            default: return "未知";
        }
    }
}
