#!/bin/bash
# Cú pháp: 
#   bash sync_vps.sh web   (chỉ đồng bộ Web Admin - siêu nhanh 0.5s)
#   bash sync_vps.sh game  (chỉ đồng bộ Game Server)
#   bash sync_vps.sh       (đồng bộ cả 2)

MODE=${1:-all}
cd /Users/tranquan/Downloads/Teamobi2026

if [ "$MODE" == "web" ]; then
    echo "⚡ Đang đồng bộ WEB ADMIN lên VPS (Siêu nhanh 0.5s)..."
    zip -q -r web_admin.zip WEB_ADMIN -x "WEB_ADMIN/node_modules/*" "WEB_ADMIN/.DS_Store"
    scp web_admin.zip root@20.222.89.127:~/
    ssh root@20.222.89.127 '
        chmod +x ~/server.sh 2>/dev/null || true
        unzip -o ~/web_admin.zip -d ~/
        mkdir -p ~/admin_web
        cp -rf ~/WEB_ADMIN/* ~/admin_web/
        rm -rf ~/WEB_ADMIN
        cd ~/admin_web && pm2 restart web_admin 2>/dev/null || pm2 start server.js --name "web_admin"
        echo "✅ Đồng bộ Web Admin thành công!"
    '

elif [ "$MODE" == "game" ]; then
    echo "🎮 Đang đồng bộ GAME SERVER lên VPS (Siêu nhanh 1s)..."
    zip -q -r src_game.zip SRC -x "SRC/dist/*" "SRC/data/*" "SRC/*.bak" "SRC/.DS_Store"
    scp src_game.zip root@20.222.89.127:~/
    ssh root@20.222.89.127 '
        chmod +x ~/server.sh 2>/dev/null || true
        killall -9 java 2>/dev/null || true
        unzip -o ~/src_game.zip -d ~/
        cp ~/SRC/data/map/tile_set_Info ~/SRC/data/map/tile_set_info 2>/dev/null || true
        cd ~/SRC && nohup bash run.sh > server.log 2>&1 &
        echo "✅ Đồng bộ Game Server thành công!"
    '

else
    echo "🚀 Đang đồng bộ CẢ WEB ADMIN & GAME SERVER (Siêu nhanh 1.5s)..."
    zip -q -r web_admin.zip WEB_ADMIN -x "WEB_ADMIN/node_modules/*" "WEB_ADMIN/.DS_Store"
    zip -q -r src_game.zip SRC -x "SRC/dist/*" "SRC/data/*" "SRC/*.bak" "SRC/.DS_Store"
    scp web_admin.zip src_game.zip root@20.222.89.127:~/
    ssh root@20.222.89.127 '
        chmod +x ~/server.sh 2>/dev/null || true
        cd ~/admin_web && pm2 restart web_admin 2>/dev/null || pm2 start server.js --name "web_admin"
        killall -9 java 2>/dev/null || true
        unzip -o ~/src_game.zip -d ~/
        cp ~/SRC/data/map/tile_set_Info ~/SRC/data/map/tile_set_info 2>/dev/null || true
        cd ~/SRC && nohup bash run.sh > server.log 2>&1 &
        echo "✅ Đồng bộ Tất Cả thành công!"
    '
fi
