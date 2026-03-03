package chat.wisechat.charging.mapper;

import chat.wisechat.charging.entity.ChargingSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充电会话 Mapper 接口
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Mapper
public interface ChargingSessionMapper extends BaseMapper<ChargingSession> {
}
