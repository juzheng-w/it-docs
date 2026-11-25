@echo off
chcp 65001 >nul
title it-docs 前端 5173
cd /d "%~dp0it-docs-web"
if not exist node_modules (
  echo [it-docs] 首次安装依赖（npmmirror 源）...
  call npm config set registry https://registry.npmmirror.com
  call npm install --no-audit --no-fund
)
echo [it-docs] 启动开发服务器 http://localhost:5173 （先启动 run-server.bat）...
call npm run dev
pause
