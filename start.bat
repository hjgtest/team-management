@echo off
chcp 65001 >nul 2>nul
title TeamDev - Build and Start
echo ============================================
echo   TeamDev Management System - Build + Start
echo ============================================
echo.

cd /d "%~dp0"

java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found. Please install JDK 8+
    pause
    exit /b 1
)

node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Please install Node.js 14+
    pause
    exit /b 1
)

echo [1/4] Installing frontend dependencies...
cd /d "%~dp0frontend"
if not exist node_modules (
    call npm install
    if %errorlevel% neq 0 (
        echo [ERROR] npm install failed
        pause
        exit /b 1
    )
) else (
    echo      node_modules exists, skip install
)

echo.
echo [2/4] Building frontend...
set NODE_OPTIONS=--max-old-space-size=4096
call npm run build
if %errorlevel% neq 0 (
    echo [ERROR] Frontend build failed
    pause
    exit /b 1
)

echo.
echo [3/4] Copying frontend to backend static...
cd /d "%~dp0"
if exist "src\main\resources\static" (
    rd /s /q "src\main\resources\static"
)
mkdir "src\main\resources\static"
xcopy /s /e /y "frontend\dist\*" "src\main\resources\static\" >nul
echo      Done

echo.
echo [4/4] Maven package and start...
call "%~dp0mvnw.cmd" clean package -DskipTests
if %errorlevel% neq 0 (
    echo [ERROR] Maven build failed
    pause
    exit /b 1
)

echo.
echo ============================================
echo   Running! Open http://localhost:8080
echo   Press Ctrl+C to stop
echo ============================================
echo.
java -jar target\team-management-system-0.0.1-SNAPSHOT.jar
pause
