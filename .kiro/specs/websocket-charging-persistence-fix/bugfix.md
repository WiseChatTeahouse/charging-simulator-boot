# Bugfix Requirements Document

## Introduction

当用户在充电过程中离开充电枪详情页面（关闭标签页或导航到其他页面）时，充电会话会被意外中断。这是因为当前实现中充电会话的生命周期错误地与WebSocket连接绑定在一起。

根本原因：
- WebSocket连接仅用于实时数据推送，不应影响充电会话的持续性
- 充电会话应该独立于前端页面状态持续进行
- 用户应该能够在离开页面后返回并恢复到正在充电的状态

影响范围：
- 用户无法在充电过程中自由导航或关闭页面
- 充电会话被意外中断，影响用户体验
- 用户返回页面时无法恢复充电状态

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN 用户在充电过程中关闭充电枪详情页面标签页 THEN 系统错误地中断正在进行的充电会话

1.2 WHEN 用户在充电过程中导航到其他页面 THEN 系统错误地中断正在进行的充电会话

1.3 WHEN WebSocket连接因网络问题断开 THEN 系统错误地中断正在进行的充电会话

1.4 WHEN 用户在充电会话中断后返回充电枪详情页面 THEN 系统无法恢复到正在充电的状态

### Expected Behavior (Correct)

2.1 WHEN 用户在充电过程中关闭充电枪详情页面标签页 THEN 系统SHALL继续充电会话而不中断

2.2 WHEN 用户在充电过程中导航到其他页面 THEN 系统SHALL继续充电会话而不中断

2.3 WHEN WebSocket连接因网络问题断开 THEN 系统SHALL继续充电会话而不中断

2.4 WHEN 用户在充电会话进行中返回充电枪详情页面 THEN 系统SHALL自动恢复WebSocket连接并显示当前充电状态

2.5 WHEN 用户返回充电枪详情页面且充电会话已结束 THEN 系统SHALL显示充电已结束的状态

### Unchanged Behavior (Regression Prevention)

3.1 WHEN 用户主动点击"结束充电"按钮 THEN 系统SHALL CONTINUE TO正常结束充电会话

3.2 WHEN 用户在充电结束后拔枪 THEN 系统SHALL CONTINUE TO正常清理会话状态和恢复充电枪为空闲状态

3.3 WHEN 用户使用重置功能 THEN 系统SHALL CONTINUE TO强制结束所有活跃会话并重置充电枪状态

3.4 WHEN WebSocket连接正常建立 THEN 系统SHALL CONTINUE TO实时推送充电数据和BMS数据

3.5 WHEN 充电会话不存在时用户尝试操作 THEN 系统SHALL CONTINUE TO返回适当的错误提示

3.6 WHEN 充电枪状态为空闲 THEN 系统SHALL CONTINUE TO只允许插枪操作

3.7 WHEN 充电枪状态为充电中 THEN 系统SHALL CONTINUE TO只允许结束充电操作
