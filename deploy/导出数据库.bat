@echo off
chcp 65001 >nul 2>nul
title TeamDev - 导出数据库
echo ============================================
echo   导出本地 MySQL 数据（只读操作，不改动任何数据）
echo ============================================
echo.

cd /d "%~dp0.."

set DB_USER=root
set DB_PASS=123456
set DB_NAME=team_management_db

set STAMP=%date:~0,4%%date:~5,2%%date:~8,2%
set OUT=deploy\backup_%STAMP%.sql

mysqldump -u %DB_USER% -p%DB_PASS% --no-tablespaces --single-transaction --quick ^
  --default-character-set=utf8mb4 --set-gtid-purged=OFF ^
  --databases %DB_NAME% > "%OUT%"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] 导出失败，请检查 MySQL 是否在运行、账号密码是否正确
    pause
    exit /b 1
)

echo.
echo [OK] 已导出到: %OUT%
echo.
pause
