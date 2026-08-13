#!/bin/bash
while true; do
    echo "🚀 Đang khởi chạy NRO Game Server..."
    java -server -Xms256m -Xmx1024m -Dfile.encoding=UTF-8 -cp "build/classes:20.jar:lib/*" nro.models.server.ServerManager
    echo "⚠️ Game Server bị tắt! Tự động khởi động lại sau 2 giây..."
    sleep 2
done
