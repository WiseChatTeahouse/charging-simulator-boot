package chat.wisechat.charging.mapper;

import chat.wisechat.charging.entity.EmulatorMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 模拟器消息 Mapper 接口
 *
 * @author siberia.hu
 * @date 2026/3/19
 */
@Mapper
public interface EmulatorMessageMapper extends BaseMapper<EmulatorMessage> {

    /**
     * 按 groupId 查询消息列表，按 stepOrder 升序排列
     */
    @Select("SELECT * FROM t_emulator_messages WHERE group_id = #{groupId} ORDER BY step_order ASC")
    List<EmulatorMessage> selectByGroupIdOrdered(Integer groupId);

    /**
     * 查询所有不重复的 groupId
     */
    @Select("SELECT DISTINCT group_id FROM t_emulator_messages ORDER BY group_id ASC")
    List<Integer> selectDistinctGroupIds();
}
