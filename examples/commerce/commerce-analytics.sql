-- # 电商经营分析 · 从交易到增长
-- 原创查询示例，MIT License；表结构对应同目录 commerce-schema.sql。
-- MySQL 8.0+；日期为固定演示区间，金额口径见各节说明。
-- 仅包含只读查询，不需要连接数据库即可体验注释导航。

-- ## 01 · 收入与订单
-- ### 月度收入趋势
-- #### 口径与时间边界
-- 统计已支付且未取消的订单；退货仅纳入已完成状态，按原订单归属月冲减。
WITH
params AS (
    SELECT DATE('2025-01-01') AS start_date,
           DATE('2026-01-01') AS end_date
),
-- #### 订单级退款预聚合
-- 先聚合再关联，避免一笔订单的多条退货记录放大订单金额。
refunds AS (
    SELECT order_id,
           SUM(return_amount) AS refund_amount,
           COUNT(*) AS return_count
    FROM oms_order_return_apply
    WHERE status = 2
    GROUP BY order_id
),
paid_orders AS (
    SELECT o.id,
           o.member_id,
           o.payment_time,
           COALESCE(o.pay_amount, 0) AS paid_amount,
           COALESCE(r.refund_amount, 0) AS refund_amount
    FROM oms_order o
    CROSS JOIN params p
    LEFT JOIN refunds r ON r.order_id = o.id
    WHERE o.delete_status = 0
      AND o.status IN (1, 2, 3)
      AND o.payment_time >= p.start_date
      AND o.payment_time < p.end_date
),
-- #### 月度汇总与环比窗口
monthly AS (
    SELECT DATE_FORMAT(payment_time, '%Y-%m') AS revenue_month,
           COUNT(*) AS paid_orders,
           COUNT(DISTINCT member_id) AS paying_members,
           SUM(paid_amount) AS gross_paid,
           SUM(refund_amount) AS refunds,
           SUM(paid_amount - refund_amount) AS net_paid
    FROM paid_orders
    GROUP BY DATE_FORMAT(payment_time, '%Y-%m')
),
comparison AS (
    SELECT monthly.*,
           LAG(net_paid) OVER (ORDER BY revenue_month) AS previous_net_paid,
           SUM(net_paid) OVER (
               ORDER BY revenue_month ROWS UNBOUNDED PRECEDING
           ) AS cumulative_net_paid
    FROM monthly
)
SELECT revenue_month,
       paid_orders,
       paying_members,
       ROUND(gross_paid, 2) AS gross_paid,
       ROUND(refunds, 2) AS refunds,
       ROUND(net_paid, 2) AS net_paid,
       ROUND(net_paid / NULLIF(paid_orders, 0), 2) AS net_order_value,
       ROUND(100 * (net_paid - previous_net_paid)
           / NULLIF(previous_net_paid, 0), 2) AS growth_pct,
       ROUND(cumulative_net_paid, 2) AS cumulative_net_paid
FROM comparison
ORDER BY revenue_month;
-- 环比比较相邻的有交易月份；需要补零月份时应接入日历维表。

-- ### 品类销售榜 · 每月 TOP 5
-- #### 明细收入与销量
-- real_amount 是订单行优惠后单价，乘以数量得到行收入；不含运费与售后冲减。
WITH category_sales AS (
    SELECT DATE_FORMAT(o.payment_time, '%Y-%m') AS sales_month,
           i.product_category_id,
           COUNT(DISTINCT o.id) AS order_count,
           SUM(i.product_quantity) AS units,
           SUM(i.real_amount * i.product_quantity) AS item_revenue
    FROM oms_order o
    JOIN oms_order_item i ON i.order_id = o.id
    WHERE o.status IN (1, 2, 3)
      AND o.delete_status = 0
      AND o.payment_time >= '2025-01-01'
      AND o.payment_time < '2026-01-01'
    GROUP BY DATE_FORMAT(o.payment_time, '%Y-%m'), i.product_category_id
),
-- #### 排名与月内收入占比
ranked AS (
    SELECT s.*,
           c.name AS category_name,
           ROW_NUMBER() OVER (
               PARTITION BY s.sales_month
               ORDER BY s.item_revenue DESC, s.product_category_id
           ) AS revenue_rank,
           SUM(s.item_revenue) OVER (
               PARTITION BY s.sales_month
           ) AS month_revenue
    FROM category_sales s
    LEFT JOIN pms_product_category c ON c.id = s.product_category_id
)
SELECT sales_month,
       category_name,
       revenue_rank,
       order_count,
       units,
       ROUND(item_revenue, 2) AS item_revenue,
       ROUND(100 * item_revenue / NULLIF(month_revenue, 0), 2) AS share_pct
FROM ranked
WHERE revenue_rank <= 5
ORDER BY sales_month, revenue_rank;

