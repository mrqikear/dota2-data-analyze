@echo off
echo ========================================================
echo   Dota2 Data Analyze - Local Start Script (DuckDB)
echo ========================================================
echo.

:: 1. Database Info
echo [INFO] Database Mode: DuckDB (No Docker required)
echo [INFO] Database File: .\data\dota2_analyze.duckdb
echo.

:: 2. Start Backend
echo [INFO] Starting Backend (Port 9601)...
start "dota2-api" cmd /c "java -jar dota2-api\target\dota2-api-1.0-SNAPSHOT.jar --spring.profiles.active=dev"
timeout /t 5 /nobreak >nul

:: 3. Start Frontend
echo [INFO] Starting Frontend (Port 5200)...
start "dota2-frontend" cmd /c "cd /d dota2-frontend && yarn22 dev"
timeout /t 3 /nobreak >nul

echo.
echo ========================================================
echo ===== Startup Triggered =====
echo Backend URL:  http://localhost:9601
echo Frontend URL: http://localhost:5200
echo Default Login: admin / admin123
echo ========================================================
echo.
pause