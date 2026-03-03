# 充电站模拟器系统 - 技术设计文档

## 1. 系统概述

充电站模拟器是一个基于 Spring Boot 的前后端不分离应用，用于模拟充电站的充电过程，实时展示充电数据和车辆 BMS 信息。

### 1.1 技术栈
- **后端框架**: Spring Boot 3.5.11 + Undertow
- **数据库**: MySQL + MyBatis-Plus
- **消息队列**: MQTT (Eclipse Paho)
- **前端**: Thymeleaf + HTML/CSS/JavaScript
- **实时通信**: WebSocket
- **缓存**: Redis + Caffeine

## 2. 高层设计 (High-Level Design)

### 2.1 系统架构

```
┌─────────────────────────────────────────────────────────────┐
│                        浏览器客户端                           │
│  ┌──────────────────────┐      ┌──────────────────────┐    │
│  │   充电站管理界面      │      │   充电模拟器界面      │    │
│  │  - 站点列表          │      │  - 充电控制          │    │
│  │  - 充电桩列表        │      │  - 实时数据展示      │    │
│  │  - 充电枪列表        │      │  - 车辆BMS数据       │    │
│  └──────────────────────┘      └──────────────────────┘    │
│           │                              │                   │
│           └──────────────┬───────────────┘                   │
│                          │ HTTP/WebSocket                    │
└──────────────────────────┼───────────────────────────────────┘
                           │
┌──────────────────────────┼───────────────────────────────────┐
│                   Spring Boot 应用                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    Controller 层                        │ │
│  │  - StationController (站点管理)                        │ │
│  │  - ChargingController (充电控制)                       │ │
│  │  - WebSocketController (实时数据推送)                  │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    Service 层                           │ │
│  │  - StationService (站点业务逻辑)                       │ │
│  │  - ChargingService (充电业务逻辑)                      │ │
│  │  - MqttService (MQTT消息处理)                          │ │
│  │  - WebSocketService (WebSocket消息推送)                │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │                    Mapper 层                            │ │
│  │  - StationMapper                                        │ │
│  │  - ChargingPileMapper                                   │ │
│  │  - ChargingGunMapper                                    │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────┬───────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
┌───────▼────────┐  ┌──────▼──────┐  ┌───────▼────────┐
│  MySQL 数据库   │  │ MQTT Broker │  │  Redis 缓存    │
│  - 站点数据     │  │ - 充电数据  │  │  - 会话数据    │
│  - 充电桩数据   │  │ - BMS数据   │  │  - 实时状态    │
│  - 充电枪数据   │  │             │  │                │
└────────────────┘  └─────────────┘  └────────────────┘
```

### 2.2 核心组件

#### 2.2.1 站点管理模块
- **职责**: 管理充电站、充电桩、充电枪的层级关系
- **功能**: 
  - 查询站点列表
  - 查询站点下的充电桩
  - 查询充电桩下的充电枪
  - 站点数据导入

#### 2.2.2 充电模拟模块
- **职责**: 模拟充电过程，控制充电状态
- **功能**:
  - 插枪/拔枪操作
  - 启动/结束充电
  - 充电状态管理
  - 实时数据展示

#### 2.2.3 MQTT 数据接收模块
- **职责**: 接收并处理 MQTT 消息
- **功能**:
  - 订阅充电数据主题
  - 订阅 BMS 数据主题
  - 解析消息并转发到 WebSocket

#### 2.2.4 WebSocket 推送模块
- **职责**: 实时推送数据到前端
- **功能**:
  - 推送充电实时数据（电压、电流、功率）
  - 推送车辆 BMS 数据
  - 维护客户端连接

### 2.3 数据模型

#### 2.3.1 实体关系图

