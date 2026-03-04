package chat.wisechat.charging.controller;

import chat.wisechat.charging.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

/**
 * Redis 管理控制器
 *
 * @author siberia.hu
 * @date 2026/3/4
 */
@Slf4j
@RestController
@RequestMapping("/api/redis")
public class RedisController {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * 清除所有缓存
     */
    @PostMapping("/clear-all")
    public Result<String> clearAll() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("清除所有缓存成功，共删除 {} 个键", deleted);
                return Result.success("清除成功，共删除 " + deleted + " 个键");
            }
            return Result.success("没有缓存需要清除");
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            return Result.error("清除失败: " + e.getMessage());
        }
    }
    
    /**
     * 清除指定前缀的缓存
     */
    @PostMapping("/clear-prefix")
    public Result<String> clearByPrefix(@RequestParam String prefix) {
        try {
            Set<String> keys = redisTemplate.keys(prefix + "*");
            if (keys != null && !keys.isEmpty()) {
                Long deleted = redisTemplate.delete(keys);
                log.info("清除前缀为 {} 的缓存成功，共删除 {} 个键", prefix, deleted);
                return Result.success("清除成功，共删除 " + deleted + " 个键");
            }
            return Result.success("没有匹配的缓存需要清除");
        } catch (Exception e) {
            log.error("清除缓存失败", e);
            return Result.error("清除失败: " + e.getMessage());
        }
    }
    
    /**
     * 查看所有缓存键
     */
    @GetMapping("/keys")
    public Result<Set<String>> getAllKeys() {
        try {
            Set<String> keys = redisTemplate.keys("*");
            return Result.success(keys);
        } catch (Exception e) {
            log.error("获取缓存键失败", e);
            return Result.error("获取失败: " + e.getMessage());
        }
    }
}
