@echo off
setlocal enabledelayedexpansion

REM Kill any running processes that lock dist_electron files
taskkill /f /im electron.exe >nul 2>&1
taskkill /f /im java.exe >nul 2>&1

REM 1. Detect Node 22 Environment
if exist "E:\easyClaw\QClaw\v0.2.26.557\resources\node\node.exe" (
    set "PATH=E:\easyClaw\QClaw\v0.2.26.557\resources\node;%PATH%"
)

REM 2. Detect Java 11 Environment
if exist "E:\jdk-11.0.20_windows-x64_bin\jdk-11.0.20\bin\java.exe" (
    set "JAVA_HOME=E:\jdk-11.0.20_windows-x64_bin\jdk-11.0.20"
    set "PATH=!JAVA_HOME!\bin;%PATH%"
)

echo ========================================================
echo   Dota2 Data Analyze - Fast Desktop Packaging Script
echo ========================================================
echo.

where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java environment not found! Please install JDK 11+
    pause
    exit /b 1
)

REM 3. Build Backend Jar
echo [1/4] Building Spring Boot Backend...
call build.cmd clean package -DskipTests
if not exist "dota2-api\target\dota2-api-1.0-SNAPSHOT.jar" (
    echo [ERROR] Backend build failed!
    pause
    exit /b 1
)
echo [SUCCESS] Backend built successfully!
echo.

REM 4. Prepare Resources
echo [2/4] Preparing Electron Resources...
if not exist "dota2-frontend\resources\backend" mkdir "dota2-frontend\resources\backend"
copy /y "dota2-api\target\dota2-api-1.0-SNAPSHOT.jar" "dota2-frontend\resources\backend\dota2-api.jar" >nul

if not exist "dota2-frontend\resources\jre" (
    echo [INFO] Searching JAVA_HOME for JRE bundle...
    if defined JAVA_HOME (
        echo Copying JRE from %JAVA_HOME%...
        xcopy /e /i /y "%JAVA_HOME%" "dota2-frontend\resources\jre" >nul
    ) else (
        echo [WARNING] JAVA_HOME not defined.
    )
)
echo.

REM 5. Clean & Build Frontend & Portable App
echo [3/4] Building Frontend Vue Assets...
if exist "dota2-frontend\dist_electron" rmdir /s /q "dota2-frontend\dist_electron"
cd /d dota2-frontend
call yarn build

echo.
echo [4/4] Packaging Single Portable Executable (Dota2DataAnalyze.exe)...
call yarn electron:build

cd /d ..
echo.
echo ========================================================
echo ===== Packaging Complete! =====
echo Output File: dota2-frontend\dist_electron\Dota2DataAnalyze.exe
echo ========================================================
echo.