```
Station (充电站)
  ├── id: Long
  ├── name: String
  ├── address: String
  ├── status: Integer
  └── 1:N ChargingPile

ChargingPile (充电桩)
  ├── id: Long
  ├── stationId: Long
  ├── pileCode: String
  ├── status: Integer
  └── 1:N ChargingGun

ChargingGun (充电枪)
  ├── id: Long
  ├── pileId: Long
  ├── gunCode: String
  ├── status: Integer (0:空闲, 1:充电中, 2:故障)
  └── type: Integer (快充/慢充)

ChargingSession (充电会话)
  ├── id: Long
  ├── gunId: Long
  ├── vehicleId: String
  ├── startTime: LocalDateTime
  ├── endTime: LocalDateTime
  ├── status: Integer
  └── totalPower: BigDecimal

ChargingData (充电实时数据)
  ├── sessionId: Long
  ├── voltage: BigDecimal
  ├── current: BigDecimal
  ├── power: BigDecimal
  ├── soc: Integer
  └── timestamp: LocalDateTime

VehicleBmsData (车辆BMS数据)
  ├── vehicleId: String
  ├── batteryVoltage: BigDecimal
  ├── batteryCurrent: BigDecimal
  ├── batteryTemp: BigDecimal
  ├── soc: Integer
  └── timestamp: LocalDateTime
```

### 2.4 MQTT 主题设计

```
充电数据主题:
  - charging/station/{stationId}/pile/{pileId}/gun/{gunId}/data
  
BMS 数据主题:
  - vehicle/{vehicleId}/bms/data
  
充电控制主题:
  - charging/station/{stationId}/pile/{pileId}/gun/{gunId}/control
```

## 3. 低层设计 (Low-Level Design)

### 3.1 API 接口设计

#### 3.1.1 站点管理接口

```java
// 获取站点列表
GET /api/stations
Response: List<StationVO>

// 获取站点详情（包含充电桩）
GET /api/stations/{stationId}
Response: StationDetailVO

// 获取充电桩详情（包含充电枪）
GET /api/piles/{pileId}
Response: ChargingPileDetailVO
```

#### 3.1.2 充电控制接口

```java
// 插枪
POST /api/charging/insert-gun
Request: { gunId: Long, vehicleId: String }
Response: { success: Boolean, sessionId: Long }

// 启动充电
POST /api/charging/start
Request: { sessionId: Long }
Response: { success: Boolean }

// 结束充电
POST /api/charging/stop
Request: { sessionId: Long }
Response: { success: Boolean, chargingResult: ChargingResultVO }

// 拔枪
POST /api/charging/remove-gun
Request: { sessionId: Long }
Response: { success: Boolean }

// 获取充电会话状态
GET /api/charging/session/{sessionId}
Response: ChargingSessionVO
```

#### 3.1.3 WebSocket 接口

```java
// WebSocket 连接端点
ws://localhost:8080/ws/charging/{sessionId}

// 消息格式
{
  "type": "CHARGING_DATA" | "BMS_DATA",
  "data": {
    "voltage": 380.5,
    "current": 125.3,
    "power": 47.6,
    "soc": 65,
    "timestamp": "2026-03-03T10:30:00"
  }
}
```

### 3.2 核心类设计

#### 3.2.1 Controller 层

```java
@RestController
@RequestMapping("/api/stations")
public class StationController {
    
    @Autowired
    private StationService stationService;
    
    @GetMapping
    public Result<List<StationVO>> listStations();
    
    @GetMapping("/{stationId}")
    public Result<StationDetailVO> getStationDetail(@PathVariable Long stationId);
}

@RestController
@RequestMapping("/api/charging")
public class ChargingController {
    
    @Autowired
    private ChargingService chargingService;
    
    @PostMapping("/insert-gun")
    public Result<Long> insertGun(@RequestBody InsertGunRequest request);
    
    @PostMapping("/start")
    public Result<Void> startCharging(@RequestBody StartChargingRequest request);
    
    @PostMapping("/stop")
    public Result<ChargingResultVO> stopCharging(@RequestBody StopChargingRequest request);
    
    @PostMapping("/remove-gun")
    public Result<Void> removeGun(@RequestBody RemoveGunRequest request);
}

@Controller
public class PageController {
    
    @GetMapping("/")
    public String index();
    
    @GetMapping("/stations")
    public String stationList(Model model);
    
    @GetMapping("/charging/{gunId}")
    public String chargingSimulator(@PathVariable Long gunId, Model model);
}
```

