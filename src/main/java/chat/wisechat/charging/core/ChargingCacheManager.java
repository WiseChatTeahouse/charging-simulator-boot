package chat.wisechat.charging.core;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 充电本地缓存管理器
 * 独立管理充电状态缓存，避免 ChargingService <-> MqttMessageHandler 循环依赖
 */
@Component
public class ChargingCacheManager {

    private Cache<String, Object> cache;

    @PostConstruct
    public void init() {
        cache = Caffeine.newBuilder()
                .maximumSize(200)
                .expireAfterWrite(2, TimeUnit.HOURS)
                .build();
    }

    /** 存储 taskId，key: task:{gunId} */
    public void putTaskId(Long gunId, String taskId) {
        cache.put("task:" + gunId, taskId);
    }

    /** 获取 taskId */
    public String getTaskId(Long gunId) {
        Object val = cache.getIfPresent("task:" + gunId);
        return val != null ? val.toString() : null;
    }

    /** 移除 taskId */
    public void removeTaskId(Long gunId) {
        cache.invalidate("task:" + gunId);
    }

    /** 判断充电枪是否处于充电中 */
    public boolean isCharging(Long gunId) {
        return cache.getIfPresent("task:" + gunId) != null;
    }

    /** 存储 vehicleId -> gunId 映射 */
    public void putVehicleGun(String vehicleId, Long gunId) {
        cache.put("vehicle:" + vehicleId, gunId);
    }

    /** 通过 vehicleId 获取 gunId */
    public Long getGunIdByVehicleId(String vehicleId) {
        Object val = cache.getIfPresent("vehicle:" + vehicleId);
        return val != null ? (Long) val : null;
    }

    /** 移除 vehicleId 映射 */
    public void removeVehicleGun(String vehicleId) {
        cache.invalidate("vehicle:" + vehicleId);
    }
}
