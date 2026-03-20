package chat.wisechat.charging.config;

import chat.wisechat.charging.vo.ChargingDataVO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.TimeUnit;

/**
 * Caffeine 本地缓存配置
 *
 * @author siberia.hu
 * @date 2026/3/20
 */
@Configuration
public class CaffeineConfig {

    /**
     * 充电数据本地缓存，key=gunId，自动过期时间 1 小时
     */
    @Bean
    public Cache<Long, ChargingDataVO> chargingDataCache() {
        return Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.HOURS)
                .build();
    }

    /**
     * 充电推送任务调度器
     */
    @Bean
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);
        scheduler.setThreadNamePrefix("charging-push-");
        scheduler.initialize();
        return scheduler;
    }
}