#### 3.2.2 Service 层

```java
@Service
public class StationService {
    
    @Autowired
    private StationMapper stationMapper;
    
    @Autowired
    private ChargingPileMapper pileMapper;
    
    @Autowired
    private ChargingGunMapper gunMapper;
    
    public List<StationVO> listStations();
    
    public StationDetailVO getStationDetail(Long stationId);
    
    public ChargingPileDetailVO getPileDetail(Long pileId);
}

@Service
public class ChargingService {
    
    @Autowired
    private ChargingSessionMapper sessionMapper;
    
    @Autowired
    private ChargingGunMapper gunMapper;
    
    @Autowired
    private MqttService mqttService;
    
    public Long insertGun(Long gunId, String vehicleId);
    
    public void startCharging(Long sessionId);
    
    public ChargingResultVO stopCharging(Long sessionId);
    
    public void removeGun(Long sessionId);
    
    public ChargingSessionVO getSessionStatus(Long sessionId);
}

@Service
public class MqttService {
    
    private MqttClient mqttClient;
    
    @Autowired
    private WebSocketService webSocketService;
    
    @PostConstruct
    public void init();
    
    public void subscribeChargingData(Long gunId);
    
    public void subscribeBmsData(String vehicleId);
    
    public void publishControlCommand(Long gunId, String command);
    
    private void handleChargingDataMessage(String topic, MqttMessage message);
    
    private void handleBmsDataMessage(String topic, MqttMessage message);
}

@Service
public class WebSocketService {
    
    private Map<Long, Set<WebSocketSession>> sessionMap;
    
    public void registerSession(Long chargingSessionId, WebSocketSession wsSession);
    
    public void unregisterSession(Long chargingSessionId, WebSocketSession wsSession);
    
    public void pushChargingData(Long chargingSessionId, ChargingDataVO data);
    
    public void pushBmsData(Long chargingSessionId, VehicleBmsDataVO data);
}
```

#### 3.2.3 MQTT 消息处理

```java
@Component
public class MqttMessageHandler implements MqttCallback {
    
    @Autowired
    private WebSocketService webSocketService;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Override
    public void messageArrived(String topic, MqttMessage message) {
        String payload = new String(message.getPayload());
        
        if (topic.contains("/charging/")) {
            handleChargingData(topic, payload);
        } else if (topic.contains("/vehicle/")) {
            handleBmsData(topic, payload);
        }
    }
    
    private void handleChargingData(String topic, String payload) {
        // 解析主题获取 gunId
        Long gunId = parseGunIdFromTopic(topic);
        
        // 解析充电数据
        ChargingDataVO data = JSON.parseObject(payload, ChargingDataVO.class);
        
        // 获取充电会话ID
        Long sessionId = getActiveSessionByGunId(gunId);
        
        // 推送到 WebSocket
        webSocketService.pushChargingData(sessionId, data);
        
        // 缓存到 Redis
        redisTemplate.opsForValue().set(
            "charging:data:" + sessionId, 
            data, 
            Duration.ofMinutes(5)
        );
    }
    
    private void handleBmsData(String topic, String payload) {
        // 解析主题获取 vehicleId
        String vehicleId = parseVehicleIdFromTopic(topic);
        
        // 解析 BMS 数据
        VehicleBmsDataVO data = JSON.parseObject(payload, VehicleBmsDataVO.class);
        
        // 获取充电会话ID
        Long sessionId = getActiveSessionByVehicleId(vehicleId);
        
        // 推送到 WebSocket
        webSocketService.pushBmsData(sessionId, data);
    }
}
```

### 3.3 前端页面设计

#### 3.3.1 站点列表页面 (stations.html)

```html
<!-- 页面结构 -->
<div class="container">
    <h1>充电站列表</h1>
    <div id="station-list">
        <!-- 动态加载站点列表 -->
    </div>
</div>

<!-- JavaScript 逻辑 -->
<script>
// 加载站点列表
function loadStations() {
    fetch('/api/stations')
        .then(response => response.json())
        .then(data => renderStations(data));
}

// 点击站点查看充电桩
function viewStation(stationId) {
    window.location.href = '/station/' + stationId;
}
</script>
```

