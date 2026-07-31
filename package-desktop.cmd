@echo off
set PATH=E:\easyClaw\QClaw\v0.2.26.557\resources\node;%PATH%
echo ========================================================
echo   Dota2 Data Analyze - Desktop Packaging Script
echo ========================================================
echo.

:: 1. Check Java Environment
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java environment not found! Please install JDK 11+
    pause
    exit /b 1
)

:: 2. Build Backend Jar
echo [1/4] Building Spring Boot Backend...
call build.cmd clean package -DskipTests
if not exist "dota2-api\target\dota2-api-1.0-SNAPSHOT.jar" (
    echo [ERROR] Backend build failed!
    pause
    exit /b 1
)
echo [SUCCESS] Backend built successfully!
echo.

:: 3. Prepare Resources
echo [2/4] Preparing Electron Resources...
if not exist "dota2-frontend\resources\backend" mkdir "dota2-frontend\resources\backend"
copy /y "dota2-api\target\dota2-api-1.0-SNAPSHOT.jar" "dota2-frontend\resources\backend\dota2-api.jar"

if not exist "dota2-frontend\resources\jre" (
    echo [INFO] Searching JAVA_HOME...
    if defined JAVA_HOME (
        echo Copying JRE from %JAVA_HOME%...
        xcopy /e /i /y "%JAVA_HOME%" "dota2-frontend\resources\jre" >nul
    ) else (
        echo [WARNING] JAVA_HOME not defined.
    )
)
echo.

:: 4. Build Frontend & Electron App
echo [3/4] Building Frontend Vue Assets...
cd /d dota2-frontend
call yarn install
call yarn build

echo.
echo [4/4] Packaging Electron Setup (.exe)...
call yarn electron:build

cd /d ..
echo.
echo ========================================================
echo ===== Packaging Complete! =====
echo Output Directory: dota2-frontend\dist_electron\
echo Setup Executable: Dota2DataAnalyze-Setup-1.0.0.exe
echo ========================================================
echo.
pause
