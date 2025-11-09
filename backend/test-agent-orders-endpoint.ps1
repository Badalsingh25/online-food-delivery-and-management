# Test Agent Orders Endpoint

Write-Host "🧪 Testing Agent Orders Endpoint" -ForegroundColor Cyan
Write-Host "=================================" -ForegroundColor Cyan

# Configuration
$baseUrl = "http://localhost:8080"
$agentEmail = "agent@test.com"
$agentPassword = "password123"

# Step 1: Login as agent
Write-Host "`n📝 Step 1: Login as agent..." -ForegroundColor Yellow
$loginBody = @{
    email = $agentEmail
    password = $agentPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$baseUrl/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json"
    $token = $loginResponse.token
    Write-Host "✅ Login successful!" -ForegroundColor Green
    Write-Host "   Token: $($token.Substring(0, 20))..." -ForegroundColor Gray
    Write-Host "   User: $($loginResponse.fullName)" -ForegroundColor Gray
    Write-Host "   Roles: $($loginResponse.roles -join ', ')" -ForegroundColor Gray
} catch {
    Write-Host "❌ Login failed: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "   Make sure agent@test.com exists with password 'password123'" -ForegroundColor Yellow
    Write-Host "   Run VERIFY_AGENT_SETUP.sql to create the test agent" -ForegroundColor Yellow
    exit 1
}

# Step 2: Get agent overview
Write-Host "`n📊 Step 2: Get agent overview..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type" = "application/json"
}

try {
    $overview = Invoke-RestMethod -Uri "$baseUrl/api/agent/overview" -Method GET -Headers $headers
    Write-Host "✅ Overview retrieved successfully!" -ForegroundColor Green
    Write-Host "   Today's Deliveries: $($overview.todayDeliveries)" -ForegroundColor Gray
    Write-Host "   Active Orders: $($overview.activeOrders)" -ForegroundColor Gray
    Write-Host "   Today's Earnings: ₹$($overview.todayEarnings)" -ForegroundColor Gray
    Write-Host "   Available: $($overview.isAvailable)" -ForegroundColor Gray
    Write-Host "   Agent Name: $($overview.agentName)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed to get overview: $($_.Exception.Message)" -ForegroundColor Red
}

# Step 3: Get assigned orders
Write-Host "`n📦 Step 3: Get assigned orders..." -ForegroundColor Yellow
try {
    $orders = Invoke-RestMethod -Uri "$baseUrl/api/agent/orders" -Method GET -Headers $headers
    Write-Host "✅ Orders retrieved successfully!" -ForegroundColor Green
    Write-Host "   Total Orders: $($orders.Count)" -ForegroundColor Gray
    
    if ($orders.Count -eq 0) {
        Write-Host "   ⚠️  No orders assigned to this agent yet" -ForegroundColor Yellow
        Write-Host "   Run CREATE_TEST_AGENT_ORDER.sql to create test orders" -ForegroundColor Yellow
    } else {
        Write-Host "`n   📋 Order Details:" -ForegroundColor Cyan
        foreach ($order in $orders) {
            Write-Host "   ---" -ForegroundColor Gray
            Write-Host "   Order #: $($order.orderNumber)" -ForegroundColor White
            Write-Host "   Status: $($order.status)" -ForegroundColor White
            Write-Host "   Restaurant: $($order.restaurantName)" -ForegroundColor White
            Write-Host "   Customer: $($order.customerName)" -ForegroundColor White
            Write-Host "   Amount: ₹$($order.totalAmount)" -ForegroundColor White
            Write-Host "   Distance: $($order.distance)" -ForegroundColor White
        }
    }
} catch {
    $statusCode = $_.Exception.Response.StatusCode.value__
    Write-Host "❌ Failed to get orders!" -ForegroundColor Red
    Write-Host "   Status Code: $statusCode" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($statusCode -eq 403) {
        Write-Host "`n   🔍 Diagnosis: Access Forbidden (403)" -ForegroundColor Yellow
        Write-Host "   Possible causes:" -ForegroundColor Yellow
        Write-Host "   1. User doesn't have AGENT role" -ForegroundColor Yellow
        Write-Host "   2. Token is invalid or expired" -ForegroundColor Yellow
        Write-Host "   3. Backend security configuration issue" -ForegroundColor Yellow
        Write-Host "`n   💡 Solution:" -ForegroundColor Yellow
        Write-Host "   Run VERIFY_AGENT_SETUP.sql to ensure AGENT role is properly set up" -ForegroundColor Yellow
    } elseif ($statusCode -eq 401) {
        Write-Host "`n   🔍 Diagnosis: Unauthorized (401)" -ForegroundColor Yellow
        Write-Host "   The authentication token is missing or invalid" -ForegroundColor Yellow
    }
}

# Step 4: Get availability status
Write-Host "`n🔄 Step 4: Get availability status..." -ForegroundColor Yellow
try {
    $availability = Invoke-RestMethod -Uri "$baseUrl/api/agent/availability" -Method GET -Headers $headers
    Write-Host "✅ Availability retrieved successfully!" -ForegroundColor Green
    Write-Host "   Available: $($availability.isAvailable)" -ForegroundColor Gray
    Write-Host "   Today's Online Hours: $($availability.todayOnlineHours)" -ForegroundColor Gray
    Write-Host "   Today's Deliveries: $($availability.todayDeliveries)" -ForegroundColor Gray
} catch {
    Write-Host "❌ Failed to get availability: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "`n✅ Testing complete!" -ForegroundColor Green
Write-Host "`n📝 Summary:" -ForegroundColor Cyan
Write-Host "   - If you see 403 errors, run: VERIFY_AGENT_SETUP.sql" -ForegroundColor White
Write-Host "   - If you have no orders, run: CREATE_TEST_AGENT_ORDER.sql" -ForegroundColor White
Write-Host "   - Backend endpoint is working, frontend should display data" -ForegroundColor White
