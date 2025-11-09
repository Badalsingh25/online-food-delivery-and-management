# ============================================
# AGENT BACKEND SETUP SCRIPT
# ============================================

Write-Host "🚀 Setting up Agent Backend & Database" -ForegroundColor Cyan
Write-Host "=======================================" -ForegroundColor Cyan

# Configuration
$dbName = "foodexpress"
$dbUser = "root"

# Ask for password
Write-Host "`n📝 Enter MySQL root password:" -ForegroundColor Yellow
$dbPassword = Read-Host -AsSecureString
$plainPassword = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($dbPassword))

Write-Host "`n✨ Step 1: Running complete agent setup..." -ForegroundColor Green
try {
    $env:MYSQL_PWD = $plainPassword
    mysql -u $dbUser $dbName -e "source COMPLETE_AGENT_SETUP.sql" 2>&1 | Out-Host
    
    if ($LASTEXITCODE -eq 0) {
        Write-Host "✅ Database setup completed successfully!" -ForegroundColor Green
    } else {
        Write-Host "❌ Database setup failed!" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "❌ Error running SQL script: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    Remove-Item Env:\MYSQL_PWD
}

Write-Host "`n✨ Step 2: Verifying backend endpoints..." -ForegroundColor Green

# Check if backend is running
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/login" -Method POST -Body (@{
        email = "agent@test.com"
        password = "password123"
    } | ConvertTo-Json) -ContentType "application/json" -ErrorAction Stop
    
    Write-Host "✅ Backend is running!" -ForegroundColor Green
    
    $loginData = $response.Content | ConvertFrom-Json
    $token = $loginData.token
    
    Write-Host "✅ Login successful!" -ForegroundColor Green
    Write-Host "   Token: $($token.Substring(0, 20))..." -ForegroundColor Gray
    
    # Test orders endpoint
    Write-Host "`n✨ Step 3: Testing agent/orders endpoint..." -ForegroundColor Green
    $headers = @{
        "Authorization" = "Bearer $token"
        "Content-Type" = "application/json"
    }
    
    $ordersResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/agent/orders" -Method GET -Headers $headers
    Write-Host "✅ Orders endpoint working!" -ForegroundColor Green
    Write-Host "   Found $($ordersResponse.Count) orders" -ForegroundColor Gray
    
    # Test transactions endpoint
    Write-Host "`n✨ Step 4: Testing agent/earnings/transactions endpoint..." -ForegroundColor Green
    $transactionsResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/agent/earnings/transactions" -Method GET -Headers $headers
    Write-Host "✅ Transactions endpoint working!" -ForegroundColor Green
    Write-Host "   Found $($transactionsResponse.Count) transactions" -ForegroundColor Gray
    
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq $null) {
        Write-Host "⚠️  Backend is not running!" -ForegroundColor Yellow
        Write-Host "   Start it with: cd backend && mvn spring-boot:run" -ForegroundColor Yellow
    } else {
        Write-Host "❌ Backend test failed: $($_.Exception.Message)" -ForegroundColor Red
    }
}

Write-Host "`n✅ ✅ ✅ SETUP COMPLETE! ✅ ✅ ✅" -ForegroundColor Green
Write-Host "`n📋 Summary:" -ForegroundColor Cyan
Write-Host "   - Database schema created ✓" -ForegroundColor White
Write-Host "   - Agent user created ✓" -ForegroundColor White
Write-Host "   - Test orders created ✓" -ForegroundColor White
Write-Host "   - Agent profile initialized ✓" -ForegroundColor White

Write-Host "`n🔑 Login Credentials:" -ForegroundColor Cyan
Write-Host "   Email:    agent@test.com" -ForegroundColor White
Write-Host "   Password: password123" -ForegroundColor White

Write-Host "`n🌐 Frontend URLs:" -ForegroundColor Cyan
Write-Host "   Dashboard:  http://localhost:4200/agent/dashboard" -ForegroundColor White
Write-Host "   Orders:     http://localhost:4200/agent/orders" -ForegroundColor White
Write-Host "   Earnings:   http://localhost:4200/agent/earnings" -ForegroundColor White
Write-Host "   Profile:    http://localhost:4200/agent/profile" -ForegroundColor White

Write-Host "`n📝 Next Steps:" -ForegroundColor Yellow
Write-Host "   1. Start backend: cd backend && mvn spring-boot:run" -ForegroundColor White
Write-Host "   2. Start frontend: cd frontend && npm start" -ForegroundColor White
Write-Host "   3. Login with agent@test.com / password123" -ForegroundColor White
Write-Host "   4. Navigate to agent pages to see orders & transactions" -ForegroundColor White
