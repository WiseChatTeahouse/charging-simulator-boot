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

import java.util.List;
import java.util.concurrent.TimeUnit;
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
    
    // Caffeine 本地缓存
    private final Cache<String, Object> cache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();
    
    /**
     * 查询所有站点
     */
    public List<StationVO> listStations() {
        String cacheKey = "stations:all";
        
        @SuppressWarnings("unchecked")
        List<StationVO> cached = (List<StationVO>) cache.getIfPresent(cacheKey);
        if (cached != null) {
            log.info("从缓存获取站点列表");
            return cached;
        }
        
        List<Station> stations = stationMapper.selectList(null);
        List<StationVO> result = stations.stream()
                .map(this::convertToStationVO)
                .collect(Collectors.toList());
        
        cache.put(cacheKey, result);
        return result;
    }
    
    /**
     * 查询站点详情（包含充电桩和充电枪）
     */
    public StationDetailVO getStationDetail(Long stationId) {
        String cacheKey = "station:detail:" + stationId;
        
        StationDetailVO cached = (StationDetailVO) cache.getIfPresent(cacheKey);
        if (cached != null) {
            log.info("从缓存获取站点详情: {}", stationId);
            return cached;
        }
        
        Station station = stationMapper.selectById(stationId);
        if (station == null) {
            throw new BusinessException("站点不存在");
        }
        
        StationDetailVO vo = new StationDetailVO();
        BeanUtils.copyProperties(station, vo);
        vo.setStatusText(getStationStatusText(station.getStatus()));
        
        // 查询充电桩列表
        List<ChargingPile> piles = pileMapper.selectList(
                new LambdaQueryWrapper<ChargingPile>()
                        .eq(ChargingPile::getStationId, stationId)
        );
        
        // 为每个充电桩查询充电枪并转换为ChargingPileDetailVO
        List<ChargingPileDetailVO> pileDetails = piles.stream()
                .map(pile -> {
                    ChargingPileDetailVO pileVO = new ChargingPileDetailVO();
                    BeanUtils.copyProperties(pile, pileVO);
                    pileVO.setStatusText(getPileStatusText(pile.getStatus()));
                    
                    // 查询该充电桩的充电枪
                    List<ChargingGun> guns = gunMapper.selectList(
                            new LambdaQueryWrapper<ChargingGun>()
                                    .eq(ChargingGun::getPileId, pile.getId())
                    );
                    
                    pileVO.setGuns(guns.stream()
                            .map(this::convertToChargingGunVO)
                            .collect(Collectors.toList()));
                    
                    return pileVO;
                })
                .collect(Collectors.toList());
        
        vo.setPiles(pileDetails);
        
        cache.put(cacheKey, vo);
        return vo;
    }
    
    /**
     * 查询充电桩详情（包含充电枪）
     */
    public ChargingPileDetailVO getPileDetail(Long pileId) {
        String cacheKey = "pile:detail:" + pileId;
        
        ChargingPileDetailVO cached = (ChargingPileDetailVO) cache.getIfPresent(cacheKey);
        if (cached != null) {
            log.info("从缓存获取充电桩详情: {}", pileId);
            return cached;
        }
        
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
        
        cache.put(cacheKey, vo);
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
        cache.invalidate(cacheKey);
        log.info("清除充电桩详情缓存: {}", pileId);
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
            case 1: return "充电中";
            case 2: return "故障";
            default: return "未知";
        }
    }
}
