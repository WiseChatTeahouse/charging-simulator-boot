package chat.wisechat.charging.core;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * 任务执行管理器
 */
@Component
public class TaskScheduledManager {

    // 存储 taskId 与 ScheduledFuture 的映射
    private final ConcurrentHashMap<String, ScheduledTaskInfo> taskMap = new ConcurrentHashMap<>();

    /**
     * 注册新任务
     */
    public String registerTask(ScheduledFuture<?> future, String businessId) {
        String taskId = UUID.randomUUID().toString();
        taskMap.put(taskId, new ScheduledTaskInfo(future, businessId, System.currentTimeMillis()));
        return taskId;
    }

    /**
     * 根据 taskId 停止任务
     */
    public boolean stopTask(String taskId) {
        ScheduledTaskInfo taskInfo = taskMap.remove(taskId);
        if (taskInfo != null) {
            taskInfo.future().cancel(false);
            return true;
        }
        return false;
    }

    /**
     * 根据 businessId 停止任务（业务场景常用）
     */
    public int stopTasksByBusinessId(String businessId) {
        int count = 0;
        for (Map.Entry<String, ScheduledTaskInfo> entry : taskMap.entrySet()) {
            if (entry.getValue().businessId().equals(businessId)) {
                entry.getValue().future().cancel(false);
                taskMap.remove(entry.getKey());
                count++;
            }
        }
        return count;
    }

    /**
     * 任务信息封装类
     */
    public record ScheduledTaskInfo(ScheduledFuture<?> future, String businessId, long createTime) {

    }
}

