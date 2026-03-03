# WebSocket充电持久性修复设计文档

## Overview

当前实现中，充电会话的生命周期错误地与WebSocket连接绑定。当用户关闭页面或WebSocket连接断开时，虽然后端充电会话仍在数据库中保持活跃状态，但前端无法恢复连接并继续显示充电数据。本修复将解耦WebSocket连接与充电会话的生命周期，使充电会话能够独立于前端页面状态持续进行，并支持用户返回页面时自动恢复连接和状态。

核心策略：
- 充电会话完全独立于WebSocket连接，由用户显式操作（插枪、启动充电、结束充电、拔枪）控制
- WebSocket仅用于实时数据推送，连接断开不影响充电会话
- 页面加载时检查是否存在活跃充电会话，自动恢复WebSocket连接和UI状态
- 保持所有现有的充电控制流程不变

## Glossary

- **Bug_Condition (C)**: WebSocket连接断开（页面关闭、导航离开、网络问题）时充电会话仍在进行中
- **Property (P)**: 充电会话应该继续进行，用户返回页面时能够恢复连接并显示当前状态
- **Preservation**: 所有现有的充电控制操作（插枪、启动充电、结束充电、拔枪、重置）必须保持原有行为不变
- **ChargingSession**: 数据库中的充电会话实体，包含状态（0=已插枪，1=充电中，2=已结束）
- **WebSocketService**: 管理WebSocket连接的服务类，负责注册/注销连接和推送数据
- **sessionId**: 充电会话的唯一标识符，用于关联WebSocket连接和充电会话

## Bug Details

### Fault Condition

当用户在充电过程中离开页面时，WebSocket连接会被关闭，但充电会话在后端仍然保持活跃状态（status=1）。用户返回页面时，前端无法检测到活跃会话并恢复连接，导致用户看到的是初始状态（空闲），而实际充电仍在后台进行。

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type PageNavigationEvent OR WebSocketCloseEvent
  OUTPUT: boolean
  
  RETURN (input.type == "PAGE_CLOSE" OR input.type == "PAGE_NAVIGATION" OR input.type == "WEBSOCKET_DISCONNECT")
         AND existsActiveChargingSession(input.gunId)
         AND chargingSessionStatus(input.sessionId) == 1  // 充电中
         AND NOT canResumeOnReturn()
