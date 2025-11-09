USE hungerexpress;
SELECT id, status, assigned_to, total, created_at FROM orders ORDER BY id DESC LIMIT 10;
SELECT COUNT(*) as total_orders FROM orders;
SELECT COUNT(*) as placed_orders FROM orders WHERE status = 'PLACED' AND assigned_to IS NULL;
SELECT COUNT(*) as assigned_orders FROM orders WHERE assigned_to IS NOT NULL;
