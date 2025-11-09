-- ====================================
-- Diagnostic SQL Script for Orders Issue
-- ====================================

-- 1. Show all orders with user association
SELECT 
    o.id AS order_id,
    o.user_id,
    o.status,
    o.total,
    o.created_at,
    u.email AS user_email,
    (SELECT COUNT(*) FROM orders_items oi WHERE oi.order_id = o.id) AS item_count
FROM orders o
LEFT JOIN users u ON o.user_id = u.id
ORDER BY o.created_at DESC
LIMIT 20;

-- 2. Show items for each recent order
SELECT 
    o.id AS order_id,
    o.user_id,
    oi.name AS item_name,
    oi.qty,
    oi.price,
    o.total AS order_total
FROM orders o
JOIN orders_items oi ON oi.order_id = o.id
ORDER BY o.created_at DESC, oi.id
LIMIT 50;

-- 3. Count orders per user
SELECT 
    u.id AS user_id,
    u.email,
    u.role,
    COUNT(o.id) AS order_count
FROM users u
LEFT JOIN orders o ON o.user_id = u.id
GROUP BY u.id, u.email, u.role
ORDER BY order_count DESC;

-- 4. Find orders with NULL user_id (guest orders)
SELECT 
    o.id AS order_id,
    o.status,
    o.total,
    o.created_at,
    (SELECT GROUP_CONCAT(oi.name SEPARATOR ', ') FROM orders_items oi WHERE oi.order_id = o.id) AS items
FROM orders o
WHERE o.user_id IS NULL
ORDER BY o.created_at DESC;

-- 5. Show Order #4 and Order #13 specifically
SELECT 
    o.id AS order_id,
    o.user_id,
    u.email AS user_email,
    o.status,
    o.total,
    o.created_at,
    (SELECT GROUP_CONCAT(CONCAT(oi.name, ' x', oi.qty) SEPARATOR ', ') FROM orders_items oi WHERE oi.order_id = o.id) AS items
FROM orders o
LEFT JOIN users u ON o.user_id = u.id
WHERE o.id IN (4, 13)
ORDER BY o.id;

-- 6. Show most recent 10 orders with full details
SELECT 
    o.id,
    o.user_id,
    u.email,
    o.status,
    o.total,
    o.ship_name,
    o.ship_phone,
    o.created_at,
    COUNT(oi.id) AS item_count
FROM orders o
LEFT JOIN users u ON o.user_id = u.id
LEFT JOIN orders_items oi ON oi.order_id = o.id
GROUP BY o.id, o.user_id, u.email, o.status, o.total, o.ship_name, o.ship_phone, o.created_at
ORDER BY o.created_at DESC
LIMIT 10;