#### 3.3.2 充电模拟器页面 (charging-simulator.html)

```html
<!-- 页面布局 -->
<div class="simulator-container">
    <!-- 左侧：充电站数据 -->
    <div class="left-panel">
        <h2>充电枪信息</h2>
        <div id="gun-info"></div>
        
        <h2>充电控制</h2>
        <div class="control-buttons">
            <button id="btn-insert-gun">插枪</button>
            <button id="btn-start-charging" disabled>启动充电</button>
            <button id="btn-stop-charging" disabled>结束充电</button>
            <button id="btn-remove-gun" disabled>拔枪</button>
        </div>
        
        <h2>充电数据</h2>
        <div id="charging-data" class="data-display">
            <div>电压: <span id="voltage">--</span> V</div>
            <div>电流: <span id="current">--</span> A</div>
            <div>功率: <span id="power">--</span> kW</div>
            <div>SOC: <span id="soc">--</span> %</div>
        </div>
    </div>
    
    <!-- 右侧：车辆数据 -->
    <div class="right-panel" id="vehicle-panel">
        <h2>车辆 BMS 数据</h2>
        <div id="bms-data" class="data-display grayed-out">
            <div>电池电压: <span id="battery-voltage">--</span> V</div>
            <div>电池电流: <span id="battery-current">--</span> A</div>
            <div>电池温度: <span id="battery-temp">--</span> °C</div>
            <div>SOC: <span id="battery-soc">--</span> %</div>
        </div>
    </div>
</div>

<!-- WebSocket 连接 -->
<script>
let ws;
let sessionId;

// 插枪
function insertGun() {
    fetch('/api/charging/insert-gun', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ gunId: gunId, vehicleId: 'TEST_VEHICLE_001' })
    })
    .then(response => response.json())
    .then(data => {
        sessionId = data.data;
        document.getElementById('btn-start-charging').disabled = false;
    });
}

// 启动充电
function startCharging() {
    fetch('/api/charging/start', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ sessionId: sessionId })
    })
    .then(() => {
        connectWebSocket();
        document.getElementById('btn-stop-charging').disabled = false;
        document.getElementById('vehicle-panel').classList.remove('grayed-out');
    });
}

// 连接 WebSocket
function connectWebSocket() {
    ws = new WebSocket('ws://localhost:8080/ws/charging/' + sessionId);
    
    ws.onmessage = function(event) {
        const message = JSON.parse(event.data);
        
        if (message.type === 'CHARGING_DATA') {
            updateChargingData(message.data);
        } else if (message.type === 'BMS_DATA') {
            updateBmsData(message.data);
        }
    };
}

// 更新充电数据
function updateChargingData(data) {
    document.getElementById('voltage').textContent = data.voltage;
    document.getElementById('current').textContent = data.current;
    document.getElementById('power').textContent = data.power;
    document.getElementById('soc').textContent = data.soc;
}

// 更新 BMS 数据
function updateBmsData(data) {
    document.getElementById('battery-voltage').textContent = data.batteryVoltage;
    document.getElementById('battery-current').textContent = data.batteryCurrent;
    document.getElementById('battery-temp').textContent = data.batteryTemp;
    document.getElementById('battery-soc').textContent = data.soc;
}
</script>
```

### 3.4 数据库表设计

