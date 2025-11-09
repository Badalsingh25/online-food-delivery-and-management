-- =========================================================================
-- CHECK DATABASE STATUS - foodexpress
-- =========================================================================
-- Quick check of your actual data (no dummy data)
-- =========================================================================

USE foodexpress;

-- Check 1: Tables exist?
-- =========================================================================
SELECT 
    '=== TABLES ===' as '',
    table_name as `Table Name`,
    table_rows as `Approx Rows`
FROM information_schema.tables 
WHERE table_schema = 'foodexpress'
AND table_name IN ('agent_profile', 'agent_order_assignment', 'agent_transactions', 'orders', 'users')
ORDER BY table_name;

-- Check 2: AGENT users
-- =========================================================================
SELECT '=== AGENT USERS ===' as '';
SELECT 
    id, 
    email, 
    full_name, 
    role,
    created_at
FROM users 
WHERE role = 'AGENT';

-- Check 3: Agent profiles
-- =========================================================================
SELECT '=== AGENT PROFILES ===' as '';
SELECT 
    user_id,
    is_available as `Online`,
    total_earnings as Total_Earnings,
    pending_payout as Pending_Payout,
    rating
FROM agent_profile;

-- Check 4: Your orders summary
-- =========================================================================
SELECT '=== YOUR ORDERS ===' as '';
SELECT 
    status as `Status`,
    COUNT(*) as `Count`,
    COUNT(CASE WHEN assigned_to IS NOT NULL THEN 1 END) as `Assigned to Agent`,
    SUM(COALESCE(total, 0)) as Total_Value
FROM orders
GROUP BY status
ORDER BY 
    CASE status
        WHEN 'PLACED' THEN 1
        WHEN 'PREPARING' THEN 2
        WHEN 'OUT_FOR_DELIVERY' THEN 3
        WHEN 'DELIVERED' THEN 4
        WHEN 'CANCELLED' THEN 5
        ELSE 6
    END;

-- Check 5: Agent assignments
-- =========================================================================
SELECT '=== AGENT ASSIGNMENTS ===' as '';
SELECT 
    COUNT(*) as `Total Assignments`,
    COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as `Completed`,
    COUNT(CASE WHEN status = 'ASSIGNED' THEN 1 END) as `Active`
FROM agent_order_assignment;

-- Check 6: Transactions
-- =========================================================================
SELECT '=== EARNINGS TRANSACTIONS ===' as '';
SELECT 
    COUNT(*) as `Total Transactions`,
    SUM(total_earning) as Total_Earnings,
    SUM(CASE WHEN DATE(delivered_at) = CURDATE() THEN total_earning ELSE 0 END) as Today_Earnings,
    SUM(CASE WHEN delivered_at >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN total_earning ELSE 0 END) as This_Week_Earnings,
    SUM(CASE WHEN delivered_at >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN total_earning ELSE 0 END) as This_Month_Earnings
FROM agent_transactions;

-- Check 7: Recent delivered orders (your actual data)
-- =========================================================================
SELECT '=== RECENT DELIVERED ORDERS ===' as '';
SELECT 
    id,
    status,
    total as Total,
    delivery_fee as Delivery_Fee,
    CASE WHEN assigned_to IS NOT NULL THEN 'Yes' ELSE 'No' END as `Has Agent`,
    created_at,
    delivered_at
FROM orders 
WHERE status = 'DELIVERED'
ORDER BY created_at DESC
LIMIT 10;

-- =========================================================================
-- WHAT TO DO NEXT:
-- =========================================================================
-- ✓ If you see AGENT users and delivered orders: Good!
-- ✓ If orders are NOT assigned to agents: Run LINK_EXISTING_ORDERS_TO_AGENTS.sql
-- ✓ If you have no AGENT users: Create one first (see below)
--
-- To create an AGENT user:
-- INSERT INTO users (email, password, full_name, role)
-- VALUES ('agent@foodexpress.com', 'bcrypt_hash_here', 'Agent Name', 'AGENT');
-- =========================================================================
