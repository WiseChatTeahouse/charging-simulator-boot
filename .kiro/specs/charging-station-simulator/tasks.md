# 实施计划：充电站模拟器系统

## 概述

本实施计划将充电站模拟器系统的设计转换为可执行的开发任务。系统基于 Spring Boot 3.5.11 + MySQL + MQTT + WebSocket 架构，实现充电站管理、充电过程模拟和实时数据展示功能。

## 任务列表

- [ ] 1. 搭建项目基础框架和数据库
  - [x] 1.1 创建 Spring Boot 项目并配置依赖
    - 创建 Maven/Gradle 项目，配置 Spring Boot 3.5.11
    - 添加依赖：Undertow、MyBatis-Plus、MySQL、Redis、MQTT (Eclipse Paho)、WebSocket、Thymeleaf
    - 配置 application.yml（数据库、Redis、MQTT 连接信息）
    - _需求: 6.1, 3.5.3_
  
  - [x] 1.2 创建数据库表结构
    - 执行 SQL 脚本创建 t_station、t_charging_pile、t_charging_gun、t_charging_session 表
    - 配置外键关系和索引
    - 插入测试数据（至少 2 个站点、每个站点 2 个充电桩、每个充电桩 2 把充电枪）
    - _需求: 4.1.1, 4.1.2, 4.1.3, 4.1.4_
  
  - [x] 1.3 创建实体类和基础配置
    - 创建 Station、ChargingPile、ChargingGun、ChargingSession 实体类
    - 配置 MyBatis-Plus 注解和字段映射
    - 创建统一响应类 Result<T>
    - 创建全局异常处理器
    - _需求: 3.5.1, 4.1.1, 4.1.2, 4.1.3, 4.1.4_

- [x] 2. 实现站点管理模块
  - [x] 2.1 实现 Mapper 层数据访问
    - 创建 StationMapper、ChargingPileMapper、ChargingGunMapper 接口
    - 实现查询站点列表方法
    - 实现根据站点ID查询充电桩列表方法
    - 实现根据充电桩ID查询充电枪列表方法
    - _需求: 2.1.1, 2.1.2, 2.1.3_
  
  - [x] 2.2 实现 Service 层业务逻辑
    - 创建 StationService 类
    - 实现 listStations() 方法（查询所有站点）
    - 实现 getStationDetail(Long stationId) 方法（查询站点及其充电桩）
    - 实现 getPileDetail(Long pileId) 方法（查询充电桩及其充电枪）
    - 配置 Caffeine 本地缓存站点数据
    - _需求: 2.1.1, 2.1.2, 2.1.3, 3.1.3_
  
  - [x] 2.3 实现 Controller 层 REST API
    - 创建 StationController 类
    - 实现 GET /api/stations 接口（返回站点列表）
    - 实现 GET /api/stations/{stationId} 接口（返回站点详情）
    - 实现 GET /api/piles/{pileId} 接口（返回充电桩详情）
    - 添加参数校验和异常处理
    - _需求: 5.1.1, 5.1.2, 5.1.3, 3.4.1_
  
  - [ ]* 2.4 编写站点管理模块单元测试
    - 测试 StationService 的查询方法
    - 测试缓存功能
    - 测试异常场景（站点不存在、充电桩不存在）
    - _需求: 9.1_

- [x] 3. 检查点 - 确保站点管理功能正常
  - 确保所有测试通过，如有问题请询问用户

- [x] 4. 实现充电模拟模块
  - [x] 4.1 创建充电会话相关实体和 VO 类
    - 创建 ChargingSessionMapper 接口
    - 创建 InsertGunRequest、StartChargingRequest、StopChargingRequest、RemoveGunRequest 请求类
    - 创建 ChargingSessionVO、ChargingResultVO 响应类
    - _需求: 4.1.4, 5.1.4, 5.1.5, 5.1.6, 5.1.7_
  
  - [x] 4.2 实现充电流程状态管理
    - 创建 ChargingService 类
    - 实现 insertGun(Long gunId, String vehicleId) 方法（创建会话，状态设为 INSERTED）
    - 实现 startCharging(Long sessionId) 方法（更新会话状态为 CHARGING，记录开始时间）
    - 实现 stopCharging(Long sessionId) 方法（更新会话状态为 FINISHED，记录结束时间和总电量）
    - 实现 removeGun(Long sessionId) 方法（结束会话，充电枪状态恢复为 IDLE）
    - 实现充电枪状态同步更新逻辑
    - 使用 Redis 缓存充电会话状态
    - _需求: 2.2.1, 2.2.2, 2.2.3, 2.2.4, 6.2, 3.1.3_
  
  - [x] 4.3 实现充电控制 REST API
    - 创建 ChargingController 类
    - 实现 POST /api/charging/insert-gun 接口
    - 实现 POST /api/charging/start 接口
    - 实现 POST /api/charging/stop 接口
    - 实现 POST /api/charging/remove-gun 接口
    - 实现 GET /api/charging/session/{sessionId} 接口
    - 添加状态转换合法性校验（防止非法状态转换）
    - _需求: 5.1.4, 5.1.5, 5.1.6, 5.1.7, 3.4.1_
  
  - [ ]* 4.4 编写充电流程状态转换属性测试
    - **属性 8.2: 充电流程状态转换合法性**
    - **验证需求: 6.2**
    - 生成随机状态转换序列，验证合法转换成功、非法转换被拒绝
    - _需求: 8.2, 9.3_
  
  - [ ]* 4.5 编写充电会话唯一性属性测试
    - **属性 8.3: 充电会话唯一性**
    - **验证需求: 6.2**
    - 测试同一充电枪不能同时有多个活跃会话
    - _需求: 8.3, 9.3_
  
  - [ ]* 4.6 编写充电枪状态一致性属性测试
    - **属性 8.1: 充电枪状态一致性**
    - **验证需求: 3.2.3**
    - 验证充电枪状态与会话状态始终保持一致
    - _需求: 8.1, 9.3_

