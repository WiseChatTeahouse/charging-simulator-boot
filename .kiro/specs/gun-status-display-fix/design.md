# 充电枪详情页面状态显示错误修复设计

## Overview

充电枪在列表页面显示"充电中"状态，但进入详情页面时显示"插枪"按钮（应显示"结束充电"按钮）。根本原因是状态管理不一致：充电操作（插枪、启动充电）更新了数据库中的充电枪状态，但详情页面通过StationService.getPileDetail()从Caffeine本地缓存读取数据，缓存过期时间为10分钟，导致无法获取最新状态。此外，系统缺少重置功能来将充电枪状态恢复为空闲。

修复策略：
1. 在充电操作后清除相关的Caffeine缓存，确保详情页面能读取到最新状态
2. 添加重置功能，允许将充电枪状态重置为空闲并清除相关缓存
3. 确保所有状态变更操作都同步更新数据库和清除缓存

## Glossary

- **Bug_Condition (C)**: 充电枪状态在数据库中已更新，但详情页面从过期的Caffeine缓存读取到旧状态
- **Property (P)**: 详情页面显示的充电枪状态和按钮状态应与数据库中的实时状态一致
- **Preservation**: 列表页面的状态显示、充电操作流程、WebSocket实时推送等现有功能必须保持不变
- **ChargingService**: `src/main/java/chat/wisechat/charging/service/ChargingService.java` 中的服务类，负责处理充电操作（插枪、启动充电、结束充电、拔枪）
- **StationService**: `src/main/java/chat/wisechat/charging/service/StationService.java` 中的服务类，负责查询站点、充电桩、充电枪信息，使用Caffeine本地缓存
- **Caffeine Cache**: StationService中使用的本地缓存，缓存键为"pile:detail:{pileId}"，过期时间10分钟
- **ChargingGun.status**: 充电枪状态字段，0=空闲，1=充电中，2=故障

## Bug Details

### Fault Condition

当用户执行充电操作（插枪、启动充电）后，ChargingService更新了数据库中的充电枪状态，但没有清除StationService中的Caffeine缓存。当用户进入详情页面时，StationService.getPileDetail()从缓存中读取到旧的充电枪状态，导致页面显示错误的按钮状态。

**Formal Specification:**
```
FUNCTION isBugCondition(input)
  INPUT: input of type { gunId: Long, operation: String }
  OUTPUT: boolean
  
  RETURN operation IN ['insertGun', 'startCharging', 'stopCharging', 'removeGun']
         AND databaseGunStatus(input.gunId) != cachedGunStatus(input.gunId)
         AND userNavigatesToDetailPage(input.gunId)
END FUNCTION
```

### Examples

- **示例1**: 充电枪BJ-CY-001-A在列表页显示"充电中"（status=1），用户点击进入详情页面，页面从缓存读取到status=0（空闲），显示"插枪"按钮而不是"结束充电"按钮
- **示例2**: 用户在充电模拟器页面执行"插枪"操作，数据库更新status=1，但返回站点详情页面时，缓存中仍是status=0，显示"插枪"按钮（应禁用）
- **示例3**: 用户执行"拔枪"操作后，数据库更新status=0，但详情页面缓存未更新，仍显示"充电中"状态
- **边缘情况**: 缓存在10分钟后自动过期，此时详情页面会显示正确状态，但用户不应等待10分钟才能看到正确状态

## Expected Behavior

### Preservation Requirements

**Unchanged Behaviors:**
- 列表页面的充电枪状态显示必须继续正确工作（从数据库实时查询）
- 充电操作流程（插枪→启动充电→结束充电→拔枪）的业务逻辑必须保持不变
- WebSocket实时推送充电数据的功能必须继续正常工作
- MQTT消息接收和状态更新的功能必须继续正常工作

**Scope:**
所有不涉及详情页面状态显示的功能应完全不受影响。这包括：
- 充电模拟器页面的按钮状态判断（基于前端sessionId状态）
- 充电数据的实时推送和显示
- 站点列表页面的显示和查询
- 其他缓存数据（站点详情、站点列表）的读取

## Hypothesized Root Cause

基于代码分析，最可能的问题是：

