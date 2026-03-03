package chat.wisechat.charging.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Configuration
@MapperScan("chat.wisechat.charging.mapper")
public class MybatisPlusConfig {
}
