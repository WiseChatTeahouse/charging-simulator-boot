# 实施计划

- [x] 1. 编写故障条件探索测试
  - **Property 1: Fault Condition** - 详情页面状态一致性
  - **关键**: 此测试必须在未修复的代码上失败 - 失败确认bug存在
  - **不要在测试失败时尝试修复测试或代码**
  - **注意**: 此测试编码了预期行为 - 在实施修复后测试通过时将验证修复
  - **目标**: 展示证明bug存在的反例
  - **范围化PBT方法**: 对于确定性bug，将属性范围限定为具体失败案例以确保可重现性
  - 测试实现来自设计文档中故障条件的详细信息
  - 测试断言应匹配设计文档中的预期行为属性
  - 在未修复的代码上运行测试
  - **预期结果**: 测试失败（这是正确的 - 证明bug存在）
  - 记录发现的反例以理解根本原因
  - 当测试编写完成、运行并记录失败时标记任务完成
  - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 2. 编写保持性属性测试（在实施修复之前）
  - **Property 2: Preservation** - 非详情页面功能保持不变
  - **重要**: 遵循观察优先方法
  - 在未修复的代码上观察非bug输入的行为
  - 编写基于属性的测试，捕获来自保持性需求的观察行为模式
  - 基于属性的测试生成许多测试用例以提供更强保证
  - 在未修复的代码上运行测试
  - **预期结果**: 测试通过（这确认了要保持的基线行为）
  - 当测试编写完成、运行并在未修复代码上通过时标记任务完成
  - _Requirements: 3.1, 3.2, 3.3, 3.4_

- [x] 3. 修复充电枪详情页面状态显示错误

  - [x] 3.1 在StationService中添加清除缓存方法
    - 在StationService类中添加公共方法clearPileDetailCache(Long pileId)
    - 实现缓存失效逻辑：cache.invalidate("pile:detail:" + pileId)
    - 添加日志记录缓存清除操作
    - _Bug_Condition: isBugCondition(input) where operation IN ['insertGun', 'startCharging', 'stopCharging', 'removeGun'] AND databaseGunStatus != cachedGunStatus_
    - _Expected_Behavior: 详情页面显示的充电枪状态和按钮状态应与数据库中的实时状态一致_
    - _Preservation: 列表页面状态显示、充电操作流程、WebSocket推送等现有功能保持不变_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4_

  - [x] 3.2 在ChargingService中注入StationService依赖
    - 添加@Autowired注解注入StationService
    - 确保依赖注入正确配置
    - _Bug_Condition: isBugCondition(input) where operation IN ['insertGun', 'startCharging', 'stopCharging', 'removeGun'] AND databaseGunStatus != cachedGunStatus_
    - _Expected_Behavior: 详情页面显示的充电枪状态和按钮状态应与数据库中的实时状态一致_
    - _Preservation: 列表页面状态显示、充电操作流程、WebSocket推送等现有功能保持不变_
    - _Requirements: 2.1, 2.2, 2.3, 2.4, 3.1, 3.2, 3.3, 3.4_

  - [x] 3.3 在insertGun方法中清除缓存
    - 在gunMapper.updateById(gun)之后调用stationService.clearPileDetailCache(gun.getPileId())
    - 确保缓存清除在状态更新之后执行
    - _Bug_Condition: isBugCondition(input) where operation = 'insertGun' AND databaseGunStatus != cachedGunStatus_
    - _Expected_Behavior: 插枪后详情页面应显示"启动充电"和"拔枪"按钮_
    - _Preservation: 插枪操作的业务逻辑保持不变_
    - _Requirements: 2.3, 2.4, 3.2_

  - [x] 3.4 在removeGun方法中清除缓存
    - 在gunMapper.updateById(gun)之后调用stationService.clearPileDetailCache(gun.getPileId())
    - 确保缓存清除在状态恢复之后执行
    - _Bug_Condition: isBugCondition(input) where operation = 'removeGun' AND databaseGunStatus != cachedGunStatus_
    - _Expected_Behavior: 拔枪后详情页面应显示"插枪"按钮_
    - _Preservation: 拔枪操作的业务逻辑保持不变_
    - _Requirements: 2.2, 2.4, 3.2_

  - [x] 3.5 添加重置充电枪状态功能
    - 在ChargingService中添加resetGun(Long gunId)方法
    - 检查充电枪是否存在活跃会话，如果有则抛出异常
    - 将充电枪状态重置为0（空闲）
    - 清除相关Redis缓存（session:status:*, gun:session:*, vehicle:session:*）
    - 调用stationService.clearPileDetailCache清除Caffeine缓存
    - _Bug_Condition: 充电枪状态异常且无法通过正常流程恢复_
    - _Expected_Behavior: 重置后充电枪状态应为空闲，详情页面显示"插枪"按钮_
    - _Preservation: 正常充电操作流程不受影响_
    - _Requirements: 2.2, 2.4, 2.5_

  - [x] 3.6 添加重置API端点
    - 在ChargingController中添加POST /api/charging/reset端点
    - 接收参数gunId
    - 调用chargingService.resetGun(gunId)
    - 返回成功或失败结果
    - 添加适当的错误处理和日志记录
    - _Bug_Condition: 用户需要手动重置充电枪状态_
    - _Expected_Behavior: API调用成功后充电枪状态重置为空闲_
    - _Preservation: 其他API端点功能保持不变_
    - _Requirements: 2.5_

  - [x] 3.7 在前端添加重置按钮
    - 在charging-simulator.html的控制按钮区域添加"重置"按钮
    - 按钮ID: btn-reset-gun，样式类: btn btn-reset
    - 实现resetGun()函数调用/api/charging/reset API
    - 成功后重置所有按钮状态和数据显示
    - 添加适当的错误提示和确认对话框
    - _Bug_Condition: 用户需要通过UI重置充电枪状态_
    - _Expected_Behavior: 点击重置按钮后充电枪状态恢复为空闲_
    - _Preservation: 其他按钮功能保持不变_
    - _Requirements: 2.5_

  - [x] 3.8 验证故障条件探索测试现在通过
    - **Property 1: Expected Behavior** - 详情页面状态一致性
    - **重要**: 重新运行任务1中的相同测试 - 不要编写新测试
    - 任务1中的测试编码了预期行为
    - 当此测试通过时，确认预期行为得到满足
    - 运行步骤1中的故障条件探索测试
    - **预期结果**: 测试通过（确认bug已修复）
    - _Requirements: 设计文档中的预期行为属性_

  - [x] 3.9 验证保持性测试仍然通过
    - **Property 2: Preservation** - 非详情页面功能保持不变
    - **重要**: 重新运行任务2中的相同测试 - 不要编写新测试
    - 运行步骤2中的保持性属性测试
    - **预期结果**: 测试通过（确认无回归）
    - 确认修复后所有测试仍然通过（无回归）

- [x] 4. 检查点 - 确保所有测试通过
  - 确保所有测试通过，如有问题请询问用户。
