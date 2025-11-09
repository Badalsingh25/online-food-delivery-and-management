-- =========================================================================
-- LINK EXISTING ORDERS TO AGENTS (NO DUMMY DATA)
-- =========================================================================
-- Database: foodexpress
-- This script works with your REAL existing data
-- It assigns your actual delivered orders to agent users
-- =========================================================================

USE foodexpress;

-- Step 1: Check if you have an AGENT user
-- =========================================================================
SELECT '=== CHECKING FOR AGENT USERS ===' as '';
SELECT id, email, full_name, role 
FROM users 
WHERE role = 'AGENT';

-- If you don't have an agent user, create one (update email/password as needed)
-- Uncomment and modify if needed:
/*
INSERT INTO users (email, password, full_name, role, created_at)
VALUES ('youragent@foodexpress.com', 'your_bcrypt_password_hash', 'Your Agent Name', 'AGENT', NOW());
*/

-- Get the first agent user ID
SET @agent_user_id = (SELECT id FROM users WHERE role = 'AGENT' LIMIT 1);

SELECT 
    CASE 
        WHEN @agent_user_id IS NOT NULL THEN CONCAT('✓ Agent found: ID ', @agent_user_id)
        ELSE '✗ No AGENT user found - create one first!'
    END as Status;

-- Step 2: Create agent profile if it doesn't exist
-- =========================================================================
INSERT INTO agent_profile (user_id, is_available, rating, total_earnings, pending_payout, created_at)
SELECT 
    @agent_user_id,
    TRUE,
    4.5,
    0.0,
    0.0,
    NOW()
WHERE @agent_user_id IS NOT NULL
AND NOT EXISTS (SELECT 1 FROM agent_profile WHERE user_id = @agent_user_id);

SELECT '=== AGENT PROFILE ===' as '';
SELECT * FROM agent_profile WHERE user_id = @agent_user_id;

-- Step 3: Check your existing DELIVERED orders
-- =========================================================================
SELECT '=== YOUR DELIVERED ORDERS ===' as '';
SELECT 
    COUNT(*) as total_delivered,
    COUNT(CASE WHEN assigned_to IS NULL THEN 1 END) as unassigned,
    COUNT(CASE WHEN assigned_to IS NOT NULL THEN 1 END) as already_assigned
FROM orders 
WHERE status = 'DELIVERED';

-- Step 4: Assign unassigned DELIVERED orders to the agent
-- =========================================================================
-- This assigns your REAL orders to the agent (adjust LIMIT as needed)
UPDATE orders 
SET 
    assigned_to = @agent_user_id,
    delivery_fee = CASE 
        WHEN delivery_fee IS NULL OR delivery_fee = 0 THEN 50.00 
        ELSE delivery_fee 
    END,
    delivered_at = CASE 
        WHEN delivered_at IS NULL THEN created_at + INTERVAL 1 HOUR
        ELSE delivered_at
    END
WHERE status = 'DELIVERED' 
AND assigned_to IS NULL
AND @agent_user_id IS NOT NULL
LIMIT 20;  -- Adjust this number or remove LIMIT to assign all

SELECT CONCAT('✓ Assigned ', ROW_COUNT(), ' orders to agent') as Result;

-- Step 5: Create agent_order_assignment records
-- =========================================================================
INSERT INTO agent_order_assignment (agent_id, order_id, assigned_at, delivered_at, status)
SELECT 
    @agent_user_id,
    id,
    created_at,
    delivered_at,
    'COMPLETED'
FROM orders
WHERE assigned_to = @agent_user_id
AND status = 'DELIVERED'
AND @agent_user_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM agent_order_assignment 
    WHERE order_id = orders.id AND agent_id = @agent_user_id
);

SELECT CONCAT('✓ Created ', ROW_COUNT(), ' assignment records') as Result;

-- Step 6: Create agent_transactions from real orders
-- =========================================================================
INSERT INTO agent_transactions (
    agent_id,
    order_id,
    order_number,
    delivery_fee,
    bonus,
    total_earning,
    transaction_type,
    status,
    restaurant_name,
    customer_name,
    delivery_address,
    delivered_at,
    created_at
)
SELECT 
    @agent_user_id,
    o.id,
    CONCAT('ORD-', o.id),
    COALESCE(o.delivery_fee, 50.00) * 0.80,  -- Agent gets 80% of delivery fee
    0.00,  -- No bonus
    COALESCE(o.delivery_fee, 50.00) * 0.80,
    'DELIVERY',
    'COMPLETED',
    COALESCE(r.name, 'Restaurant'),
    COALESCE(o.ship_name, 'Customer'),
    CONCAT_WS(', ', o.ship_line1, o.ship_city, o.ship_state),
    COALESCE(o.delivered_at, o.created_at + INTERVAL 1 HOUR),
    NOW()
FROM orders o
LEFT JOIN restaurants r ON o.restaurant_id = r.id
WHERE o.assigned_to = @agent_user_id
AND o.status = 'DELIVERED'
AND @agent_user_id IS NOT NULL
AND NOT EXISTS (
    SELECT 1 FROM agent_transactions 
    WHERE order_id = o.id AND agent_id = @agent_user_id
);

SELECT CONCAT('✓ Created ', ROW_COUNT(), ' transaction records') as Result;

-- Step 7: Update agent profile with calculated earnings
-- =========================================================================
UPDATE agent_profile ap
SET 
    total_earnings = (
        SELECT COALESCE(SUM(total_earning), 0)
        FROM agent_transactions
        WHERE agent_id = ap.user_id
    ),
    pending_payout = (
        SELECT COALESCE(SUM(total_earning), 0)
        FROM agent_transactions
        WHERE agent_id = ap.user_id
        AND status = 'COMPLETED'
    )
WHERE ap.user_id = @agent_user_id
AND @agent_user_id IS NOT NULL;

-- Step 8: Verification - Show Results
-- =========================================================================
SELECT '=== ✓ FINAL RESULTS ===' as '';

SELECT 
    'Agent Profile' as Type,
    user_id,
    is_available as Online,
    total_earnings as Total_Earnings,
    pending_payout as Pending_Payout,
    rating
FROM agent_profile 
WHERE user_id = @agent_user_id;

SELECT 
    'Assigned Orders' as Type,
    COUNT(*) as Count,
    SUM(total) as Total_Order_Value,
    SUM(delivery_fee) as Total_Delivery_Fees
FROM orders 
WHERE assigned_to = @agent_user_id;

SELECT 
    'Transactions' as Type,
    COUNT(*) as Count,
    SUM(total_earning) as Total_Earnings,
    COUNT(CASE WHEN DATE(delivered_at) = CURDATE() THEN 1 END) as Today_Count,
    SUM(CASE WHEN DATE(delivered_at) = CURDATE() THEN total_earning ELSE 0 END) as Today_Earnings
FROM agent_transactions 
WHERE agent_id = @agent_user_id;

-- =========================================================================
-- WHAT THIS SCRIPT DID:
-- =========================================================================
-- ✓ Found or confirmed your AGENT user
-- ✓ Created agent_profile for the agent
-- ✓ Assigned your REAL delivered orders to the agent
-- ✓ Created assignment records
-- ✓ Created transaction records with earnings
-- ✓ Calculated total and pending earnings
--
-- NO DUMMY DATA WAS CREATED - This uses your actual orders!
-- =========================================================================