-- ## 02 · 会员增长与复购
-- ### 首购分群留存
-- #### 全量首购月份
-- 首购必须从完整订单历史计算，不能先裁剪报告区间。
WITH paid AS (
    SELECT member_id, payment_time, pay_amount
    FROM oms_order
    WHERE status IN (1, 2, 3)
      AND delete_status = 0
      AND payment_time IS NOT NULL
      AND payment_time < '2026-01-01'
),
first_purchase AS (
    SELECT member_id,
           CAST(DATE_FORMAT(MIN(payment_time), '%Y-%m-01') AS DATE) AS cohort_month
    FROM paid
    GROUP BY member_id
),
-- #### 月活去重与生命周期
member_activity AS (
    SELECT DISTINCT p.member_id,
           f.cohort_month,
           TIMESTAMPDIFF(
               MONTH, f.cohort_month,
               CAST(DATE_FORMAT(p.payment_time, '%Y-%m-01') AS DATE)
           ) AS month_index
    FROM paid p
    JOIN first_purchase f ON f.member_id = p.member_id
    WHERE f.cohort_month >= '2025-01-01'
),
cohort_size AS (
    SELECT cohort_month, COUNT(*) AS members
    FROM first_purchase
    WHERE cohort_month >= '2025-01-01'
    GROUP BY cohort_month
)
SELECT a.cohort_month,
       a.month_index,
       s.members AS cohort_members,
       COUNT(*) AS active_members,
       ROUND(100 * COUNT(*) / NULLIF(s.members, 0), 2) AS retention_pct
FROM member_activity a
JOIN cohort_size s ON s.cohort_month = a.cohort_month
GROUP BY a.cohort_month, a.month_index, s.members
ORDER BY a.cohort_month, a.month_index;

-- ### RFM 客户分层
-- #### 最近购买、频次与金额
-- 观察窗固定为 2025 年；近期购买越近、频次及金额越高，分数越高。
WITH member_metrics AS (
    SELECT member_id,
           DATEDIFF('2026-01-01', MAX(payment_time)) AS recency_days,
           COUNT(*) AS frequency,
           SUM(pay_amount) AS monetary
    FROM oms_order
    WHERE status IN (1, 2, 3)
      AND delete_status = 0
      AND payment_time >= '2025-01-01'
      AND payment_time < '2026-01-01'
    GROUP BY member_id
),
-- #### 窗口评分与业务标签
scored AS (
    SELECT m.*,
           NTILE(5) OVER (ORDER BY recency_days DESC, member_id) AS r_score,
           NTILE(5) OVER (ORDER BY frequency, member_id) AS f_score,
           NTILE(5) OVER (ORDER BY monetary, member_id) AS m_score
    FROM member_metrics m
),
segments AS (
    SELECT scored.*,
           CASE
               WHEN r_score >= 4 AND f_score >= 4 AND m_score >= 4
                   THEN '高价值活跃'
               WHEN r_score <= 2 AND m_score >= 4
                   THEN '高价值待召回'
               WHEN r_score >= 4 AND frequency = 1
                   THEN '首购待培育'
               WHEN f_score >= 4 THEN '稳定复购'
               ELSE '常规运营'
           END AS segment_name
    FROM scored
)
SELECT segment_name,
       COUNT(*) AS members,
       ROUND(AVG(recency_days), 1) AS average_recency,
       ROUND(AVG(frequency), 2) AS average_frequency,
       ROUND(SUM(monetary), 2) AS segment_revenue,
       ROUND(AVG(monetary), 2) AS revenue_per_member
FROM segments
GROUP BY segment_name
ORDER BY segment_revenue DESC;

-- ## 03 · 营销与转化
-- ### 优惠券领取到核销
-- #### 去重统计领取及核销
-- 按领取日期分群；这是使用率，不代表优惠券带来的因果增量。
WITH issued AS (
    SELECT coupon_id,
           COUNT(*) AS issued_count,
           COUNT(DISTINCT member_id) AS recipients,
           SUM(CASE WHEN use_status = 1 THEN 1 ELSE 0 END) AS used_count
    FROM sms_coupon_history
    WHERE create_time >= '2025-01-01'
      AND create_time < '2026-01-01'
    GROUP BY coupon_id
),
-- #### 订单收入独立聚合
coupon_orders AS (
    SELECT coupon_id,
           COUNT(*) AS paid_orders,
           SUM(pay_amount) AS paid_revenue,
           SUM(coupon_amount) AS coupon_discount
    FROM oms_order
    WHERE coupon_id IS NOT NULL
      AND status IN (1, 2, 3)
      AND delete_status = 0
      AND payment_time >= '2025-01-01'
      AND payment_time < '2026-01-01'
    GROUP BY coupon_id
)
SELECT c.id,
       c.name,
       i.issued_count,
       i.recipients,
       i.used_count,
       ROUND(100 * i.used_count / NULLIF(i.issued_count, 0), 2) AS usage_pct,
       COALESCE(o.paid_orders, 0) AS paid_orders,
       ROUND(COALESCE(o.paid_revenue, 0), 2) AS paid_revenue,
       ROUND(COALESCE(o.coupon_discount, 0), 2) AS coupon_discount
