# 充电站模拟器系统

基于 Spring Boot 3.5.11 的充电站管理和充电过程模拟系统，支持实时数据展示。

## 功能特性

- 充电站、充电桩、充电枪的层级管理
- 完整的充电流程模拟（插枪、启动充电、结束充电、拔枪）
- 通过 MQTT 接收实时充电数据和车辆 BMS 数据
- 通过 WebSocket 实时推送数据到前端
- 使用 Redis 缓存充电会话状态
- 使用 Caffeine 本地缓存站点数据

## 技术栈

- **后端**: Spring Boot 3.5.11 + Undertow
- **数据库**: MySQL 8.0+ + MyBatis-Plus
- **消息队列**: MQTT (Eclipse Paho)
- **实时通信**: WebSocket
- **缓存**: Redis + Caffeine
- **前端**: Thymeleaf + HTML/CSS/JavaScript

## 环境要求

- JDK 21
- MySQL 8.0+
- Redis 6.0+
- MQTT Broker (如 Mosquitto)

## 快速开始

### 1. 数据库初始化

执行 SQL 脚本创建数据库和表：

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

或手动创建数据库：

```sql
CREATE DATABASE charging_simulator CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

然后执行 `src/main/resources/db/schema.sql` 中的 SQL 语句。

### 2. 配置文件

修改 `src/main/resources/application.yml` 中的配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/charging_simulator
    username: root
    password: your_password
  
  redis:
    host: localhost
    port: 6379

mqtt:
  broker-url: tcp://localhost:1883
  username: admin
  password: admin
```

### 3. 启动 Redis

```bash
redis-server
```

### 4. 启动 MQTT Broker

使用 Mosquitto：

```bash
mosquitto -v
```

或使用 Docker：

```bash
docker run -it -p 1883:1883 eclipse-mosquitto
```

### 5. 运行应用

```bash
mvn spring-boot:run
```

或使用 IDE 直接运行 `ChargingSimulatorApplication` 类。

### 6. 访问系统

打开浏览器访问：http://localhost:8080

## 系统架构

```
浏览器
  ↓ HTTP/WebSocket
Spring Boot 应用
  ├── Controller 层（REST API + 页面路由）
  ├── Service 层（业务逻辑）
  ├── Mapper 层（数据访问）
  └── WebSocket/MQTT 处理
       ↓
  MySQL + Redis + MQTT Broker
```

## API 接口

### 站点管理

- `GET /api/stations` - 获取站点列表
- `GET /api/stations/{stationId}` - 获取站点详情
- `GET /api/piles/{pileId}` - 获取充电桩详情

### 充电控制

- `POST /api/charging/insert-gun` - 插枪
- `POST /api/charging/start` - 启动充电
- `POST /api/charging/stop` - 结束充电
- `POST /api/charging/remove-gun` - 拔枪
- `GET /api/charging/session/{sessionId}` - 获取充电会话状态

### WebSocket

- `ws://localhost:8080/ws/charging/{sessionId}` - 实时数据推送

## MQTT 主题

### 充电数据

```
charging/station/{stationId}/pile/{pileId}/gun/{gunId}/data
```

消息格式：

```json
{
  "voltage": 380.5,
  "current": 125.3,
  "power": 47.6,
  "soc": 65,
  "timestamp": "2026-03-03T10:30:00"
}
```

### BMS 数据

```
vehicle/{vehicleId}/bms/data
```

消息格式：

```json
{
  "batteryVoltage": 400.2,
  "batteryCurrent": 120.5,
  "batteryTemp": 35.8,
  "soc": 68,
  "timestamp": "2026-03-03T10:30:00"
}
```

## 测试 MQTT 消息

使用 mosquitto_pub 发送测试消息：

```bash
# 发送充电数据
mosquitto_pub -h localhost -t "charging/station/1/pile/1/gun/1/data" -m '{"voltage":380.5,"current":125.3,"power":47.6,"soc":65,"timestamp":"2026-03-03T10:30:00"}'

# 发送 BMS 数据
mosquitto_pub -h localhost -t "vehicle/TEST_VEHICLE_001/bms/data" -m '{"batteryVoltage":400.2,"batteryCurrent":120.5,"batteryTemp":35.8,"soc":68,"timestamp":"2026-03-03T10:30:00"}'
```

## 使用流程

1. 访问首页，查看充电站列表
2. 点击充电站，查看充电桩和充电枪
3. 点击充电枪，进入充电模拟器页面
4. 按顺序操作：插枪 → 启动充电 → 结束充电 → 拔枪
5. 充电过程中，通过 MQTT 发送实时数据，前端会自动更新显示

## 项目结构

```
src/main/java/chat/wisechat/charging/
├── config/              # 配置类
├── controller/          # 控制器
├── service/             # 服务层
├── mapper/              # 数据访问层
├── entity/              # 实体类
├── vo/                  # 视图对象
├── dto/                 # 数据传输对象
├── exception/           # 异常处理
├── common/              # 公共类
└── websocket/           # WebSocket 处理器

src/main/resources/
├── templates/           # Thymeleaf 模板
├── static/              # 静态资源
├── db/                  # 数据库脚本
└── application.yml      # 配置文件
```

## 注意事项

1. 确保 MySQL、Redis 和 MQTT Broker 都已启动
2. 数据库中需要有测试数据（schema.sql 已包含）
3. MQTT 消息的主题格式必须正确
4. WebSocket 连接需要在启动充电后才会建立
5. 充电流程必须按顺序执行：插枪 → 启动 → 结束 → 拔枪

## 许可证

MIT License
