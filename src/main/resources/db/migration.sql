-- 数据库迁移脚本：废除t_charging_session表，扩展t_charging_gun表

-- 1. 为t_charging_gun表添加新字段
ALTER TABLE t_charging_gun 
ADD COLUMN vehicle_id VARCHAR(50) COMMENT '车辆ID（插枪时设置）' AFTER status,
ADD COLUMN start_time DATETIME COMMENT '充电开始时间' AFTER vehicle_id,
ADD COLUMN end_time DATETIME COMMENT '充电结束时间' AFTER start_time,
ADD COLUMN total_power DECIMAL(10,2) COMMENT '总电量(kWh)' AFTER end_time;

-- 2. 更新t_charging_gun表的status字段注释
ALTER TABLE t_charging_gun 
MODIFY COLUMN status TINYINT DEFAULT 0 COMMENT '状态：0-空闲，1-已插枪，2-充电中，3-故障';

-- 3. 为新字段添加索引
ALTER TABLE t_charging_gun 
ADD INDEX idx_vehicle_id (vehicle_id),
ADD INDEX idx_status (status);

-- 4. 数据迁移：将t_charging_session表的数据迁移到t_charging_gun表
-- 注意：这个脚本假设每个充电枪只有一个活跃会话
UPDATE t_charging_gun g 
INNER JOIN (
    SELECT gun_id, vehicle_id, start_time, end_time, total_power,
           CASE 
               WHEN status = 0 THEN 1  -- 已插枪 -> 已插枪
               WHEN status = 1 THEN 2  -- 充电中 -> 充电中
               WHEN status = 2 THEN 1  -- 已结束 -> 已插枪（等待拔枪）
               ELSE 0                  -- 其他状态 -> 空闲
           END as new_status
    FROM t_charging_session 
    WHERE id IN (
        SELECT MAX(id) FROM t_charging_session GROUP BY gun_id
    )
) s ON g.id = s.gun_id
SET g.vehicle_id = s.vehicle_id,
    g.start_time = s.start_time,
    g.end_time = s.end_time,
    g.total_power = s.total_power,
    g.status = s.new_status;

-- 5. 备份t_charging_session表（可选）
-- CREATE TABLE t_charging_session_backup AS SELECT * FROM t_charging_session;

-- 6. 删除t_charging_session表的外键约束
-- ALTER TABLE t_charging_session DROP FOREIGN KEY t_charging_session_ibfk_1;

-- 7. 重命名t_charging_session表为备份表（而不是直接删除）
-- RENAME TABLE t_charging_session TO t_charging_session_deprecated;

-- 注意：实际执行时请根据具体情况调整，建议先在测试环境验证

-- 为t_charging_gun表添加乐观锁version字段
ALTER TABLE t_charging_gun
ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号' AFTER total_power;
