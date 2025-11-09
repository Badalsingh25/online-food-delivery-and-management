# 🚀 Agent Backend & Database Setup Guide

## 📋 Overview

This guide will help you set up the complete backend infrastructure for the Agent role, including:
- ✅ Database schema with `agent_id` column
- ✅ Agent role and test user
- ✅ Test orders assigned to agent
- ✅ Transactions with payment status
- ✅ All necessary indexes and constraints

---

## 🎯 Quick Setup (Recommended)

### Option 1: PowerShell (Windows)
```powershell
cd d:\ProductDevelopment\backend
.\setup-agent-backend.ps1
```

### Option 2: Batch File (Windows)
```cmd
cd d:\ProductDevelopment\backend
setup-agent-backend.bat
```

### Option 3: Manual SQL (Any OS)
```bash
cd d:/ProductDevelopment/backend
mysql -u root -p foodexpress < COMPLETE_AGENT_SETUP.sql
```

---

## 📊 What Gets Created

### 1. **Database Schema Updates**
```sql
orders table:
  - agent_id (BIGINT) - Foreign key to user.id
  - payment_status (VARCHAR) - PENDING/COMPLETED
  - Indexes on: agent_id, status, payment_status, delivered_at
```

### 2. **Agent User**
```
Email:    agent@test.com
Password: password123
Role:     AGENT
Status:   Enabled
```

### 3. **Agent Profile**
```
Rating:         4.5/5
Total Earnings: ₹150 (from delivered orders)
Pending Payout: ₹150
Vehicle:        BIKE (KA-01-AB-1234)
License:        DL-1234567890
```

### 4. **Test Orders**
- **Order 1**: ASSIGNED_TO_AGENT (New, needs pickup)
- **Order 2**: OUT_FOR_DELIVERY (In transit)
- **Order 3**: DELIVERED (Completed, ₹50 fee)

Each order includes:
- Customer information
- Delivery address with GPS coordinates
- Order items (2-3 items per order)
- Payment method (CASH/ONLINE)
- Timestamps for created, dispatched, delivered

---

## 🧪 Testing the Setup

### 1. **Login as Agent**
```
URL: http://localhost:4200/login
Email: agent@test.com
Password: password123
```

### 2. **Check Dashboard**
```
URL: http://localhost:4200/agent/dashboard

Should show:
- Today's Deliveries: 1
- Active Orders: 2
- Deliveries Completed: 1
- Today's Earnings: ₹50
```

### 3. **Check Orders Page**
```
URL: http://localhost:4200/agent/orders

Should display:
- 2 active orders (ASSIGNED + OUT_FOR_DELIVERY)
- Order details with restaurant & customer info
- Action buttons (Mark Picked Up, Call, etc.)
```

### 4. **Check Transactions**
```
URL: http://localhost:4200/agent/earnings

Should show:
- Total Earnings: ₹50
- Recent Transactions table with 1 completed delivery
```

---

## 🔧 Manual Setup (Step by Step)

If you prefer to understand each step:

### Step 1: Setup Database Schema
```bash
mysql -u root -p foodexpress < SETUP_AGENT_ORDERS_DB.sql
```

### Step 2: Create Agent User
```bash
mysql -u root -p foodexpress < VERIFY_AGENT_SETUP.sql
```

### Step 3: Create Test Data
```bash
mysql -u root -p foodexpress < CREATE_AGENT_TEST_DATA.sql
```

---

## 📝 API Endpoints

### Get Assigned Orders
```http
GET /api/agent/orders
Authorization: Bearer <token>

Returns: Array of orders assigned to the agent
Status: ASSIGNED_TO_AGENT or PICKED_UP
```

### Get Transactions
```http
GET /api/agent/earnings/transactions
Authorization: Bearer <token>

Returns: Array of completed deliveries where payment_status = 'COMPLETED'
```

### Get Overview Stats
```http
GET /api/agent/overview
Authorization: Bearer <token>

Returns:
{
  "todayDeliveries": 1,
  "activeOrders": 2,
  "todayEarnings": 50.0,
  "totalDeliveries": 1,
  "isAvailable": false,
  "agentName": "Test Agent"
}
```

