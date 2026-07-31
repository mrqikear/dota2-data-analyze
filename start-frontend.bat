@echo off
setlocal enabledelayedexpansion

REM Dota2数据分析 - 前端启动脚本 (使用 Node 22)
REM 双击运行即可

set PROJECT_DIR=D:\dota2-data-analyze\dota2-frontend

echo ====================================
echo  Dota2数据分析 - 前端开发服务器
echo ====================================
echo.

pushd "E:\easyClaw\QClaw\v0.2.26.557\resources\node"
for /f %%i in ('.\node.exe --version') do set NODE_VER=%%i
popd
echo Node: %NODE_VER%
echo Project: %PROJECT_DIR%
echo.

cd /d "%PROJECT_DIR%"

echo 启动 Vite 开发服务器 (Ctrl+C 停止)
echo ====================================
echo.

yarn22 dev
pause
