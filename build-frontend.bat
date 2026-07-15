@echo off
chcp 65001 >nul 2>nul
title TeamDev - Build Frontend Only
echo ============================================
echo   TeamDev - Build Frontend Only
echo   This script requires Node.js environment
echo ============================================
echo.

cd /d "%~dp0"

node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found. Please install Node.js 14+ or use a machine with npm.
    pause
    exit /b 1
)

echo [1/2] Building frontend...
cd /d "%~dp0frontend"
set NODE_OPTIONS=--max-old-space-size=4096
call npm run build
if %errorlevel% neq 0 (
    echo [ERROR] Frontend build failed
    pause
    exit /b 1
)

echo.
echo [2/2] Copying to backend static folder...
cd /d "%~dp0"
if exist "src\main\resources\static" (
    rd /s /q "src\main\resources\static"
)
mkdir "src\main\resources\static"
xcopy /s /e /y "frontend\dist\*" "src\main\resources\static\" >nul

echo.
echo ============================================
echo   Frontend build completed!
echo   Files copied to: src/main/resources/static/
echo   Now restart the backend to see changes.
echo ============================================
pause
