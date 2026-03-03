package chat.wisechat.charging.exception;

import chat.wisechat.charging.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理器
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        log.error("业务异常: {}", e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }
    
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<Void> handleNoResourceFoundException(NoResourceFoundException e) {
        // 忽略浏览器开发者工具等请求的静态资源不存在错误
        log.debug("静态资源不存在: {}", e.getMessage());
        return Result.error(404, "资源不存在");
    }
    
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("系统异常", e);
        return Result.error("系统异常，请稍后重试");
    }
}
