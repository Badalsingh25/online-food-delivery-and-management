@echo off
echo ============================================
echo AGENT BACKEND SETUP
echo ============================================
echo.

set /p DB_PASSWORD="Enter MySQL root password: "

echo.
echo Running database setup...
mysql -u root -p%DB_PASSWORD% foodexpress < COMPLETE_AGENT_SETUP.sql

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✅ Setup completed successfully!
    echo.
    echo 🔑 Login Credentials:
    echo    Email:    agent@test.com
    echo    Password: password123
    echo.
    echo 📝 Next Steps:
    echo    1. Start backend: mvn spring-boot:run
    echo    2. Start frontend: cd ../frontend ^&^& npm start
    echo    3. Login and test agent pages
) else (
    echo.
    echo ❌ Setup failed! Check the error messages above.
)

pause