- [x] 5. 实现 MQTT 数据接收模块
  - [x] 5.1 配置 MQTT 客户端连接
    - 创建 MqttConfig 配置类
    - 配置 MQTT 连接参数（broker URL、client ID、认证信息）
    - 实现 MQTT 连接和断线重连机制
    - _需求: 3.2.2, 3.4.2, 5.3.1, 5.3.2_
  
  - [x] 5.2 实现 MQTT 消息处理服务
    - 创建 MqttService 类
    - 实现 subscribeChargingData(Long gunId) 方法（订阅充电数据主题）
    - 实现 subscribeBmsData(String vehicleId) 方法（订阅 BMS 数据主题）
    - 创建 ChargingDataVO 和 VehicleBmsDataVO 类
    - _需求: 2.3.1, 2.3.2, 5.3.1, 5.3.2_
  
  - [x] 5.3 实现 MQTT 消息解析和分发
    - 创建 MqttMessageHandler 类实现 MqttCallback 接口
    - 实现 messageArrived() 方法解析 MQTT 消息
    - 实现 handleChargingData() 方法处理充电数据
    - 实现 handleBmsData() 方法处理 BMS 数据
    - 从主题中解析 gunId 和 vehicleId
    - 查询充电会话ID并缓存到 Redis
    - _需求: 2.3.1, 2.3.2, 4.2.2, 3.1.2_
  
  - [ ]* 5.4 编写实时数据时序性属性测试
    - **属性 8.4: 实时数据时序性**
    - **验证需求: 2.3.1, 2.3.2**
    - 验证推送数据的时间戳单调递增
    - _需求: 8.4, 9.3_

- [x] 6. 实现 WebSocket 实时推送模块
  - [x] 6.1 配置 WebSocket 端点
    - 创建 WebSocketConfig 配置类
    - 配置 WebSocket 端点 /ws/charging/{sessionId}
    - 实现 WebSocket 握手拦截器（会话验证）
    - _需求: 5.2.1, 5.2.2, 3.4.2_
  
  - [x] 6.2 实现 WebSocket 消息推送服务
    - 创建 WebSocketService 类
    - 实现 registerSession(Long chargingSessionId, WebSocketSession wsSession) 方法
    - 实现 unregisterSession(Long chargingSessionId, WebSocketSession wsSession) 方法
    - 实现 pushChargingData(Long chargingSessionId, ChargingDataVO data) 方法
    - 实现 pushBmsData(Long chargingSessionId, VehicleBmsDataVO data) 方法
    - 维护 sessionMap（充电会话ID -> WebSocket会话集合）
    - _需求: 2.3.1, 2.3.2, 3.1.2_
  
  - [x] 6.3 集成 MQTT 和 WebSocket
    - 在 MqttMessageHandler 中注入 WebSocketService
    - MQTT 消息到达后通过 WebSocketService 推送到前端
    - 实现消息格式转换（MQTT payload -> WebSocket message）
    - 添加异常处理和日志记录
    - _需求: 4.3.2, 3.5.2_
  
  - [ ]* 6.4 编写 WebSocket 连接绑定属性测试
    - **属性 8.6: WebSocket 连接与会话绑定**
    - **验证需求: 2.3.1, 2.3.2**
    - 验证每个 WebSocket 连接只接收对应会话的数据
    - _需求: 8.6, 9.3_

- [x] 7. 检查点 - 确保后端功能完整
  - 确保所有测试通过，如有问题请询问用户

