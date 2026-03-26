<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>充电模拟器</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        ::-webkit-scrollbar {
            width: 8px;
            height: 8px;
        }
        
        ::-webkit-scrollbar-track {
            background: rgba(15, 52, 96, 0.3);
            border-radius: 4px;
        }
        
        ::-webkit-scrollbar-thumb {
            background: linear-gradient(180deg, #00d4ff 0%, #0099cc 100%);
            border-radius: 4px;
            transition: all 0.3s;
        }
        
        ::-webkit-scrollbar-thumb:hover {
            background: linear-gradient(180deg, #00e5ff 0%, #00b8e6 100%);
            box-shadow: 0 0 8px rgba(0, 212, 255, 0.5);
        }
        
        * {
            scrollbar-width: thin;
            scrollbar-color: #00d4ff rgba(15, 52, 96, 0.3);
        }
        
        body {
            font-family: 'Microsoft YaHei', Arial, sans-serif;
            background: #1a1a2e;
            color: #fff;
            height: 100vh;
            overflow: hidden;
        }
        
        .simulator-container {
            display: flex;
            height: 100vh;
            gap: 16px;
            padding: 20px;
            background: #1a1a2e;
            position: relative;
        }
        
        .left-panel, .right-panel {
            flex: 1;
            background: #16213e;
            border-radius: 12px;
            padding: 24px;
            overflow-y: auto;
            position: relative;
        }
        
        .simulator-container::before {
            content: '';
            position: absolute;
            left: 50%;
            top: 50%;
            transform: translate(-50%, -50%);
            height: 85%;
            width: 1px;
            background: linear-gradient(180deg, 
                transparent 0%, 
                rgba(0, 212, 255, 0.2) 5%,
                rgba(0, 212, 255, 0.5) 50%, 
                rgba(0, 212, 255, 0.2) 95%,
                transparent 100%);
            pointer-events: none;
            z-index: 1;
        }
        
        .right-panel.grayed-out {
            opacity: 0.4;
            pointer-events: none;
        }
        
        h2 {
            color: #00d4ff;
            margin-bottom: 20px;
            font-size: 24px;
            border-bottom: 2px solid #00d4ff;
            padding-bottom: 10px;
        }
        
        .gun-info {
            background: #0f3460;
            padding: 16px;
            border-radius: 8px;
            margin-bottom: 24px;
        }
        
        .info-item {
            display: flex;
            justify-content: space-between;
            margin-bottom: 8px;
            font-size: 14px;
        }
        
        .info-label { color: #aaa; }
        .info-value { color: #fff; font-weight: bold; }
        
        .control-buttons {
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 10px;
            margin-bottom: 20px;
        }
        
        .btn {
            padding: 10px 12px;
            border: none;
            border-radius: 8px;
            font-size: 14px;
            font-weight: bold;
            cursor: pointer;
            transition: all 0.3s;
            text-transform: uppercase;
        }
        
        .btn:disabled { opacity: 0.5; cursor: not-allowed; }
        
        .btn-insert { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; }
        .btn-insert:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(102, 126, 234, 0.4); }
        
        .btn-start { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); color: white; }
        .btn-start:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(245, 87, 108, 0.4); }
        
        .btn-stop { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); color: white; }
        .btn-stop:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(250, 112, 154, 0.4); }
        
        .btn-remove { background: linear-gradient(135deg, #30cfd0 0%, #330867 100%); color: white; }
        .btn-remove:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(48, 207, 208, 0.4); }
        
        .btn-reset { background: linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%); color: white; }
        .btn-reset:hover:not(:disabled) { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(255, 107, 107, 0.4); }
        
        .data-display {
            background: #0f3460;
            padding: 16px;
            border-radius: 8px;
        }
        
        .data-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 10px 0;
            border-bottom: 1px solid rgba(255,255,255,0.1);
        }
        
        .data-item:last-child { border-bottom: none; }
        .data-label { color: #aaa; font-size: 13px; }
        .data-value { font-size: 20px; font-weight: bold; color: #00d4ff; }
        .data-unit { font-size: 12px; color: #aaa; margin-left: 4px; }
        
        .back-link {
            display: inline-block;
            color: #00d4ff;
            text-decoration: none;
            margin-bottom: 16px;
            transition: color 0.3s;
        }
        
        .back-link:hover { color: #00a8cc; }
        
        .status-message {
            background: #0f3460;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 16px;
            text-align: center;
            font-size: 14px;
            color: #00d4ff;
        }
        
        .bms-container {
            display: flex;
            align-items: flex-start;
            justify-content: flex-start;
            background: #0f3460;
            padding: 20px;
            border-radius: 12px;
            gap: 20px;
        }
        
        .bms-left {
            flex: 1;
            display: grid;
            grid-template-columns: repeat(2, 1fr);
            gap: 12px;
        }
        
        .bms-data-item { display: flex; flex-direction: column; gap: 4px; }
        .bms-label { color: #aaa; font-size: 11px; text-transform: uppercase; letter-spacing: 1px; }
        .bms-value { font-size: 18px; font-weight: bold; color: #00d4ff; }
        .bms-unit { font-size: 12px; color: #aaa; margin-left: 3px; }
        
        .battery-container {
            flex: 0 0 auto;
            display: flex;
            flex-direction: column;
            align-items: center;
            justify-content: center;
        }
        
        .battery-pole {
            width: 40px;
            height: 15px;
            background: linear-gradient(180deg, #555 0%, #333 100%);
            border-radius: 8px 8px 0 0;
            position: relative;
        }
        
        .battery-pole::before {
            content: '';
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            width: 20px;
            height: 8px;
            background: #222;
            border-radius: 2px;
        }
        
        .battery-body {
            width: 80px;
            height: 200px;
            background: linear-gradient(135deg, #1a2332 0%, #0f1923 100%);
            border: 3px solid #00d4ff;
            border-radius: 12px;
            position: relative;
            overflow: hidden;
            box-shadow: 0 0 20px rgba(0, 212, 255, 0.3), inset 0 0 20px rgba(0, 0, 0, 0.5);
        }
        
        .battery-level {
            position: absolute;
            bottom: 0;
            left: 0;
            right: 0;
            height: 0%;
            background: linear-gradient(180deg, #00ff88 0%, #00d4ff 50%, #0099cc 100%);
            border-radius: 0 0 8px 8px;
            overflow: hidden;
        }
        
        .battery-level.low { background: linear-gradient(180deg, #ff6b6b 0%, #ee5a6f 100%); }
        .battery-level.medium { background: linear-gradient(180deg, #ffd93d 0%, #f5a623 100%); }
        
        .battery-liquid {
            position: absolute;
            top: -10px;
            left: -50%;
            width: 200%;
            height: 20px;
            background: rgba(255, 255, 255, 0.3);
            border-radius: 45%;
            animation: wave 3s ease-in-out infinite;
        }
        
        .battery-liquid::before, .battery-liquid::after {
            content: '';
            position: absolute;
            width: 100%;
            height: 100%;
            top: 0;
            left: 0;
            background: rgba(255, 255, 255, 0.2);
            border-radius: 45%;
        }
        
        .battery-liquid::before { animation: wave 4s ease-in-out infinite; }
        .battery-liquid::after { animation: wave 5s ease-in-out infinite reverse; }
        
        @keyframes wave {
            0%, 100% { transform: translateX(0) translateY(0); }
            25% { transform: translateX(-25%) translateY(-5px); }
            50% { transform: translateX(0) translateY(0); }
            75% { transform: translateX(25%) translateY(-5px); }
        }
        
        .battery-percentage {
            position: absolute;
            top: 50%;
            left: 50%;
            transform: translate(-50%, -50%);
            font-size: 28px;
            font-weight: bold;
            color: #fff;
            text-shadow: 0 0 10px rgba(0, 0, 0, 0.8), 0 0 20px rgba(0, 212, 255, 0.5);
            z-index: 10;
        }
        
        .custom-modal {
            display: none;
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.8);
            backdrop-filter: blur(8px);
            z-index: 9999;
            animation: fadeIn 0.3s ease;
        }
        
        .custom-modal.show {
            display: flex;
            align-items: center;
            justify-content: center;
        }
        
        @keyframes fadeIn {
            from { opacity: 0; }
            to { opacity: 1; }
        }
        
        .modal-content {
            background: rgba(22, 33, 62, 0.95);
            border: 2px solid rgba(0, 212, 255, 0.6);
            border-radius: 16px;
            padding: 40px 32px 32px;
            max-width: 400px;
            width: 90%;
            box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5), 0 0 30px rgba(0, 212, 255, 0.2);
            animation: slideIn 0.3s ease;
            position: relative;
            backdrop-filter: blur(10px);
        }
        
        @keyframes slideIn {
            from { transform: translateY(-50px); opacity: 0; }
            to { transform: translateY(0); opacity: 1; }
        }
        
        .modal-title { color: #00d4ff; font-size: 22px; font-weight: bold; text-align: center; margin-bottom: 20px; }
        .modal-message { color: rgba(170, 170, 170, 0.9); font-size: 15px; text-align: center; line-height: 1.8; margin-bottom: 32px; }
        .modal-buttons { display: flex; gap: 12px; }
        
        .modal-btn {
            flex: 1;
            padding: 12px;
            border: none;
            border-radius: 8px;
            font-size: 15px;
            font-weight: bold;
            cursor: pointer;
            transition: all 0.3s;
        }
        
        .modal-btn-cancel {
            background: rgba(42, 63, 95, 0.8);
            color: #aaa;
            border: 1px solid rgba(255, 255, 255, 0.1);
        }
        
        .modal-btn-cancel:hover { background: rgba(58, 79, 111, 0.9); color: #fff; border-color: rgba(255, 255, 255, 0.2); }
        
        .modal-btn-confirm {
            background: linear-gradient(135deg, rgba(0, 212, 255, 0.9) 0%, rgba(0, 153, 204, 0.9) 100%);
            color: #fff;
        }
        
        .modal-btn-confirm:hover {
            background: linear-gradient(135deg, #00d4ff 0%, #0099cc 100%);
            transform: translateY(-2px);
            box-shadow: 0 4px 15px rgba(0, 212, 255, 0.4);
        }
    </style>
</head>
<body>
    <div id="custom-modal" class="custom-modal">
        <div class="modal-content">
            <div class="modal-title">确认重置</div>
            <div class="modal-message">确定要重置充电枪状态吗？<br>这将清除所有相关数据。</div>
            <div class="modal-buttons">
                <button class="modal-btn modal-btn-cancel" onclick="closeModal()">取消</button>
                <button class="modal-btn modal-btn-confirm" onclick="confirmReset()">确定</button>
            </div>
        </div>
    </div>
    
    <div class="simulator-container">
        <div class="left-panel">
            <a href="javascript:history.back()" class="back-link">← 返回</a>
            
            <h2>充电枪信息</h2>
            <div class="gun-info" id="gun-info">
                <div class="info-item">
                    <span class="info-label">充电枪编号:</span>
                    <span class="info-value" id="gun-code">--</span>
                </div>
                <div class="info-item">
                    <span class="info-label">充电枪类型:</span>
                    <span class="info-value" id="gun-type">--</span>
                </div>
                <div class="info-item">
                    <span class="info-label">充电枪状态:</span>
                    <span class="info-value" id="gun-status">--</span>
                </div>
            </div>
            
            <div id="status-message" class="status-message" style="display: none;"></div>
            
            <h2>充电控制</h2>
            <div class="control-buttons">
                <button id="btn-insert-gun" class="btn btn-insert">插枪</button>
                <button id="btn-start-charging" class="btn btn-start" disabled>启动充电</button>
                <button id="btn-stop-charging" class="btn btn-stop" disabled>结束充电</button>
                <button id="btn-remove-gun" class="btn btn-remove" disabled>拔枪</button>
            </div>
            
            <button id="btn-reset-gun" class="btn btn-reset" style="width: 100%; margin-bottom: 24px;">重置</button>
            
            <h2>充电数据</h2>
            <div id="charging-data" class="data-display">
                <div class="data-item">
                    <span class="data-label">电压</span>
                    <span><span class="data-value" id="voltage">--</span><span class="data-unit">V</span></span>
                </div>
                <div class="data-item">
                    <span class="data-label">电流</span>
                    <span><span class="data-value" id="current">--</span><span class="data-unit">A</span></span>
                </div>
                <div class="data-item">
                    <span class="data-label">功率</span>
                    <span><span class="data-value" id="power">--</span><span class="data-unit">kW</span></span>
                </div>
                <div class="data-item">
                    <span class="data-label">SOC</span>
                    <span><span class="data-value" id="soc">--</span><span class="data-unit">%</span></span>
                </div>
                <div class="data-item">
                    <span class="data-label">充电电量</span>
                    <span><span class="data-value" id="energy">--</span><span class="data-unit">kWh</span></span>
                </div>
            </div>
        </div>
        
        <div class="right-panel grayed-out" id="vehicle-panel">
            <h2>车辆 BMS 数据</h2>
            <div id="bms-data" class="bms-container">
                <div class="bms-left">
                    <div class="bms-data-item">
                        <span class="bms-label">SOC</span>
                        <span class="bms-value"><span id="battery-soc">--</span><span class="bms-unit">%</span></span>
                    </div>
                    <div class="bms-data-item">
                        <span class="bms-label">预估剩余时间</span>
                        <span class="bms-value"><span id="bms-remain-time">--</span><span class="bms-unit">min</span></span>
                    </div>
                    <div class="bms-data-item">
                        <span class="bms-label">电池组最低温度</span>
                        <span class="bms-value"><span id="bms-min-temp">--</span><span class="bms-unit">°C</span></span>
                    </div>
                    <div class="bms-data-item">
                        <span class="bms-label">电池组最高温度</span>
                        <span class="bms-value"><span id="bms-max-temp">--</span><span class="bms-unit">°C</span></span>
                    </div>
                    <div class="bms-data-item">
                        <span class="bms-label">需求电压</span>
                        <span class="bms-value"><span id="bms-r-vol">--</span><span class="bms-unit">V</span></span>
                    </div>
                    <div class="bms-data-item">
                        <span class="bms-label">需求电流</span>
                        <span class="bms-value"><span id="bms-r-cur">--</span><span class="bms-unit">A</span></span>
                    </div>
                    <div class="bms-data-item">
                        <span class="bms-label">测量电压</span>
                        <span class="bms-value"><span id="bms-m-vol">--</span><span class="bms-unit">V</span></span>
                    </div>
                    <div class="bms-data-item">
                        <span class="bms-label">测量电流</span>
                        <span class="bms-value"><span id="bms-m-cur">--</span><span class="bms-unit">A</span></span>
                    </div>
                </div>
                <div class="battery-container">
                    <div class="battery-pole"></div>
                    <div class="battery-body">
                        <div class="battery-level" id="battery-level">
                            <div class="battery-liquid"></div>
                        </div>
                        <div class="battery-percentage" id="battery-percentage">--</div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <script>
        const gunId = ${gunId};
        let sessionId = null;
        let ws = null;
        let chargingDataTimer = null;
        let bmsDataTimer = null;
        let isCharging = false;
        const DATA_TIMEOUT = 20000;
        
        window.onload = function() {
            initButtons();
            checkAndRestoreActiveSession();
        };
        
        function checkAndRestoreActiveSession() {
            fetch('/api/charging/active-session?gunId=' + gunId)
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200 && result.data) {
                        restoreSessionState(result.data);
                    } else {
                        loadGunInfo();
                    }
                })
                .catch(error => {
                    console.error('查询活跃会话失败:', error);
                    loadGunInfo();
                });
        }
        
        function restoreSessionState(sessionData) {
            sessionId = sessionData.gunId;
            loadGunBasicInfo();
            const status = sessionData.status;
            document.getElementById('gun-status').textContent = sessionData.statusText;
            
            if (status === 1) {
                updateButtonsByGunStatus(1);
                showStatus('检测到已插枪状态，枪ID: ' + sessionId);
            } else if (status === 2) {
                isCharging = true;
                updateButtonsByGunStatus(2);
                document.getElementById('vehicle-panel').classList.remove('grayed-out');
                showStatus('检测到充电中状态，正在恢复连接...');
                connectWebSocket();
            }
        }
        
        function loadGunBasicInfo() {
            fetch('/api/piles/by-gun/' + gunId)
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200) {
                        const gun = result.data.guns.find(g => g.id == gunId);
                        if (gun) {
                            document.getElementById('gun-code').textContent = gun.gunCode;
                            document.getElementById('gun-type').textContent = gun.typeText;
                        }
                    }
                })
                .catch(() => {
                    document.getElementById('gun-code').textContent = 'GUN-' + gunId;
                    document.getElementById('gun-type').textContent = '快充';
                });
        }
        
        function loadGunInfo() {
            fetch('/api/piles/by-gun/' + gunId)
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200) {
                        const gun = result.data.guns.find(g => g.id == gunId);
                        if (gun) {
                            document.getElementById('gun-code').textContent = gun.gunCode;
                            document.getElementById('gun-type').textContent = gun.typeText;
                            document.getElementById('gun-status').textContent = gun.statusText;
                            updateButtonsByGunStatus(gun.status);
                        }
                    }
                })
                .catch(() => {
                    document.getElementById('gun-code').textContent = 'GUN-' + gunId;
                    document.getElementById('gun-type').textContent = '快充';
                    document.getElementById('gun-status').textContent = '未知';
                });
        }
        
        function updateButtonsByGunStatus(status) {
            const btnInsert = document.getElementById('btn-insert-gun');
            const btnStart = document.getElementById('btn-start-charging');
            const btnStop = document.getElementById('btn-stop-charging');
            const btnRemove = document.getElementById('btn-remove-gun');
            
            btnInsert.disabled = status !== 0;
            btnStart.disabled = status !== 1;
            btnStop.disabled = status !== 2;
            btnRemove.disabled = status !== 1;
            
            if (status === 1) showStatus('充电枪已插枪，可以启动充电');
            if (status === 2) showStatus('充电枪正在充电中');
        }
        
        function initButtons() {
            document.getElementById('btn-insert-gun').addEventListener('click', insertGun);
            document.getElementById('btn-start-charging').addEventListener('click', startCharging);
            document.getElementById('btn-stop-charging').addEventListener('click', stopCharging);
            document.getElementById('btn-remove-gun').addEventListener('click', removeGun);
            document.getElementById('btn-reset-gun').addEventListener('click', resetGun);
        }
        
        function insertGun() {
            showStatus('正在插枪...');
            fetch('/api/charging/insert-gun', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ gunId: parseInt(gunId), vehicleId: 'TEST_VEHICLE_001' })
            })
            .then(response => response.json())
            .then(result => {
                if (result.code === 200) {
                    sessionId = result.data;
                    showStatus('插枪成功！枪ID: ' + sessionId);
                    updateButtonsByGunStatus(1);
                    document.getElementById('gun-status').textContent = '已插枪';
                } else {
                    showStatus('插枪失败: ' + result.message, true);
                }
            })
            .catch(() => showStatus('插枪失败，请稍后重试', true));
        }
        
        function startCharging() {
            if (!sessionId) { showStatus('请先插枪', true); return; }
            showStatus('正在启动充电...');
            fetch('/api/charging/start', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ gunId: gunId })
            })
            .then(response => response.json())
            .then(result => {
                if (result.code === 200) {
                    showStatus('充电已启动');
                    isCharging = true;
                    updateButtonsByGunStatus(2);
                    document.getElementById('gun-status').textContent = '充电中';
                    document.getElementById('vehicle-panel').classList.remove('grayed-out');
                    connectWebSocket();
                } else {
                    showStatus('启动充电失败: ' + result.message, true);
                }
            })
            .catch(() => showStatus('启动充电失败，请稍后重试', true));
        }
        
        function stopCharging() {
            if (!sessionId) { showStatus('没有活跃的充电会话', true); return; }
            showStatus('正在结束充电...');
            fetch('/api/charging/stop', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ gunId: gunId })
            })
            .then(response => response.json())
            .then(result => {
                if (result.code === 200) {
                    showStatus('充电已结束');
                    isCharging = false;
                    updateButtonsByGunStatus(1);
                    document.getElementById('gun-status').textContent = '已插枪';
                } else {
                    showStatus('结束充电失败: ' + result.message, true);
                }
            })
            .catch(() => showStatus('结束充电失败，请稍后重试', true));
        }
        
        function removeGun() {
            if (!sessionId) { showStatus('没有活跃的充电会话', true); return; }
            showStatus('正在拔枪...');
            fetch('/api/charging/remove-gun', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ gunId: gunId })
            })
            .then(response => response.json())
            .then(result => {
                if (result.code === 200) {
                    showStatus('拔枪成功！');
                    isCharging = false;
                    sessionId = null;
                    if (ws) { ws.close(); ws = null; }
                    updateButtonsByGunStatus(0);
                    document.getElementById('gun-status').textContent = '空闲';
                    document.getElementById('vehicle-panel').classList.add('grayed-out');
                    resetDataDisplay();
                } else {
                    showStatus('拔枪失败: ' + result.message, true);
                }
            })
            .catch(() => showStatus('拔枪失败，请稍后重试', true));
        }
        
        function resetGun() { showModal(); }
        function showModal() { document.getElementById('custom-modal').classList.add('show'); }
        function closeModal() { document.getElementById('custom-modal').classList.remove('show'); }
        function confirmReset() { closeModal(); executeReset(); }
        
        function executeReset() {
            showStatus('正在重置充电枪...');
            fetch('/api/charging/reset?gunId=' + gunId, { method: 'POST' })
            .then(response => response.json())
            .then(result => {
                if (result.code === 200) {
                    showStatus('充电枪状态已重置');
                    isCharging = false;
                    if (ws) { ws.close(); ws = null; }
                    sessionId = null;
                    updateButtonsByGunStatus(0);
                    document.getElementById('gun-status').textContent = '空闲';
                    document.getElementById('vehicle-panel').classList.add('grayed-out');
                    resetDataDisplay();
                } else {
                    showStatus('重置失败: ' + result.message, true);
                }
            })
            .catch(() => showStatus('重置失败，请稍后重试', true));
        }
        
        function showStatus(message, isError = false) {
            const el = document.getElementById('status-message');
            el.textContent = message;
            el.style.display = 'block';
            el.style.background = isError ? '#721c24' : '#0f3460';
            el.style.color = isError ? '#f8d7da' : '#00d4ff';
            setTimeout(() => { el.style.display = 'none'; }, 3000);
        }
        
        function resetDataDisplay() {
            if (chargingDataTimer) { clearTimeout(chargingDataTimer); chargingDataTimer = null; }
            if (bmsDataTimer) { clearTimeout(bmsDataTimer); bmsDataTimer = null; }
            ['voltage','current','power','soc','energy','battery-soc','bms-m-vol','bms-m-cur','bms-r-vol','bms-r-cur',
             'bms-max-temp','bms-min-temp','bms-remain-time'].forEach(id => {
                document.getElementById(id).textContent = '--';
            });
        }
        
        function connectWebSocket() {
            if (!sessionId) return;
            const wsUrl = 'ws://' + window.location.host + '/ws/charging/' + gunId;
            ws = new WebSocket(wsUrl);
            
            ws.onopen = () => showStatus('实时数据连接成功');
            ws.onmessage = function(event) {
                try {
                    const message = JSON.parse(event.data);
                    if (message.type === 'CHARGING_DATA') updateChargingData(message.data);
                    else if (message.type === 'BMS_DATA') updateBmsData(message.data);
                } catch (e) { console.error('解析消息失败:', e); }
            };
            ws.onerror = () => showStatus('实时数据连接失败', true);
            ws.onclose = function(event) {
                ws = null;
                if (isCharging && sessionId) {
                    showStatus('连接断开，正在重连...', false);
                    setTimeout(connectWebSocket, 3000);
                }
            };
        }
        
        function updateChargingData(data) {
            if (chargingDataTimer) clearTimeout(chargingDataTimer);
            if (data.voltage != null) document.getElementById('voltage').textContent = data.voltage.toFixed(1);
            if (data.current != null) document.getElementById('current').textContent = data.current.toFixed(1);
            if (data.power != null) document.getElementById('power').textContent = data.power.toFixed(2);
            if (data.soc != null) document.getElementById('soc').textContent = data.soc;
            if (data.energy != null) document.getElementById('energy').textContent = data.energy.toFixed(3);
            chargingDataTimer = setTimeout(() => {
                ['voltage','current','power','soc','energy'].forEach(id => document.getElementById(id).textContent = '--');
            }, DATA_TIMEOUT);
        }
        
        function updateBmsData(data) {
            if (bmsDataTimer) clearTimeout(bmsDataTimer);
            if (data.soc != null) {
                document.getElementById('battery-soc').textContent = data.soc;
                updateBatteryAnimation(data.soc);
            }
            if (data.mVol != null) document.getElementById('bms-m-vol').textContent = Math.abs(data.mVol);
            if (data.mCur != null) document.getElementById('bms-m-cur').textContent = Math.abs(data.mCur);
            if (data.rVol != null) document.getElementById('bms-r-vol').textContent = Math.abs(data.rVol);
            if (data.rCur != null) document.getElementById('bms-r-cur').textContent = Math.abs(data.rCur);
            if (data.maxTemp != null) document.getElementById('bms-max-temp').textContent = data.maxTemp;
            if (data.minTemp != null) document.getElementById('bms-min-temp').textContent = data.minTemp;
            if (data.remainTime != null) document.getElementById('bms-remain-time').textContent = data.remainTime;
            bmsDataTimer = setTimeout(() => {
                ['battery-soc','bms-m-vol','bms-m-cur','bms-r-vol','bms-r-cur',
                 'bms-max-temp','bms-min-temp','bms-remain-time'].forEach(id => {
                    document.getElementById(id).textContent = '--';
                });
                document.getElementById('battery-percentage').textContent = '--';
                document.getElementById('battery-level').style.height = '0%';
            }, DATA_TIMEOUT);
        }
        
        function updateBatteryAnimation(soc) {
            const level = document.getElementById('battery-level');
            const pct = document.getElementById('battery-percentage');
            pct.textContent = soc + '%';
            level.style.transition = 'none';
            level.style.height = '0%';
            level.classList.remove('low', 'medium');
            if (soc <= 20) level.classList.add('low');
            else if (soc <= 50) level.classList.add('medium');
            requestAnimationFrame(() => {
                requestAnimationFrame(() => {
                    level.style.transition = 'height 1s ease-out';
                    level.style.height = soc + '%';
                });
            });
        }
    </script>
</body>
</html>
