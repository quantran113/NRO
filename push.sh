#!/bin/bash
echo "🚀 Đang push code lên GitHub..."
git add .
git commit -m "Update code $(date '+%Y-%m-%d %H:%M:%S')" 2>/dev/null
git push origin main
echo "✅ Push thành công!"
