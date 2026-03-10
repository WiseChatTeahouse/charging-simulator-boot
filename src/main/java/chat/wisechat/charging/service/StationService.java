package chat.wisechat.charging.service;

import chat.wisechat.charging.entity.ChargingGun;
import chat.wisechat.charging.entity.ChargingPile;
import chat.wisechat.charging.entity.Station;
import chat.wisechat.charging.exception.BusinessException;
import chat.wisechat.charging.mapper.ChargingGunMapper;
import chat.wisechat.charging.mapper.ChargingPileMapper;
import chat.wisechat.charging.mapper.StationMapper;
import chat.wisechat.charging.vo.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 站点服务类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@Service
public class StationService {
    
    @Autowired
    private StationMapper stationMapper;
    
    @Autowired
    private ChargingPileMapper pileMapper;
    
    @Autowired
    private ChargingGunMapper gunMapper;
    
    // Caffeine 本地缓存 - 增加缓存大小并添加锁保证原子性
    private final Cache<String, Object> cache = Caffeine.newBuilder()
            .maximumSize(5000)  // 增加缓存大小
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    
    // 读写锁保证缓存操作的原子性
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
    
    /**
     * 查询所有站点
     */
    public List<StationVO> listStations() {
        String cacheKey = "stations:all";
        
        // 使用读锁获取缓存
        cacheLock.readLock().lock();
        try {
            @SuppressWarnings("unchecked")
            List<StationVO> cached = (List<StationVO>) cache.getIfPresent(cacheKey);
            if (cached != null) {
                log.info("从缓存获取站点列表");
                return cached;
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        
        // 使用写锁更新缓存
        cacheLock.writeLock().lock();
        try {
            // 双重检查，防止并发时重复查询
            @SuppressWarnings("unchecked")
            List<StationVO> cached = (List<StationVO>) cache.getIfPresent(cacheKey);
            if (cached != null) {
                log.info("从缓存获取站点列表（双重检查）");
                return cached;
            }
            
            List<Station> stations = stationMapper.selectList(null);
            List<StationVO> result = stations.stream()
                    .map(this::convertToStationVO)
                    .collect(Collectors.toList());
            
            cache.put(cacheKey, result);
            log.info("查询并缓存站点列表，数量: {}", result.size());
            return result;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * 查询站点详情（包含充电桩和充电枪）
     * 使用连表查询优化，减少数据库查询次数
     */
    public StationDetailVO getStationDetail(Long stationId) {
        // 使用连表查询一次性获取所有数据
        List<Map<String, Object>> rows = stationMapper.selectStationDetailWithPilesAndGuns(stationId);
        
        if (rows.isEmpty()) {
            throw new BusinessException("站点不存在");
        }
        
        // 构建站点详情VO
        Map<String, Object> firstRow = rows.get(0);
        StationDetailVO vo = new StationDetailVO();
        vo.setId(((Number) firstRow.get("station_id")).longValue());
        vo.setName((String) firstRow.get("station_name"));
        vo.setAddress((String) firstRow.get("station_address"));
        Integer stationStatus = ((Number) firstRow.get("station_status")).intValue();
        vo.setStatus(stationStatus);
        vo.setStatusText(getStationStatusText(stationStatus));
        
        // 使用Map来组织充电桩和充电枪的层级关系
        Map<Long, ChargingPileDetailVO> pileMap = new LinkedHashMap<>();
        
        for (Map<String, Object> row : rows) {
            Object pileIdObj = row.get("pile_id");
            if (pileIdObj == null) {
                continue; // 站点没有充电桩
            }
            
            Long pileId = ((Number) pileIdObj).longValue();
            
            // 如果充电桩不在Map中，创建新的充电桩VO
            ChargingPileDetailVO pileVO = pileMap.get(pileId);
            if (pileVO == null) {
                pileVO = new ChargingPileDetailVO();
                pileVO.setId(pileId);
                pileVO.setPileCode((String) row.get("pile_code"));
                Integer pileStatus = ((Number) row.get("pile_status")).intValue();
                pileVO.setStatus(pileStatus);
                pileVO.setStatusText(getPileStatusText(pileStatus));
                pileVO.setStationId(vo.getId());
                pileVO.setGuns(new ArrayList<>());
                pileMap.put(pileId, pileVO);
            }
            
            // 添加充电枪
            Object gunIdObj = row.get("gun_id");
            if (gunIdObj != null) {
                Long gunId = ((Number) gunIdObj).longValue();
                ChargingGunVO gunVO = new ChargingGunVO();
                gunVO.setId(gunId);
                gunVO.setGunCode((String) row.get("gun_code"));
                Integer gunType = ((Number) row.get("gun_type")).intValue();
                gunVO.setType(gunType);
                gunVO.setTypeText(getGunTypeText(gunType));
                Integer gunStatus = ((Number) row.get("gun_status")).intValue();
                gunVO.setStatus(gunStatus);
                gunVO.setStatusText(getGunStatusText(gunStatus));
                gunVO.setPileId(pileId);
                
                pileVO.getGuns().add(gunVO);
            }
        }
        
        vo.setPiles(new ArrayList<>(pileMap.values()));
        
        return vo;
    }
    
    /**
     * 查询充电桩详情（包含充电枪）
     * 不使用缓存，直接查询数据库以获取实时状态
     */
    public ChargingPileDetailVO getPileDetail(Long pileId) {
        ChargingPile pile = pileMapper.selectById(pileId);
        if (pile == null) {
            throw new BusinessException("充电桩不存在");
        }
        
        ChargingPileDetailVO vo = new ChargingPileDetailVO();
        BeanUtils.copyProperties(pile, vo);
        vo.setStatusText(getPileStatusText(pile.getStatus()));
        
        // 查询充电枪列表
        List<ChargingGun> guns = gunMapper.selectList(
                new LambdaQueryWrapper<ChargingGun>()
                        .eq(ChargingGun::getPileId, pileId)
        );
        
        vo.setGuns(guns.stream()
                .map(this::convertToChargingGunVO)
                .collect(Collectors.toList()));
        
        return vo;
    }
    
    /**
     * 根据充电枪ID查询充电桩详情
     */
    public ChargingPileDetailVO getPileDetailByGunId(Long gunId) {
        ChargingGun gun = gunMapper.selectById(gunId);
        if (gun == null) {
            throw new BusinessException("充电枪不存在");
        }
        return getPileDetail(gun.getPileId());
    }
    /**
     * 清除充电桩详情缓存
     */
    public void clearPileDetailCache(Long pileId) {
        String cacheKey = "pile:detail:" + pileId;
        cacheLock.writeLock().lock();
        try {
            cache.invalidate(cacheKey);
            log.info("清除充电桩详情缓存: {}", pileId);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        cacheLock.writeLock().lock();
        try {
            cache.invalidateAll();
            log.info("清除所有缓存");
        } finally {
            cacheLock.writeLock().unlock();
        }
    }
    
    private StationVO convertToStationVO(Station station) {
        StationVO vo = new StationVO();
        BeanUtils.copyProperties(station, vo);
        vo.setStatusText(getStationStatusText(station.getStatus()));
        return vo;
    }
    
    private ChargingPileVO convertToChargingPileVO(ChargingPile pile) {
        ChargingPileVO vo = new ChargingPileVO();
        BeanUtils.copyProperties(pile, vo);
        vo.setStatusText(getPileStatusText(pile.getStatus()));
        return vo;
    }
    
    private ChargingGunVO convertToChargingGunVO(ChargingGun gun) {
        ChargingGunVO vo = new ChargingGunVO();
        BeanUtils.copyProperties(gun, vo);
        vo.setTypeText(getGunTypeText(gun.getType()));
        vo.setStatusText(getGunStatusText(gun.getStatus()));
        return vo;
    }
    
    private String getStationStatusText(Integer status) {
        return status == 1 ? "启用" : "停用";
    }
    
    private String getPileStatusText(Integer status) {
        return status == 1 ? "正常" : "故障";
    }
    
    private String getGunTypeText(Integer type) {
        return type == 1 ? "快充" : "慢充";
    }
    
    private String getGunStatusText(Integer status) {
        switch (status) {
            case 0: return "空闲";
            case 1: return "已插枪";
            case 2: return "充电中";
            case 3: return "故障";
            default: return "未知";
        }
    }
}
