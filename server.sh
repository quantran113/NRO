#!/bin/bash

ACTION=${1:-status}

case "$ACTION" in
    start)
        echo "🚀 Đang khởi chạy Game Server & Web Admin..."
        cd ~/NRO/SRC && nohup bash run.sh > server.log 2>&1 &
        cd ~/NRO/WEB_ADMIN && pm2 start server.js --name "web_admin" 2>/dev/null || pm2 restart web_admin
        echo "✅ Khởi chạy hoàn tất!"
        ;;
    stop)
        echo "🛑 Đang dừng Game Server & Web Admin..."
        killall -9 java 2>/dev/null || true
        pm2 stop web_admin 2>/dev/null || true
        echo "✅ Đã dừng tất cả dịch vụ!"
        ;;
    restart)
        echo "🔄 Đang khởi chạy lại tất cả..."
        killall -9 java 2>/dev/null || true
        cd ~/NRO/SRC && nohup bash run.sh > server.log 2>&1 &
        pm2 restart web_admin 2>/dev/null || (cd ~/NRO/WEB_ADMIN && pm2 start server.js --name "web_admin")
        echo "✅ Khởi chạy lại thành công!"
        ;;
    log)
        echo "📋 Đang xem log Game Server (Nhấn Ctrl+C để thoát)..."
        cd ~/NRO/SRC && tail -f server.log
        ;;
    status)
        echo "📊 TRẠNG THÁI SERVER:"
        echo "-------------------------------------"
        echo "🎮 Game Server (Java):"
        if pgrep -f "nro.models.server.ServerManager" > /dev/null; then
            echo "   ✅ Đang HOẠT ĐỘNG (PID: $(pgrep -f 'nro.models.server.ServerManager'))"
        else
            echo "   ❌ Đang TẮT"
        fi
        echo "-------------------------------------"
        echo "🌐 Web Admin (PM2):"
        pm2 status
        ;;
    *)
        echo "Cú pháp: bash server.sh {start|stop|restart|log|status}"
        ;;
esac
