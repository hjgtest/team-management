@echo off
chcp 65001 >nul 2>nul
title TeamDev - Quick Start
echo ============================================
echo   TeamDev - Quick Start (jar)
echo   Open http://localhost:8080
echo   Press Ctrl+C to stop
echo ============================================
echo.

cd /d "%~dp0"

if not exist "target\team-management-system-0.0.1-SNAPSHOT.jar" (
    echo [INFO] No jar found, building first...
    echo.
    call "%~dp0mvnw.cmd" clean package -DskipTests
    if %errorlevel% neq 0 (
        echo [ERROR] Maven build failed
        pause
        exit /b 1
    )
)

java -jar target\team-management-system-0.0.1-SNAPSHOT.jar
pause
