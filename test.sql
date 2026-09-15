
-- 创建名为 "products" 的表，用于存储产品信息
CREATE TABLE products (
    id INT PRIMARY KEY AUTO_INCREMENT, -- 产品ID，整型，主键，自动递增
    name VARCHAR(255) NOT NULL,      -- 产品名称，字符串类型，不允许为空
    description TEXT,                 -- 产品描述，长文本类型
    price DECIMAL(10, 2) NOT NULL,    -- 产品价格，十进制类型，总共10位，小数点后2位，不允许为空
    stock_quantity INT NOT NULL DEFAULT 0, -- 库存数量，整型，不允许为空，默认值为0
    category_id INT,                  -- 产品类别ID，整型，外键，关联到 "categories" 表
    supplier_id INT,                  -- 供应商ID，整型，外键，关联到 "suppliers" 表
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, -- 创建时间，时间戳类型，默认值为当前时间
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP -- 更新时间，时间戳类型，默认值为当前时间，并在更新时自动更新
);

-- 创建名为 "categories" 的表，用于存储产品类别信息
CREATE TABLE categories (
    id INT PRIMARY KEY AUTO_INCREMENT, -- 类别ID，整型，主键，自动递增
    name VARCHAR(100) NOT NULL UNIQUE -- 类别名称，字符串类型，不允许为空，且唯一
);

-- 创建名为 "suppliers" 的表，用于存储供应商信息
CREATE TABLE suppliers (
    id INT PRIMARY KEY AUTO_INCREMENT, -- 供应商ID，整型，主键，自动递增
    name VARCHAR(255) NOT NULL,      -- 供应商名称，字符串类型，不允许为空
    contact_person VARCHAR(100),      -- 联系人，字符串类型
    phone_number VARCHAR(20),         -- 电话号码，字符串类型
    email VARCHAR(100) UNIQUE        -- 邮箱，字符串类型，唯一
);

-- 向 "categories" 表中插入一条新的类别记录
INSERT INTO categories (name) VALUES ('电子产品');

-- 向 "products" 表中插入一条新的产品记录
INSERT INTO products (name, description, price, stock_quantity, category_id)
VALUES ('智能手机', '最新款智能手机', 599.99, 100, 1);

-- 从 "products" 表中查询所有产品的信息
SELECT * FROM products;

-- 从 "products" 表中查询名称为 "智能手机" 的产品信息
SELECT * FROM products WHERE name = '智能手机';

-- 从 "products" 表中查询价格大于 500 的产品名称和价格
SELECT name, price FROM products WHERE price > 500;

-- 更新 "products" 表中 ID 为 1 的产品的库存数量为 150
UPDATE products SET stock_quantity = 150 WHERE id = 1;

-- 更新 "products" 表中所有价格低于 550 的产品的价格增加 10%
UPDATE products SET price = price * 1.1 WHERE price < 550;

-- 从 "products" 表中删除 ID 为 1 的产品记录
DELETE FROM products WHERE id = 1;

-- 查询 "products" 表中每个类别的产品数量
SELECT c.name AS category_name, COUNT(p.id) AS product_count
FROM categories c
LEFT JOIN products p ON c.id = p.category_id
GROUP BY c.name;

-- 查询 "products" 表中库存数量低于 50 的产品名称和库存数量
SELECT name, stock_quantity FROM products WHERE stock_quantity < 50;

-- 查询 "products" 表中价格最高的 3 个产品
SELECT name, price FROM products ORDER BY price DESC LIMIT 3;

-- 查询 "products" 表中属于 "电子产品" 类别并且库存大于 0 的产品名称和价格
SELECT p.name, p.price
FROM products p
JOIN categories c ON p.category_id = c.id
WHERE c.name = '电子产品' AND p.stock_quantity > 0;

-- 向 "suppliers" 表中插入一条新的供应商记录
INSERT INTO suppliers (name, contact_person, phone_number, email)
VALUES ('ABC 公司', '张三', '138XXXXXXXX', 'zhangsan@abc.com');

-- 将 "products" 表中所有 "supplier_id" 为 NULL 的产品更新为供应商 ID 为 1
UPDATE products SET supplier_id = 1 WHERE supplier_id IS NULL;

-- 查询 "products" 表中所有产品的名称以及其所属的类别名称和供应商名称
SELECT p.name AS product_name, c.name AS category_name, s.name AS supplier_name
FROM products p
LEFT JOIN categories c ON p.category_id = c.id
LEFT JOIN suppliers s ON p.supplier_id = s.id;

-- 查询 "products" 表中每个供应商提供的产品数量，并按照产品数量降序排列
SELECT s.name AS supplier_name, COUNT(p.id) AS product_count
FROM suppliers s
LEFT JOIN products p ON s.id = p.supplier_id
GROUP BY s.name
ORDER BY product_count DESC;

-- 查询 "products" 表中价格在 500 到 1000 之间的产品名称和价格（包含 500 和 1000）
SELECT name, price FROM products WHERE price BETWEEN 500 AND 1000;

-- 查询 "products" 表中名称包含 "手机" 关键字的产品信息
SELECT * FROM products WHERE name LIKE '%手机%';

-- 删除 "products" 表中所有库存为 0 的产品
DELETE FROM products WHERE stock_quantity = 0;

-- 修改 "products" 表，添加一个名为 "weight" 的 DECIMAL(5, 2) 类型的列
ALTER TABLE products ADD COLUMN weight DECIMAL(5, 2);

-- 修改 "products" 表，将 "description" 列的数据类型修改为 MEDIUMTEXT
ALTER TABLE products MODIFY COLUMN description MEDIUMTEXT;
