#!/bin/bash
while true; do
    echo "🚀 Bắt đầu khởi chạy Game Server..."
    java -server -Dfile.encoding=UTF-8 -cp "build/classes:20.jar:lib/*" nro.models.server.ServerManager
    echo "🔄 Server đã lưu dữ liệu và hoàn tất bảo trì. Đang tự động mở lại sau 5 giây..."
    sleep 5
done