1. **缓存未失效**: ChargingService在更新充电枪状态后，没有清除StationService中的Caffeine缓存（键为"pile:detail:{pileId}"）
   - insertGun()方法更新gun.status=1后，未清除缓存
   - removeGun()方法更新gun.status=0后，未清除缓存
   - 缓存过期时间为10分钟，导致长时间显示错误状态

2. **服务间依赖**: ChargingService和StationService是独立的服务类，ChargingService不知道需要清除StationService的缓存

3. **缓存键设计**: 缓存键为"pile:detail:{pileId}"，包含充电桩的所有充电枪信息，当任一充电枪状态变化时，整个缓存应失效

4. **缺少重置功能**: 系统没有提供重置充电枪状态的API，当充电枪状态异常时，无法手动恢复

## Correctness Properties

Property 1: Fault Condition - 详情页面状态一致性

_For any_ 充电操作（插枪、启动充电、结束充电、拔枪）执行后，当用户进入充电枪详情页面时，系统SHALL显示与数据库中实时状态一致的充电枪状态和按钮状态，不受Caffeine缓存影响。

**Validates: Requirements 2.1, 2.2, 2.3, 2.4**

Property 2: Preservation - 非详情页面功能保持不变

_For any_ 不涉及详情页面状态显示的操作（列表页面查询、充电数据推送、MQTT消息处理），系统SHALL产生与原代码完全相同的行为，保持所有现有功能正常工作。

**Validates: Requirements 3.1, 3.2, 3.3, 3.4**

## Fix Implementation

### Changes Required

假设我们的根本原因分析正确：

**File**: `src/main/java/chat/wisechat/charging/service/ChargingService.java`

**Function**: `insertGun`, `removeGun`

**Specific Changes**:
1. **注入StationService依赖**: 在ChargingService中添加StationService的依赖注入
   - 添加 `@Autowired private StationService stationService;`

2. **insertGun方法中清除缓存**: 在更新充电枪状态后，清除对应充电桩的详情缓存
   - 在 `gunMapper.updateById(gun);` 之后
   - 调用 `stationService.clearPileDetailCache(gun.getPileId());`

3. **removeGun方法中清除缓存**: 在恢复充电枪状态后，清除对应充电桩的详情缓存
   - 在 `gunMapper.updateById(gun);` 之后
   - 调用 `stationService.clearPileDetailCache(gun.getPileId());`

4. **StationService添加清除缓存方法**: 提供公共方法供其他服务清除缓存
   - 添加方法 `public void clearPileDetailCache(Long pileId)`
   - 实现：`cache.invalidate("pile:detail:" + pileId);`

5. **添加重置功能**: 在ChargingService中添加重置充电枪状态的方法
   - 添加方法 `public void resetGun(Long gunId)`
   - 检查充电枪是否存在活跃会话，如果有则抛出异常
   - 将充电枪状态重置为0（空闲）
   - 清除相关Redis缓存（session:status:*, gun:session:*, vehicle:session:*）
   - 清除Caffeine缓存

**File**: `src/main/java/chat/wisechat/charging/service/StationService.java`

**Function**: 新增方法

**Specific Changes**:
1. **添加清除缓存方法**: 提供公共方法供其他服务清除特定充电桩的详情缓存
   ```java
   public void clearPileDetailCache(Long pileId) {
       String cacheKey = "pile:detail:" + pileId;
       cache.invalidate(cacheKey);
       log.info("清除充电桩详情缓存: {}", pileId);
   }
   ```

**File**: `src/main/java/chat/wisechat/charging/controller/ChargingController.java`

**Function**: 新增API端点

**Specific Changes**:
1. **添加重置API**: 添加POST /api/charging/reset端点
   - 接收参数：gunId
   - 调用 `chargingService.resetGun(gunId)`
   - 返回成功或失败结果

**File**: `src/main/resources/templates/charging-simulator.html`

**Function**: 添加重置按钮

**Specific Changes**:
1. **添加重置按钮**: 在控制按钮区域添加"重置"按钮
   - 按钮ID: `btn-reset-gun`
   - 样式类: `btn btn-reset`
   - 默认启用状态

2. **添加重置按钮事件处理**: 实现resetGun()函数
   - 调用 `/api/charging/reset` API
   - 成功后重置所有按钮状态和数据显示

