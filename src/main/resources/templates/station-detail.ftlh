<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>充电站详情 - 充电模拟器</title>
    <style>
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Microsoft YaHei', Arial, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        
        .container {
            max-width: 1400px;
            margin: 0 auto;
        }
        
        .header {
            background: white;
            border-radius: 12px;
            padding: 24px;
            margin-bottom: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        
        .back-btn {
            display: inline-block;
            padding: 8px 16px;
            background: #667eea;
            color: white;
            text-decoration: none;
            border-radius: 6px;
            margin-bottom: 16px;
            transition: background 0.3s;
        }
        
        .back-btn:hover {
            background: #5568d3;
        }
        
        .station-title {
            font-size: 28px;
            font-weight: bold;
            color: #333;
            margin-bottom: 8px;
        }
        
        .station-info {
            color: #666;
            font-size: 14px;
        }
        
        .pile-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
            gap: 20px;
        }
        
        .pile-card {
            background: white;
            border-radius: 12px;
            padding: 20px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        
        .pile-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 16px;
            padding-bottom: 12px;
            border-bottom: 2px solid #f0f0f0;
        }
        
        .pile-code {
            font-size: 18px;
            font-weight: bold;
            color: #333;
        }
        
        .pile-status {
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: bold;
        }
        
        .status-normal {
            background: #d4edda;
            color: #155724;
        }
        
        .status-fault {
            background: #f8d7da;
            color: #721c24;
        }
        
        .gun-list {
            display: flex;
            flex-direction: column;
            gap: 12px;
        }
        
        .gun-item {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 12px;
            background: #f8f9fa;
            border-radius: 8px;
            cursor: pointer;
            transition: all 0.3s;
        }
        
        .gun-item:hover {
            background: #e9ecef;
            transform: translateX(5px);
        }
        
        .gun-info {
            flex: 1;
        }
        
        .gun-code {
            font-weight: bold;
            color: #333;
            margin-bottom: 4px;
        }
        
        .gun-type {
            font-size: 12px;
            color: #666;
        }
        
        .gun-status {
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: bold;
        }
        
        .gun-status-idle {
            background: #d1ecf1;
            color: #0c5460;
        }
        
        .gun-status-charging {
            background: #fff3cd;
            color: #856404;
        }
        
        .gun-status-fault {
            background: #f8d7da;
            color: #721c24;
        }
        
        .loading {
            text-align: center;
            color: white;
            font-size: 18px;
            padding: 40px;
        }
    </style>
</head>
<body>
    <div class="container">
        <div class="header">
            <a href="javascript:history.back()" class="back-btn">← 返回站点列表</a>
            <div class="station-title" id="station-name">加载中...</div>
            <div class="station-info" id="station-address"></div>
        </div>
        
        <div id="pile-list" class="pile-grid">
            <div class="loading">加载中...</div>
        </div>
    </div>

    <script>
        const stationId = ${stationId};
        
        function loadStationDetail() {
            fetch('/api/stations/' + stationId)
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200) {
                        renderStationDetail(result.data);
                    } else {
                        alert('加载失败: ' + result.message);
                    }
                })
                .catch(error => {
                    console.error('加载站点详情失败:', error);
                    alert('加载失败，请稍后重试');
                });
        }
        
        function renderStationDetail(station) {
            document.getElementById('station-name').textContent = station.name;
            document.getElementById('station-address').textContent = station.address || '暂无地址';
            
            const pileList = document.getElementById('pile-list');
            
            if (!station.piles || station.piles.length === 0) {
                pileList.innerHTML = '<div class="loading">该站点暂无充电桩</div>';
                return;
            }
            
            renderPiles(station.piles);
        }
        
        function renderPiles(piles) {
            const container = document.getElementById('pile-list');
            
            container.innerHTML = piles.map(function(pile) {
                var statusClass = pile.status === 1 ? 'status-normal' : 'status-fault';
                var guns = pile.guns ? pile.guns : [];
                return '<div class="pile-card">'
                    + '<div class="pile-header">'
                    + '<div class="pile-code">' + pile.pileCode + '</div>'
                    + '<span class="pile-status ' + statusClass + '">' + pile.statusText + '</span>'
                    + '</div>'
                    + '<div class="gun-list">' + renderGuns(guns) + '</div>'
                    + '</div>';
            }).join('');
        }
        
        function renderGuns(guns) {
            if (!guns || guns.length === 0) {
                return '<div style="text-align: center; color: #999;">暂无充电枪</div>';
            }
            
            return guns.map(function(gun) {
                var statusClass = 'gun-status-' + getGunStatusClass(gun.status);
                return '<div class="gun-item" onclick="viewGun(' + gun.id + ')">'
                    + '<div class="gun-info">'
                    + '<div class="gun-code">' + gun.gunCode + '</div>'
                    + '<div class="gun-type">' + gun.typeText + '</div>'
                    + '</div>'
                    + '<span class="gun-status ' + statusClass + '">' + gun.statusText + '</span>'
                    + '</div>';
            }).join('');
        }
        
        function getGunStatusClass(status) {
            switch(status) {
                case 0: return 'idle';
                case 1: return 'charging';
                case 2: return 'fault';
                default: return 'idle';
            }
        }
        
        function viewGun(gunId) {
            window.location.href = '/charging/' + gunId;
        }
        
        window.onload = loadStationDetail;
    </script>
</body>
</html>
