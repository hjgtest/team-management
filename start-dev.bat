@echo off
chcp 65001 >nul 2>nul
title TeamDev - Dev Mode
echo ============================================
echo   TeamDev - Dev Mode (Backend Only)
echo   Backend: http://localhost:8080
 echo   Frontend: Use built static files (no npm needed)
echo ============================================
echo.

cd /d "%~dp0"

java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo [ERROR] Java not found
    pause
    exit /b 1
)

echo [1/2] Starting backend (new window)...
cd /d "%~dp0"
start "Backend-SpringBoot" cmd /c "cd /d "%~dp0" && "%~dp0mvnw.cmd" spring-boot:run && pause"

echo.
echo [2/2] Backend is starting...
echo      Frontend static files are ready in frontend/dist/
echo      Access: http://localhost:8080
pause
