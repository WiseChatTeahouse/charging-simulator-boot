<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>充电站列表 - 充电模拟器</title>
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
            max-width: 1200px;
            margin: 0 auto;
        }
        
        h1 {
            color: white;
            text-align: center;
            margin-bottom: 30px;
            font-size: 36px;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
        }
        
        .station-grid {
            display: grid;
            grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
            gap: 20px;
        }
        
        .station-card {
            background: white;
            border-radius: 12px;
            padding: 24px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
            transition: transform 0.3s, box-shadow 0.3s;
            cursor: pointer;
        }
        
        .station-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 8px 12px rgba(0,0,0,0.2);
        }
        
        .station-name {
            font-size: 22px;
            font-weight: bold;
            color: #333;
            margin-bottom: 12px;
        }
        
        .station-address {
            color: #666;
            margin-bottom: 12px;
            font-size: 14px;
        }
        
        .station-status {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 20px;
            font-size: 12px;
            font-weight: bold;
        }
        
        .status-active {
            background: #d4edda;
            color: #155724;
        }
        
        .status-inactive {
            background: #f8d7da;
            color: #721c24;
        }
        
        .loading {
            text-align: center;
            color: white;
            font-size: 18px;
            padding: 40px;
        }
        
        .error {
            background: #f8d7da;
            color: #721c24;
            padding: 20px;
            border-radius: 8px;
            text-align: center;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>充电站列表</h1>
        <div id="station-list" class="station-grid">
            <div class="loading">加载中...</div>
        </div>
    </div>

    <script>
        function loadStations() {
            fetch('/api/stations')
                .then(response => response.json())
                .then(result => {
                    if (result.code === 200) {
                        renderStations(result.data);
                    } else {
                        showError(result.message);
                    }
                })
                .catch(error => {
                    console.error('加载站点失败:', error);
                    showError('加载站点失败，请稍后重试');
                });
        }
        
        function renderStations(stations) {
            const container = document.getElementById('station-list');
            
            if (!stations || stations.length === 0) {
                container.innerHTML = '<div class="loading">暂无站点数据</div>';
                return;
            }
            
            container.innerHTML = stations.map(function(station) {
                var address = station.address ? station.address : '暂无地址';
                var statusClass = station.status === 1 ? 'status-active' : 'status-inactive';
                return '<div class="station-card" onclick="viewStation(' + station.id + ')">'
                    + '<div class="station-name">' + station.name + '</div>'
                    + '<div class="station-address">' + address + '</div>'
                    + '<span class="station-status ' + statusClass + '">' + station.statusText + '</span>'
                    + '</div>';
            }).join('');
        }
        
        function viewStation(stationId) {
            window.location.href = '/station/' + stationId;
        }
        
        function showError(message) {
            const container = document.getElementById('station-list');
            container.innerHTML = '<div class="error">' + message + '</div>';
        }
        
        window.onload = loadStations;
    </script>
</body>
</html>