- [x] 8. 实现前端页面
  - [x] 8.1 创建站点列表页面
    - 创建 templates/stations.html 页面
    - 实现站点列表展示（使用 Thymeleaf 或 JavaScript 动态加载）
    - 实现点击站点跳转到充电桩列表
    - 添加 CSS 样式美化页面
    - _需求: 2.1.1, 2.4.1, 2.4.2, 3.3.1_
  
  - [x] 8.2 创建充电桩和充电枪列表页面
    - 创建 templates/station-detail.html 页面
    - 展示充电桩列表和充电枪列表
    - 实现点击充电枪跳转到充电模拟器
    - 显示充电枪状态（空闲/充电中/故障）
    - _需求: 2.1.2, 2.1.3, 2.4.3, 3.3.1_
  
  - [x] 8.3 创建充电模拟器页面布局
    - 创建 templates/charging-simulator.html 页面
    - 实现左右分栏布局（左侧充电站数据，右侧车辆数据）
    - 添加充电控制按钮（插枪、启动充电、结束充电、拔枪）
    - 添加充电数据显示区域（电压、电流、功率、SOC）
    - 添加 BMS 数据显示区域（电池电压、电池电流、电池温度、SOC）
    - 右侧面板初始状态设为置灰
    - _需求: 2.2.1, 2.2.2, 2.2.3, 2.2.4, 2.3.1, 2.3.2, 3.3.1_
  
  - [x] 8.4 实现充电控制交互逻辑
    - 实现插枪按钮点击事件（调用 /api/charging/insert-gun 接口）
    - 实现启动充电按钮点击事件（调用 /api/charging/start 接口）
    - 实现结束充电按钮点击事件（调用 /api/charging/stop 接口）
    - 实现拔枪按钮点击事件（调用 /api/charging/remove-gun 接口）
    - 实现按钮状态管理（根据充电流程启用/禁用按钮）
    - 插枪成功后启用"启动充电"按钮
    - 启动充电后启用"结束充电"按钮，禁用"插枪"和"启动充电"
    - 结束充电后启用"拔枪"按钮
    - _需求: 2.2.1, 2.2.2, 2.2.3, 2.2.4, 3.3.2_
  
  - [x] 8.5 实现 WebSocket 连接和实时数据更新
    - 启动充电后建立 WebSocket 连接（ws://localhost:8080/ws/charging/{sessionId}）
    - 实现 WebSocket onmessage 事件处理
    - 根据消息类型（CHARGING_DATA / BMS_DATA）更新对应数据显示
    - 实现 updateChargingData() 函数更新左侧充电数据
    - 实现 updateBmsData() 函数更新右侧 BMS 数据
    - 启动充电后右侧面板从置灰变为正常显示
    - 结束充电后断开 WebSocket 连接
    - 实现 WebSocket 断线重连机制
    - _需求: 2.3.1, 2.3.2, 3.2.2, 3.3.1_
  
  - [x] 8.6 创建页面路由控制器
    - 创建 PageController 类
    - 实现 GET / 路由（首页，重定向到站点列表）
    - 实现 GET /stations 路由（站点列表页面）
    - 实现 GET /station/{stationId} 路由（站点详情页面）
    - 实现 GET /charging/{gunId} 路由（充电模拟器页面）
    - 传递必要的数据到模板（gunId、充电枪信息等）
    - _需求: 2.4.1, 2.4.2, 2.4.3_

- [x] 9. 集成测试和优化
  - [x] 9.1 执行端到端集成测试
    - 测试完整充电流程（插枪 → 启动 → 实时数据 → 结束 → 拔枪）
    - 测试多充电枪并发充电场景
    - 测试 WebSocket 断线重连
    - 测试 MQTT 消息处理
    - _需求: 9.2, 9.4_
  
  - [ ]* 9.2 编写充电总电量非负性属性测试
    - **属性 8.5: 充电总电量非负性**
    - **验证需求: 2.2.3**
    - 验证各种充电时长和功率下总电量始终非负
    - _需求: 8.5, 9.3_
  
  - [x] 9.3 性能优化和监控
    - 验证页面加载时间符合性能需求（< 2秒）
    - 验证 WebSocket 推送延迟（< 100ms）
    - 验证缓存命中率（> 80%）
    - 测试 50 个并发充电会话
    - 添加关键操作日志记录
    - _需求: 3.1.1, 3.1.2, 3.1.3, 3.5.2_
  
  - [x] 9.4 安全性检查和加固
    - 验证 API 参数校验
    - 验证 SQL 注入防护
    - 验证 MQTT 和 WebSocket 连接认证
    - 检查敏感配置是否加密
    - _需求: 3.4.1, 3.4.2, 3.4.3_

- [x] 10. 最终检查点 - 确保所有功能完整可用
  - 确保所有测试通过，系统满足验收标准，如有问题请询问用户

## 注意事项

- 标记 `*` 的任务为可选任务，可跳过以加快 MVP 开发
- 每个任务都引用了具体的需求编号以确保可追溯性
- 检查点任务确保增量验证
- 属性测试验证通用正确性属性
- 单元测试和集成测试验证具体示例和边界情况