```sql
-- 充电站表
CREATE TABLE t_station (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '站点名称',
    address VARCHAR(200) COMMENT '站点地址',
    status TINYINT DEFAULT 1 COMMENT '状态：0-停用，1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 充电桩表
CREATE TABLE t_charging_pile (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    station_id BIGINT NOT NULL COMMENT '所属站点ID',
    pile_code VARCHAR(50) NOT NULL COMMENT '充电桩编号',
    status TINYINT DEFAULT 1 COMMENT '状态：0-故障，1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (station_id) REFERENCES t_station(id)
);

-- 充电枪表
CREATE TABLE t_charging_gun (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    pile_id BIGINT NOT NULL COMMENT '所属充电桩ID',
    gun_code VARCHAR(50) NOT NULL COMMENT '充电枪编号',
    type TINYINT DEFAULT 1 COMMENT '类型：1-快充，2-慢充',
    status TINYINT DEFAULT 0 COMMENT '状态：0-空闲，1-充电中，2-故障',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (pile_id) REFERENCES t_charging_pile(id)
);

-- 充电会话表
CREATE TABLE t_charging_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    gun_id BIGINT NOT NULL COMMENT '充电枪ID',
    vehicle_id VARCHAR(50) NOT NULL COMMENT '车辆ID',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    status TINYINT DEFAULT 0 COMMENT '状态：0-已插枪，1-充电中，2-已结束',
    total_power DECIMAL(10,2) COMMENT '总电量(kWh)',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (gun_id) REFERENCES t_charging_gun(id)
);
```

### 3.5 配置文件设计

```yaml
# application.yml
server:
  port: 8080

spring:
  application:
    name: charging-simulator-boot
  
  datasource:
    type: com.zaxxer.hikari.HikariDataSource
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/charging_simulator?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: root
  
  redis:
    host: localhost
    port: 6379
    database: 0
    timeout: 3000ms

# MQTT 配置
mqtt:
  broker-url: tcp://localhost:1883
  client-id: charging-simulator-${random.value}
  username: admin
  password: admin
  topics:
    charging-data: charging/+/pile/+/gun/+/data
    bms-data: vehicle/+/bms/data
  qos: 1

# MyBatis-Plus 配置
mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  type-aliases-package: chat.wisechat.charging.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

## 4. 关键算法与流程

### 4.1 充电流程状态机

```
[空闲] --插枪--> [已插枪] --启动充电--> [充电中] --结束充电--> [已结束] --拔枪--> [空闲]
```

### 4.2 MQTT 消息处理流程

```
1. MQTT Broker 发布消息
2. MqttService 接收消息
3. 解析消息内容和主题
4. 根据主题类型分发到对应处理器
5. 查询充电会话ID
6. 通过 WebSocket 推送到前端
7. 缓存数据到 Redis
```

### 4.3 实时数据推送流程

```
1. 前端建立 WebSocket 连接
2. 后端注册会话映射关系
3. MQTT 消息到达
4. 查找对应的 WebSocket 会话
5. 推送数据到前端
6. 前端更新界面显示
```

## 5. 非功能性设计

### 5.1 性能优化
- 使用 Redis 缓存充电会话状态
- 使用 Caffeine 本地缓存站点数据
- WebSocket 连接池管理
- MQTT 消息异步处理

### 5.2 可靠性设计
- MQTT 消息 QoS 设置为 1（至少一次）
- WebSocket 断线重连机制
- 充电会话状态持久化
- 异常情况下的状态恢复

### 5.3 安全性设计
- MQTT 连接认证
- API 接口参数校验
- WebSocket 连接鉴权
- SQL 注入防护（MyBatis-Plus）

## 6. 部署架构

```
┌─────────────────┐
│   Nginx/前端     │
└────────┬────────┘
         │
┌────────▼────────┐
│  Spring Boot    │
│   Application   │
└────────┬────────┘
         │
    ┌────┴────┬────────┬────────┐
    │         │        │        │
┌───▼───┐ ┌──▼──┐ ┌───▼───┐ ┌──▼──┐
│ MySQL │ │MQTT │ │ Redis │ │ Log │
└───────┘ └─────┘ └───────┘ └─────┘
```

## 7. 开发计划

### 7.1 第一阶段：基础框架
- 数据库表设计与创建
- 实体类和 Mapper 创建
- 基础配置完成

### 7.2 第二阶段：站点管理
- 站点查询接口
- 充电桩和充电枪查询
- 前端列表页面

### 7.3 第三阶段：充电模拟
- 充电控制接口
- 充电状态管理
- 前端模拟器页面

### 7.4 第四阶段：实时数据
- MQTT 集成
- WebSocket 实现
- 实时数据推送

### 7.5 第五阶段：测试与优化
- 功能测试
- 性能优化
- 文档完善
