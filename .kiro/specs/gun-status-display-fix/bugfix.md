# 充电枪详情页面状态显示错误修复

## Introduction

充电枪BJ-CY-001-A在列表页面显示"充电中"状态（正确），但进入详情页面时显示"插枪"按钮（错误）。详情页面的按钮状态判断逻辑没有正确读取充电枪的当前状态，导致用户看到不一致的状态信息和错误的操作按钮。此外，需要添加重置功能来将枪状态重置为空闲。

## Bug Analysis

### Current Behavior (Defect)

1.1 WHEN 充电枪在列表页显示"充电中"状态 AND 用户进入该枪的详情页面 THEN 详情页面显示"插枪"按钮而不是"结束充电"按钮

1.2 WHEN 详情页面加载时 THEN 系统未能正确从Redis或数据库读取充电枪的实时状态

1.3 WHEN 用户需要重置充电枪状态时 THEN 系统没有提供重置功能

### Expected Behavior (Correct)

2.1 WHEN 充电枪状态为"充电中" AND 用户进入详情页面 THEN 系统SHALL显示"结束充电"按钮，并禁用"插枪"、"启动充电"、"拔枪"按钮

2.2 WHEN 充电枪状态为"空闲" AND 用户进入详情页面 THEN 系统SHALL显示"插枪"按钮

2.3 WHEN 充电枪状态为"已插枪" AND 用户进入详情页面 THEN 系统SHALL显示"启动充电"和"拔枪"按钮

2.4 WHEN 详情页面加载时 THEN 系统SHALL正确从Redis或数据库读取充电枪的实时状态

2.5 WHEN 用户点击"重置"按钮 THEN 系统SHALL将充电枪状态重置为空闲，并清除相关缓存数据

### Unchanged Behavior (Regression Prevention)

3.1 WHEN 充电枪在列表页面显示状态 THEN 系统SHALL CONTINUE TO正确显示充电枪的实时状态

3.2 WHEN 用户在详情页面执行正常操作（插枪、启动充电、结束充电、拔枪）THEN 系统SHALL CONTINUE TO正确处理这些操作并更新状态

3.3 WHEN 充电枪状态发生变化 THEN 系统SHALL CONTINUE TO通过WebSocket实时推送状态更新到前端

3.4 WHEN 系统从MQTT接收到充电枪状态消息 THEN 系统SHALL CONTINUE TO正确解析并更新状态
