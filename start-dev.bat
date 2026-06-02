@echo off
chcp 65001 >nul 2>nul
title TeamDev - Dev Mode
echo ============================================
echo   TeamDev - Dev Mode
echo   Backend: http://localhost:8080
echo   Frontend: http://localhost:8081
echo ============================================
echo.

cd /d "%~dp0"

java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found
    pause
    exit /b 1
)

node -v >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Node.js not found
    pause
    exit /b 1
)

echo [1/3] Installing frontend deps...
cd /d "%~dp0frontend"
if not exist node_modules (
    call npm install
) else (
    echo      node_modules exists, skip
)

echo.
echo [2/3] Starting backend (new window)...
cd /d "%~dp0"
start "Backend-SpringBoot" cmd /c "cd /d "%~dp0" && "%~dp0mvnw.cmd" spring-boot:run && pause"

echo.
echo [3/3] Starting frontend dev server...
echo      Waiting 5s for backend...
timeout /t 5 /nobreak >nul

cd /d "%~dp0frontend"
echo.
echo ============================================
echo   Frontend: http://localhost:8081
echo   Backend:  http://localhost:8080
echo   Press Ctrl+C to stop frontend
echo ============================================
echo.
call npm run serve
pause