END FUNCTION
```

### Examples

- **场景1**: 用户在充电过程中关闭浏览器标签页
  - 当前行为：WebSocket断开，前端无法再接收数据，用户返回时看到空闲状态
  - 期望行为：充电会话继续，用户返回时自动恢复连接并显示当前充电状态

- **场景2**: 用户在充电过程中导航到其他页面（如返回充电站列表）
  - 当前行为：WebSocket断开，充电会话继续但前端无法恢复
  - 期望行为：充电会话继续，用户返回充电枪详情页时自动恢复连接

- **场景3**: WebSocket因网络问题断开
  - 当前行为：前端有重连逻辑，但依赖sessionId变量，页面刷新后丢失
  - 期望行为：页面刷新后能够检测到活跃会话并自动恢复连接

- **边缘情况**: 用户在充电结束后返回页面
  - 期望行为：显示充电已结束状态，允许拔枪操作

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- 用户点击"结束充电"按钮时，必须正常结束充电会话并关闭WebSocket连接
- 用户在充电结束后拔枪时，必须正常清理会话状态和恢复充电枪为空闲状态
- 用户使用重置功能时，必须强制结束所有活跃会话并重置充电枪状态
- WebSocket连接正常建立时，必须实时推送充电数据和BMS数据
- 充电会话不存在时用户尝试操作，必须返回适当的错误提示
- 充电枪状态为空闲时，只允许插枪操作
- 充电枪状态为充电中时，只允许结束充电操作

**Scope:**
所有不涉及页面导航或WebSocket断开的操作应该完全不受影响。这包括：
- 所有充电控制API调用（插枪、启动充电、结束充电、拔枪、重置）
- WebSocket数据推送逻辑
- 充电会话状态管理
- 数据库和缓存操作

## Hypothesized Root Cause

基于代码分析，问题的根本原因是：

1. **缺少会话恢复机制**: 前端页面加载时没有检查是否存在活跃的充电会话
   - `charging-simulator.html`在页面加载时只初始化UI，不查询后端状态
   - `sessionId`变量仅在插枪操作后设置，页面刷新后丢失
   - 没有API端点用于根据gunId查询活跃会话

2. **WebSocket连接依赖前端状态**: WebSocket连接的建立依赖于前端的sessionId变量
   - `connectWebSocket()`函数需要sessionId参数
   - 页面刷新或重新打开后，sessionId丢失，无法建立连接

3. **UI状态不同步**: 前端UI状态（按钮启用/禁用、状态文本）不从后端同步
   - UI状态仅在用户操作后更新
   - 页面加载时UI始终显示为空闲状态

4. **缺少状态查询API**: 虽然存在`getSessionStatus`方法，但前端没有在页面加载时调用
   - 需要一个根据gunId查询活跃会话的API
   - 需要在页面加载时调用此API并恢复状态

## Correctness Properties

Property 1: Fault Condition - 充电会话独立于WebSocket连接

_For any_ 页面导航事件或WebSocket断开事件，当充电会话处于活跃状态（status=1）时，充电会话SHALL继续进行而不被中断，并且用户返回页面时SHALL能够自动恢复WebSocket连接并显示当前充电状态。

**Validates: Requirements 2.1, 2.2, 2.3, 2.4, 2.5**

Property 2: Preservation - 充电控制操作行为不变

_For any_ 充电控制操作（插枪、启动充电、结束充电、拔枪、重置），修复后的代码SHALL产生与原始代码完全相同的行为，保持所有现有的状态转换、数据库操作、缓存操作和WebSocket推送逻辑不变。

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 3.5, 3.6, 3.7**

## Fix Implementation

### Changes Required

基于根本原因分析，需要进行以下修改：

**File**: `src/main/java/chat/wisechat/charging/controller/ChargingController.java`

**Function**: 新增API端点

**Specific Changes**:
1. **新增根据gunId查询活跃会话的API**:
   - 添加`GET /api/charging/active-session?gunId={gunId}`端点
   - 返回活跃会话信息（sessionId、status、vehicleId等）
   - 如果没有活跃会话，返回null或空对象

**File**: `src/main/java/chat/wisechat/charging/service/ChargingService.java`

**Function**: 新增业务方法

**Specific Changes**:
2. **实现查询活跃会话的业务逻辑**:
   - 添加`getActiveSessionByGunId(Long gunId)`方法
   - 首先从Redis缓存查询`gun:session:{gunId}`
   - 如果缓存未命中，从数据库查询status=0或1的会话
   - 返回会话VO对象，包含完整的会话信息

**File**: `src/main/resources/templates/charging-simulator.html`

**Function**: 页面初始化逻辑

**Specific Changes**:
3. **添加页面加载时的状态恢复逻辑**:
   - 在`window.onload`或页面初始化时调用新的API
   - 如果存在活跃会话，设置sessionId变量
   - 根据会话状态更新UI（按钮状态、状态文本）
   - 如果会话状态为充电中（status=1），自动建立WebSocket连接

4. **优化WebSocket重连逻辑**:
   - 保持现有的重连机制（3秒后重试）
   - 确保重连逻辑在页面刷新后仍然有效

5. **添加状态同步函数**:
   - 创建`restoreSessionState(sessionData)`函数
   - 根据会话数据恢复UI状态
   - 根据会话状态决定是否建立WebSocket连接

## Testing Strategy

### Validation Approach

测试策略采用两阶段方法：首先在未修复的代码上演示bug，然后验证修复后的代码正确工作并保持现有行为不变。

### Exploratory Fault Condition Checking

**Goal**: 在实施修复之前，在未修复的代码上演示bug。确认或反驳根本原因分析。如果反驳，需要重新假设。

**Test Plan**: 编写测试模拟页面关闭和重新打开的场景，验证充电会话是否继续以及返回时是否能恢复。在未修复的代码上运行这些测试以观察失败并理解根本原因。

**Test Cases**:
1. **页面关闭后充电会话持续性测试**: 启动充电后关闭页面，检查数据库中会话状态是否仍为充电中（将在未修复代码上通过，因为后端会话确实继续）
2. **页面重新打开后状态恢复测试**: 启动充电后关闭页面再重新打开，检查是否能恢复连接（将在未修复代码上失败）
3. **WebSocket断开后会话持续性测试**: 模拟WebSocket断开，检查充电会话是否继续（将在未修复代码上通过）
4. **页面刷新后状态恢复测试**: 启动充电后刷新页面，检查UI状态是否正确恢复（将在未修复代码上失败）

**Expected Counterexamples**:
- 页面重新打开时，前端无法检测到活跃会话
- 可能原因：缺少状态查询API、缺少页面加载时的状态恢复逻辑

### Fix Checking

**Goal**: 验证对于所有满足bug条件的输入，修复后的功能产生期望的行为。

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := handlePageNavigation_fixed(input)
  ASSERT chargingSessionContinues(result)
  ASSERT canResumeOnReturn(result)
END FOR
```

### Preservation Checking

**Goal**: 验证对于所有不满足bug条件的输入，修复后的功能产生与原始功能相同的结果。

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT handleChargingOperation_original(input) = handleChargingOperation_fixed(input)
END FOR
```

**Testing Approach**: 推荐使用基于属性的测试进行保持性检查，因为：
- 它自动生成许多测试用例覆盖输入域
- 它捕获手动单元测试可能遗漏的边缘情况
- 它为所有非bug输入提供强有力的行为不变保证

**Test Plan**: 首先在未修复的代码上观察充电控制操作的行为，然后编写基于属性的测试捕获该行为。

**Test Cases**:
1. **插枪操作保持性**: 在未修复代码上观察插枪操作的完整行为，然后验证修复后行为相同
2. **启动充电操作保持性**: 在未修复代码上观察启动充电的完整行为，然后验证修复后行为相同
3. **结束充电操作保持性**: 在未修复代码上观察结束充电的完整行为，然后验证修复后行为相同
4. **拔枪操作保持性**: 在未修复代码上观察拔枪操作的完整行为，然后验证修复后行为相同
5. **重置操作保持性**: 在未修复代码上观察重置操作的完整行为，然后验证修复后行为相同

### Unit Tests

- 测试新增的`getActiveSessionByGunId`方法在各种场景下的行为（有活跃会话、无活跃会话、多个会话）
- 测试新增的API端点返回正确的数据格式
- 测试前端状态恢复函数正确设置UI状态
- 测试WebSocket连接在状态恢复后能够正常建立

### Property-Based Tests

- 生成随机的充电会话状态，验证状态查询API总是返回正确的活跃会话
- 生成随机的页面导航序列，验证充电会话始终保持一致性
- 测试所有充电控制操作在修复前后产生相同的数据库和缓存状态

### Integration Tests

- 测试完整的充电流程：插枪 -> 启动充电 -> 关闭页面 -> 重新打开 -> 继续充电 -> 结束充电 -> 拔枪
- 测试在充电过程中多次关闭和打开页面
- 测试在不同会话状态下打开页面的行为（空闲、已插枪、充电中、已结束）
- 测试WebSocket断开和重连在状态恢复后的行为
