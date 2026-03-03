package chat.wisechat.charging.mapper;

import chat.wisechat.charging.entity.Station;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 充电站 Mapper 接口
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Mapper
public interface StationMapper extends BaseMapper<Station> {
}
