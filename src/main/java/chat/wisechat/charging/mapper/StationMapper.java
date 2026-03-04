package chat.wisechat.charging.mapper;

import chat.wisechat.charging.entity.Station;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 充电站 Mapper 接口
 *
 * @author siberia.hu
 * @date 2026/3/3
 */
@Mapper
public interface StationMapper extends BaseMapper<Station> {
    
    /**
     * 查询站点详情（包含充电桩和充电枪）
     * 使用连表查询一次性获取所有数据
     */
    @Select("SELECT " +
            "s.id as station_id, s.name as station_name, s.address as station_address, s.status as station_status, " +
            "p.id as pile_id, p.pile_code, p.status as pile_status, " +
            "g.id as gun_id, g.gun_code, g.type as gun_type, g.status as gun_status " +
            "FROM t_station s " +
            "LEFT JOIN t_charging_pile p ON s.id = p.station_id " +
            "LEFT JOIN t_charging_gun g ON p.id = g.pile_id " +
            "WHERE s.id = #{stationId} " +
            "ORDER BY p.id, g.id")
    List<Map<String, Object>> selectStationDetailWithPilesAndGuns(Long stationId);
}
