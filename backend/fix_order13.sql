-- Fix Order #13 by associating it with user_id 10
-- This will make Order #13 appear in "My Orders" for the logged-in user

-- First, verify the current state
SELECT id, user_id, status, total, created_at 
FROM orders 
WHERE id = 13;

-- Update Order #13 to be owned by user_id 10
UPDATE orders 
SET user_id = 10 
WHERE id = 13;

-- Verify the fix
SELECT id, user_id, status, total, created_at 
FROM orders 
WHERE id = 13;

-- Show all orders for user_id 10 (should now include both #4 and #13)
SELECT id, user_id, status, total, created_at 
FROM orders 
WHERE user_id = 10
ORDER BY created_at DESC;
