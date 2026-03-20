package chat.wisechat.charging.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author siberia.hu
 * @date 2026/3/20
 */
@Slf4j
@Configuration
public class ThreadPoolConfig {

    @Bean(name = "chargingTaskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);              // 线程池大小
        scheduler.setThreadNamePrefix("charging-task-"); // 线程名前缀
        scheduler.setErrorHandler(t -> {        // 异常处理
            log.warn("任务执行异常：" + t.getMessage());
        });
        scheduler.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());// 设置一下拒绝策略
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }
}