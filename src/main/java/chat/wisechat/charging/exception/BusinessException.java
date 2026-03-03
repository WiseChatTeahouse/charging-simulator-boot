package chat.wisechat.charging.exception;

import lombok.Getter;

/**
 * 业务异常类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Getter
public class BusinessException extends RuntimeException {
    
    private final Integer code;
    
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }
    
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}
