@echo off
echo ========================================
echo HungerExpress - Correct Startup Script
echo ========================================
echo.
echo This will start:
echo - Backend on port 8080
echo - Frontend on port 4200
echo.
echo Press Ctrl+C to stop this script
echo ========================================
echo.

:: Check if ports are free
echo Checking if ports 4200 and 8080 are free...
netstat -ano | findstr :4200 >nul
if %errorlevel% equ 0 (
    echo WARNING: Port 4200 is already in use!
    echo Please close the application using port 4200
    pause
    exit /b 1
)

netstat -ano | findstr :8080 >nul
if %errorlevel% equ 0 (
    echo WARNING: Port 8080 is already in use!
    echo Please close the application using port 8080
    pause
    exit /b 1
)

echo Ports are free! Starting services...
echo.

:: Start backend in new window
echo Starting Backend on port 8080...
start "HungerExpress Backend (Port 8080)" cmd /k "cd /d D:\ProductDevelopment\backend && mvn spring-boot:run"

:: Wait a bit for backend to start
timeout /t 5 /nobreak

:: Start frontend in new window
echo Starting Frontend on port 4200...
start "HungerExpress Frontend (Port 4200)" cmd /k "cd /d D:\ProductDevelopment\frontend && ng serve"

echo.
echo ========================================
echo Services are starting!
echo ========================================
echo.
echo Backend: http://localhost:8080
echo Frontend: http://localhost:4200
echo.
echo Wait for both windows to show "started" messages
echo Then open: http://localhost:4200 in your browser
echo.
echo Press any key to close this window...
pause >nul
