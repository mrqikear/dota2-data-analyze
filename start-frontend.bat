@echo off
setlocal enabledelayedexpansion

REM Dota2数据分析 - 前端启动脚本
REM 智能检测 Node 22 环境

if exist "E:\easyClaw\QClaw\v0.2.26.557\resources\node\node.exe" (
    set "PATH=E:\easyClaw\QClaw\v0.2.26.557\resources\node;%PATH%"
)

echo ====================================
echo  Dota2数据分析 - 前端开发服务器
echo ====================================
echo.

for /f %%i in ('node --version') do set NODE_VER=%%i
echo Node: !NODE_VER!
echo Project: %~dp0dota2-frontend
echo.

cd /d "%~dp0dota2-frontend"

echo 启动 Vite 开发服务器 (Ctrl+C 停止)
echo ====================================
echo.

call yarn dev
pause
