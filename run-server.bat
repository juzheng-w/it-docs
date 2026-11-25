@echo off
chcp 65001 >nul
title it-docs 后端 8080
rem ===== 改这三行（首次）=====
set KB_DB_HOST=192.168.1.10
set KB_DB_USER=root
set KB_DB_PASSWORD=你的密码
rem ============================
set KB_DB_PORT=3306
set KB_DB_NAME=it_docs
set KB_STORAGE_TYPE=local

cd /d "%~dp0it-docs-api"
echo [it-docs] 编译并启动后端 http://localhost:8080 ...
call mvn -q package -DskipTests
if errorlevel 1 (echo 编译失败 & pause & exit /b 1)
echo [it-docs] 启动中，Ctrl+C 停止。日志关注: 初始管理员 admin/admin@123
java -jar target\it-docs.jar
pause
