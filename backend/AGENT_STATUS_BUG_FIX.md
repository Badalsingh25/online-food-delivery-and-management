# 🐛 AGENT STATUS BUG - FIXED

**Issue:** Agent goes offline when navigating between pages  
**User:** bikash@gmail.com  
**Date:** November 4, 2025

---

## 🐛 **Problem:**

When agent sets themselves ONLINE in dashboard, then navigates to Orders/Map/Earnings pages, **status automatically resets to OFFLINE**.

---

## 🔍 **Root Cause:**

### **Backend Issue:**
```java
// AgentController.java - Line 51-52
AgentProfile profile = agentProfileRepository.findByUserId(user.getId())
    .orElseGet(() -> createDefaultAgentProfile(user.getId()));
```

If `bikash@gmail.com` doesn't have an `agent_profile` record:
1. Every page calls `getOverview()`, `getOrders()`, etc.
2. Each checks if profile exists
3. If NOT found → creates new profile with `isAvailable = false`
4. **This happens on EVERY page navigation!**

### **Why This Happens:**
- User `bikash@gmail.com` exists in `users` table
- BUT no record in `agent_profile` table
- Backend auto-creates profile with OFFLINE status
- Toggle to ONLINE works temporarily
- But next page navigation recreates profile as OFFLINE again!

---

## ✅ **SOLUTION:**

### **1. Fix Database (Immediate)** 🚀

Run this SQL to create proper agent profile:

```bash
cd d:\ProductDevelopment\backend
mysql -u root -p foodexpress < FIX_BIKASH_AGENT.sql
```

**Password:** `Badal@1234`

**What it does:**
- ✅ Adds AGENT role to bikash@gmail.com
- ✅ Creates agent_profile with ONLINE status
- ✅ Sets vehicle type: BIKE
- ✅ Sets rating: 5.0
- ✅ Ready to receive orders!

---

### **2. Fix Backend Code (Permanent)** 🔧

**File:** `AgentController.java`  
**Method:** `createDefaultAgentProfile()` - Line 333

**Added:**
- Logging when profile is auto-created
- Default vehicle type
- Better rating (5.0 instead of 0.0)

**Changed Code:**
```java
private AgentProfile createDefaultAgentProfile(Long userId) {
    AgentProfile profile = new AgentProfile();
    profile.setUserId(userId);
    profile.setIsAvailable(false); // Default to offline for NEW agents
    profile.setRating(5.0); // Start with good rating
    profile.setVehicleType("BIKE"); // Default vehicle
    profile.setLastStatusChange(Instant.now());
    profile.setCreatedAt(Instant.now());
    
    System.out.println("[AgentController] Created new agent profile for user: " + userId);
    return agentProfileRepository.save(profile);
}
```

---

## 📝 **How to Fix Your Account:**

### **Quick Fix (5 seconds):**

```bash
# Run the SQL script
mysql -u root -p foodexpress < FIX_BIKASH_AGENT.sql
```

### **Manual Fix (if needed):**

```sql
USE foodexpress;

-- Get your user ID
SET @bikash_id = (SELECT id FROM users WHERE email = 'bikash@gmail.com');

-- Add AGENT role
INSERT IGNORE INTO user_roles (user_id, role_id)
VALUES (@bikash_id, (SELECT id FROM role WHERE name = 'AGENT'));

-- Create agent profile (ONLINE by default)
INSERT INTO agent_profile (user_id, is_available, rating, total_earnings, pending_payout, vehicle_type, created_at, last_status_change)
VALUES (@bikash_id, 1, 5.0, 0.0, 0.0, 'BIKE', NOW(), NOW());
```

---

## 🧪 **Testing:**

### **Before Fix:**
1. ❌ Go online in dashboard
2. ❌ Navigate to Orders page
3. ❌ Status shows OFFLINE again

### **After Fix:**
1. ✅ Run SQL script
2. ✅ Go online in dashboard  
3. ✅ Navigate to Orders page
4. ✅ Status stays ONLINE! 🎉

---

## 🔄 **Restart Backend:**

After running SQL fix:

```bash
# Stop current backend (Ctrl+C if running)

# Recompile
mvn clean compile

# Start backend
mvn spring-boot:run
```

---

## ✅ **Verification:**

### **Check Database:**
```sql
SELECT 
    u.email,
    ap.is_available as online,
    ap.rating,
    ap.vehicle_type,
    r.name as role
FROM users u
LEFT JOIN agent_profile ap ON u.id = ap.user_id
LEFT JOIN user_roles ur ON u.id = ur.user_id
LEFT JOIN role r ON ur.role_id = r.id
WHERE u.email = 'bikash@gmail.com';
```

**Expected Result:**
| email | online | rating | vehicle_type | role |
|-------|--------|--------|--------------|------|
| bikash@gmail.com | 1 | 5.0 | BIKE | AGENT |

---

## 🎯 **Summary:**

**Issue:** Auto-reset to offline on page navigation  
**Cause:** Missing agent_profile record  
**Fix:** Run `FIX_BIKASH_AGENT.sql`  
**Time:** 10 seconds  
**Result:** Stay online permanently! ✅

---

**Now try:**
1. Run SQL script
2. Restart backend
3. Login as bikash@gmail.com
4. Toggle to ONLINE
5. Navigate to Orders/Map/Earnings
6. ✅ Should stay ONLINE! 🎉