## Testing Strategy

### Validation Approach

测试策略采用两阶段方法：首先在未修复的代码上演示bug，确认根本原因分析正确；然后验证修复后的代码能正确处理所有场景并保持现有功能不变。

### Exploratory Fault Condition Checking

**Goal**: 在实施修复之前演示bug。确认或反驳根本原因分析。如果反驳，需要重新假设。

**Test Plan**: 编写测试用例模拟充电操作后访问详情页面的场景。在未修复的代码上运行这些测试，观察缓存导致的状态不一致问题。

**Test Cases**:
1. **插枪后访问详情页面**: 执行插枪操作，立即查询充电桩详情，验证缓存中的状态是否过期（未修复代码将失败）
2. **启动充电后访问详情页面**: 执行启动充电操作，立即查询充电桩详情，验证状态是否为"充电中"（未修复代码将失败）
3. **拔枪后访问详情页面**: 执行拔枪操作，立即查询充电桩详情，验证状态是否恢复为"空闲"（未修复代码将失败）
4. **缓存过期后访问详情页面**: 执行充电操作，等待10分钟后查询详情，验证状态是否正确（未修复代码将成功，但不应依赖自动过期）

**Expected Counterexamples**:
- 充电操作后立即查询详情页面，返回的充电枪状态与数据库不一致
- 可能原因：Caffeine缓存未失效，缓存键为"pile:detail:{pileId}"，过期时间10分钟

### Fix Checking

**Goal**: 验证对于所有触发bug条件的输入，修复后的函数产生预期行为。

**Pseudocode:**
```
FOR ALL input WHERE isBugCondition(input) DO
  result := performChargingOperation_fixed(input)
  detailPage := getPileDetail_fixed(input.pileId)
  ASSERT detailPage.gunStatus == databaseGunStatus(input.gunId)
END FOR
```

### Preservation Checking

**Goal**: 验证对于所有不触发bug条件的输入，修复后的函数产生与原函数相同的结果。

**Pseudocode:**
```
FOR ALL input WHERE NOT isBugCondition(input) DO
  ASSERT originalFunction(input) = fixedFunction(input)
END FOR
```

**Testing Approach**: 推荐使用基于属性的测试进行保持性检查，因为：
- 它自动生成跨输入域的多个测试用例
- 它能捕获手动单元测试可能遗漏的边缘情况
- 它为所有非bug输入提供强有力的保证，确保行为不变

**Test Plan**: 首先在未修复的代码上观察非充电操作的行为，然后编写基于属性的测试捕获该行为。

**Test Cases**:
1. **列表页面状态显示保持**: 观察未修复代码中列表页面正确显示状态，编写测试验证修复后继续正确显示
2. **充电操作流程保持**: 观察未修复代码中充电操作流程正常工作，编写测试验证修复后流程不变
3. **WebSocket推送保持**: 观察未修复代码中WebSocket正常推送数据，编写测试验证修复后推送功能不变
4. **其他缓存数据保持**: 观察未修复代码中站点列表等其他缓存数据正常工作，编写测试验证修复后不受影响

### Unit Tests

- 测试ChargingService.insertGun()方法在更新状态后清除缓存
- 测试ChargingService.removeGun()方法在恢复状态后清除缓存
- 测试ChargingService.resetGun()方法正确重置状态并清除所有相关缓存
- 测试StationService.clearPileDetailCache()方法正确清除指定缓存
- 测试边缘情况：充电枪不存在、充电桩不存在、有活跃会话时尝试重置

### Property-Based Tests

- 生成随机充电操作序列，验证每次操作后详情页面状态与数据库一致
- 生成随机充电桩配置，验证缓存清除不影响其他充电桩的缓存
- 测试跨多个场景的保持性：列表查询、充电数据推送、MQTT消息处理

### Integration Tests

- 测试完整充电流程：插枪→查询详情→启动充电→查询详情→结束充电→查询详情→拔枪→查询详情
- 测试多充电枪场景：同一充电桩的多个充电枪同时操作，验证缓存清除的正确性
- 测试重置功能：在不同状态下重置充电枪，验证状态恢复和缓存清除
- 测试前端集成：在充电模拟器页面执行操作，返回详情页面验证按钮状态正确