FROM issued i
JOIN sms_coupon c ON c.id = i.coupon_id
LEFT JOIN coupon_orders o ON o.coupon_id = i.coupon_id
ORDER BY paid_revenue DESC, c.id;
-- 两项时间口径独立：年内领取券的核销情况、年内支付订单的优惠券收入。

-- ## 04 · 库存与补货
-- ### SKU 库存覆盖天数
-- #### 近 30 天销售速度
WITH recent_sales AS (
    SELECT i.product_sku_id,
           SUM(i.product_quantity) AS units_30d,
           COUNT(DISTINCT o.id) AS orders_30d
    FROM oms_order o
    JOIN oms_order_item i ON i.order_id = o.id
    WHERE o.status IN (1, 2, 3)
      AND o.delete_status = 0
      AND o.payment_time >= '2025-12-02'
      AND o.payment_time < '2026-01-01'
    GROUP BY i.product_sku_id
),
-- #### 可用库存与补货建议
-- stock 扣减锁定量得到演示可售数；实际项目需核实库存扣减时点。
coverage AS (
    SELECT s.id AS sku_id,
           s.sku_code,
           p.name AS product_name,
           b.name AS brand_name,
           GREATEST(COALESCE(s.stock, 0) - COALESCE(s.lock_stock, 0), 0) AS available_stock,
           COALESCE(s.low_stock, p.low_stock, 0) AS warning_stock,
           COALESCE(r.units_30d, 0) AS units_30d,
           COALESCE(r.orders_30d, 0) AS orders_30d,
           COALESCE(r.units_30d, 0) / 30.0 AS daily_velocity
    FROM pms_sku_stock s
    JOIN pms_product p ON p.id = s.product_id
    LEFT JOIN pms_brand b ON b.id = p.brand_id
    LEFT JOIN recent_sales r ON r.product_sku_id = s.id
    WHERE p.delete_status = 0
      AND p.publish_status = 1
)
SELECT sku_id,
       sku_code,
       product_name,
       brand_name,
       available_stock,
       units_30d,
       orders_30d,
       ROUND(available_stock / NULLIF(daily_velocity, 0), 1) AS coverage_days,
       GREATEST(CEIL(daily_velocity * 14) - available_stock, 0) AS suggested_restock,
       CASE
           WHEN available_stock <= warning_stock THEN '低于警戒线'
           WHEN available_stock < daily_velocity * 7 THEN '不足 7 天'
           WHEN units_30d = 0 THEN '近期无销量'
           ELSE '库存正常'
       END AS stock_health
FROM coverage
ORDER BY suggested_restock DESC, sku_id;

-- ## 05 · 售后与数据质量
-- ### 商品退货率
-- #### 相同订单队列的销售与退货
-- 以年内支付订单为队列；退货可发生在队列期间之后，快照时间影响结果。
WITH eligible_orders AS (
    SELECT id
    FROM oms_order
    WHERE status IN (1, 2, 3)
      AND delete_status = 0
      AND payment_time >= '2025-01-01'
      AND payment_time < '2026-01-01'
),
sales AS (
    SELECT i.product_id,
           SUM(i.product_quantity) AS sold_units,
           SUM(i.real_amount * i.product_quantity) AS sold_amount
    FROM oms_order_item i
    JOIN eligible_orders o ON o.id = i.order_id
    GROUP BY i.product_id
),
returns AS (
    SELECT r.product_id,
           SUM(r.product_count) AS returned_units,
           SUM(r.return_amount) AS returned_amount
    FROM oms_order_return_apply r
    JOIN eligible_orders o ON o.id = r.order_id
    WHERE r.status = 2
    GROUP BY r.product_id
)
SELECT s.product_id,
       p.name,
       s.sold_units,
       COALESCE(r.returned_units, 0) AS returned_units,
       ROUND(100 * COALESCE(r.returned_units, 0)
           / NULLIF(s.sold_units, 0), 2) AS unit_return_pct,
       ROUND(COALESCE(r.returned_amount, 0), 2) AS returned_amount
FROM sales s
LEFT JOIN returns r ON r.product_id = s.product_id
LEFT JOIN pms_product p ON p.id = s.product_id
WHERE s.sold_units >= 10
ORDER BY unit_return_pct DESC, s.product_id;

-- ### 支付订单完整性巡检
-- #### 孤立明细与缺失支付时间
SELECT '孤立订单明细' AS issue_type,
       COUNT(*) AS issue_count
FROM oms_order_item i
LEFT JOIN oms_order o ON o.id = i.order_id
WHERE o.id IS NULL
UNION ALL
SELECT '已支付状态缺少支付时间', COUNT(*)
FROM oms_order
WHERE status IN (1, 2, 3)
  AND payment_time IS NULL
UNION ALL
SELECT '订单明细数量非正数', COUNT(*)
FROM oms_order_item
WHERE product_quantity <= 0 OR product_quantity IS NULL
UNION ALL
SELECT '库存锁定量大于库存', COUNT(*)
FROM pms_sku_stock
WHERE lock_stock > stock;