---

## 🐛 Troubleshooting

### Problem: "403 Forbidden" on agent endpoints

**Solution 1**: Ensure agent role is assigned
```sql
SELECT u.email, r.name 
FROM user u 
JOIN user_role ur ON u.id = ur.user_id 
JOIN role r ON ur.role_id = r.id 
WHERE u.email = 'agent@test.com';
```

**Solution 2**: Re-login to get fresh JWT token
```
1. Logout from frontend
2. Login again with agent@test.com / password123
3. Try accessing agent pages
```

### Problem: "No orders showing"

**Solution**: Check if orders are assigned
```sql
SELECT order_number, status, agent_id 
FROM orders 
WHERE agent_id IS NOT NULL;
```

### Problem: "No transactions showing"

**Solution**: Transactions only show for DELIVERED orders with COMPLETED payment
```sql
SELECT order_number, status, payment_status, delivery_fee
FROM orders 
WHERE agent_id = (SELECT id FROM user WHERE email = 'agent@test.com')
  AND status = 'DELIVERED'
  AND payment_status = 'COMPLETED';
```

---

## 📚 Database Schema Reference

### orders table (relevant columns)
```sql
id                BIGINT          PRIMARY KEY AUTO_INCREMENT
user_id           BIGINT          NOT NULL (customer)
restaurant_id     BIGINT          NOT NULL
agent_id          BIGINT          NULL (assigned delivery agent)
order_number      VARCHAR(50)     UNIQUE
status            VARCHAR(50)     (ASSIGNED_TO_AGENT, OUT_FOR_DELIVERY, DELIVERED, etc.)
payment_method    VARCHAR(50)     (CASH, ONLINE, CARD)
payment_status    VARCHAR(50)     (PENDING, COMPLETED, FAILED)
total_amount      DECIMAL(10,2)   Order total
delivery_fee      DECIMAL(10,2)   Agent's earning per delivery
delivery_address  TEXT            Full delivery address
delivery_phone    VARCHAR(20)     Customer phone
delivery_latitude DECIMAL(10,8)   GPS coordinates
delivery_longitude DECIMAL(11,8)  GPS coordinates
created_at        TIMESTAMP       Order placed time
dispatched_at     TIMESTAMP       Picked up time
delivered_at      TIMESTAMP       Delivered time
```

### agent_profile table
```sql
user_id           BIGINT          PRIMARY KEY (FK to user.id)
is_available      BOOLEAN         Online/Offline status
rating            DECIMAL(3,2)    Average rating (0-5)
total_earnings    DECIMAL(10,2)   All-time earnings
pending_payout    DECIMAL(10,2)   Unpaid amount
vehicle_type      VARCHAR(50)     BIKE, SCOOTER, CAR, etc.
vehicle_number    VARCHAR(50)     Registration number
license_number    VARCHAR(50)     Driving license
last_status_change TIMESTAMP      When status was last changed
```

---

## ✅ Success Checklist

- [ ] Database schema updated (agent_id column exists)
- [ ] Agent role created in database
- [ ] Test agent user created (agent@test.com)
- [ ] Agent profile created
- [ ] Test orders assigned to agent
- [ ] Backend server running (port 8080)
- [ ] Can login as agent@test.com
- [ ] Dashboard shows statistics
- [ ] Orders page displays assigned orders
- [ ] Transactions page shows completed deliveries

---

## 🆘 Need Help?

1. **Check backend logs**: Look for SQL errors or authentication issues
2. **Verify database**: Run queries to check data exists
3. **Test endpoints**: Use the test-agent-orders-endpoint.ps1 script
4. **Re-run setup**: Safe to run COMPLETE_AGENT_SETUP.sql multiple times

---

## 📞 Support

For issues or questions, check:
- Backend console logs
- Browser console (Network tab)
- MySQL error logs
- Application logs in `backend/logs/`
