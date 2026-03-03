-- 充电站模拟器数据库表结构

-- 充电站表
CREATE TABLE IF NOT EXISTS t_station (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '站点ID',
    name VARCHAR(100) NOT NULL COMMENT '站点名称',
    address VARCHAR(200) COMMENT '站点地址',
    status TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充电站表';

-- 充电桩表
CREATE TABLE IF NOT EXISTS t_charging_pile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '充电桩ID',
    station_id BIGINT NOT NULL COMMENT '所属站点ID',
    pile_code VARCHAR(50) NOT NULL COMMENT '充电桩编号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-故障，1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_station_id (station_id),
    FOREIGN KEY (station_id) REFERENCES t_station(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充电桩表';

-- 充电枪表
CREATE TABLE IF NOT EXISTS t_charging_gun (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '充电枪ID',
    pile_id BIGINT NOT NULL COMMENT '所属充电桩ID',
    gun_code VARCHAR(50) NOT NULL COMMENT '充电枪编号',
    type TINYINT DEFAULT 1 COMMENT '类型：1-快充，2-慢充',
    status TINYINT DEFAULT 0 COMMENT '状态：0-空闲，1-充电中，2-故障',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_pile_id (pile_id),
    FOREIGN KEY (pile_id) REFERENCES t_charging_pile(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充电枪表';

-- 充电会话表
CREATE TABLE IF NOT EXISTS t_charging_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '会话ID',
    gun_id BIGINT NOT NULL COMMENT '充电枪ID',
    vehicle_id VARCHAR(50) NOT NULL COMMENT '车辆ID',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    status TINYINT DEFAULT 0 COMMENT '状态：0-已插枪，1-充电中，2-已结束',
    total_power DECIMAL(10,2) COMMENT '总电量(kWh)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_gun_id (gun_id),
    INDEX idx_vehicle_id (vehicle_id),
    FOREIGN KEY (gun_id) REFERENCES t_charging_gun(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='充电会话表';

-- 插入测试数据

-- 插入站点数据
INSERT INTO t_station (id, name, address, status) VALUES
(1, '北京朝阳充电站', '北京市朝阳区建国路88号', 1),
(2, '上海浦东充电站', '上海市浦东新区世纪大道100号', 1);

-- 插入充电桩数据
INSERT INTO t_charging_pile (id, station_id, pile_code, status) VALUES
(1, 1, 'BJ-CY-001', 1),
(2, 1, 'BJ-CY-002', 1),
(3, 2, 'SH-PD-001', 1),
(4, 2, 'SH-PD-002', 1);

-- 插入充电枪数据
INSERT INTO t_charging_gun (id, pile_id, gun_code, type, status) VALUES
(1, 1, 'BJ-CY-001-A', 1, 0),
(2, 1, 'BJ-CY-001-B', 1, 0),
(3, 2, 'BJ-CY-002-A', 1, 0),
(4, 2, 'BJ-CY-002-B', 2, 0),
(5, 3, 'SH-PD-001-A', 1, 0),
(6, 3, 'SH-PD-001-B', 1, 0),
(7, 4, 'SH-PD-002-A', 1, 0),
(8, 4, 'SH-PD-002-B', 2, 0);
