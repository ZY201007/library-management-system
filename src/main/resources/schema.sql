-- ============================================================
-- 图书馆管理系统 数据库脚本（MySQL 5.7+ / 8.x，InnoDB，utf8mb4）
-- 运行方式：mysql -u root -p < src/main/resources/schema.sql
-- 包含：6 张表、外键、检索字段索引、防重复借出的唯一约束、种子数据
-- ============================================================

DROP DATABASE IF EXISTS library_db;
CREATE DATABASE library_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_db;

-- 1. 分类表 -----------------------------------------------------
CREATE TABLE category (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE COMMENT '分类名称',
    description VARCHAR(200) COMMENT '分类描述',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书分类';

-- 2. 管理员表 ---------------------------------------------------
CREATE TABLE admin (
    id         INT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE COMMENT '登录名',
    password   VARCHAR(100) NOT NULL COMMENT '密码（课程设计示例，生产环境须加盐哈希）',
    real_name  VARCHAR(50)  COMMENT '姓名',
    phone      VARCHAR(20)  COMMENT '电话',
    role       VARCHAR(20)  NOT NULL DEFAULT 'LIBRARIAN' COMMENT 'ADMIN/LIBRARIAN',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '管理员';

-- 3. 读者表 -----------------------------------------------------
CREATE TABLE reader (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    reader_no   VARCHAR(32)  NOT NULL UNIQUE COMMENT '读者证号',
    name        VARCHAR(50)  NOT NULL COMMENT '姓名',
    gender      TINYINT      NOT NULL DEFAULT 0 COMMENT '0未知 1男 2女',
    phone       VARCHAR(20)  COMMENT '联系电话',
    email       VARCHAR(100) COMMENT '邮箱',
    reader_type VARCHAR(20)  NOT NULL DEFAULT '普通' COMMENT '读者类型',
    max_borrow  INT          NOT NULL DEFAULT 5 COMMENT '最大可借数量',
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 检索字段索引：读者证号（精确/前缀检索）
    INDEX idx_reader_no (reader_no),
    INDEX idx_reader_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '读者';

-- 4. 图书表 -----------------------------------------------------
CREATE TABLE book (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    isbn            VARCHAR(32)   NOT NULL COMMENT 'ISBN',
    title           VARCHAR(200)  NOT NULL COMMENT '书名',
    author          VARCHAR(100)  COMMENT '作者',
    publisher       VARCHAR(100)  COMMENT '出版社',
    category_id     INT           COMMENT '分类外键',
    total_stock     INT           NOT NULL DEFAULT 1 COMMENT '馆藏总册数',
    available_stock INT           NOT NULL DEFAULT 1 COMMENT '可借册数（借阅事务中原子扣减）',
    price           DECIMAL(10,2) COMMENT '定价',
    publish_year    INT           COMMENT '出版年份',
    location        VARCHAR(50)   COMMENT '馆藏位置',
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- 检索字段索引：title / author / isbn。
    -- 其中 idx_book_title 即基准测试里把 1 万条查询从 ~300ms 降到 ~40ms 的索引；
    -- 注意必须使用前缀匹配（LIKE 'Java%'）或等值查询才能命中 B+Tree 索引。
    INDEX idx_book_title  (title),
    INDEX idx_book_author (author),
    INDEX idx_book_isbn   (isbn),
    CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '图书';

-- 5. 借阅记录表 -------------------------------------------------
CREATE TABLE borrow_record (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    reader_id   INT           NOT NULL COMMENT '读者外键',
    book_id     INT           NOT NULL COMMENT '图书外键',
    copy_no     VARCHAR(64)   NOT NULL COMMENT '馆藏副本号（同一本书的多册副本各自唯一）',
    borrow_date DATE          NOT NULL COMMENT '借出日期',
    due_date    DATE          NOT NULL COMMENT '应还日期',
    return_date DATE          COMMENT '实际归还日期',
    renew_count INT           NOT NULL DEFAULT 0 COMMENT '续借次数',
    status      VARCHAR(16)   NOT NULL DEFAULT 'BORROWED' COMMENT 'BORROWED/RETURNED',
    fine_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '归还时计算的罚款',
    -- ★ 防重复借出的核心设计 ★
    -- 生成列：仅"在借"(BORROWED) 的记录写入非 NULL 的副本号，归还后自动变为 NULL。
    -- MySQL 唯一索引允许多个 NULL，因此：
    --   · 同一副本在借期间只能存在一条 BORROWED 记录；
    --   · 归还后再次借出不会产生冲突。
    -- 若并发重复借出同一副本，INSERT 会报 1062 错误，由 Service 层回滚事务并抛出业务异常。
    active_copy VARCHAR(64)
        GENERATED ALWAYS AS (CASE WHEN status = 'BORROWED' THEN copy_no ELSE NULL END) STORED,
    UNIQUE KEY uk_active_copy (active_copy),
    INDEX idx_borrow_reader (reader_id),
    INDEX idx_borrow_book   (book_id),
    INDEX idx_borrow_due    (due_date),
    CONSTRAINT fk_borrow_reader FOREIGN KEY (reader_id) REFERENCES reader(id),
    CONSTRAINT fk_borrow_book   FOREIGN KEY (book_id)   REFERENCES book(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '借阅记录';

-- 6. 罚款/逾期账单表 --------------------------------------------
CREATE TABLE fine (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    borrow_id    INT           NOT NULL COMMENT '借阅记录外键',
    reader_id    INT           NOT NULL COMMENT '读者外键',
    amount       DECIMAL(10,2) NOT NULL COMMENT '罚款金额',
    days_overdue INT           NOT NULL DEFAULT 0 COMMENT '逾期天数',
    paid         TINYINT       NOT NULL DEFAULT 0 COMMENT '0未缴 1已缴',
    paid_at      TIMESTAMP     NULL COMMENT '缴费时间',
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_fine_reader (reader_id),
    CONSTRAINT fk_fine_borrow FOREIGN KEY (borrow_id)    REFERENCES borrow_record(id),
    CONSTRAINT fk_fine_reader FOREIGN KEY (reader_id)    REFERENCES reader(id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = '逾期罚款账单';

-- ============================================================
-- 种子数据
-- ============================================================
INSERT INTO category(name, description) VALUES
    ('计算机', '计算机与软件工程类图书'),
    ('文学',   '中外文学名著与小说'),
    ('历史',   '历史与传记类图书'),
    ('数学',   '基础数学与应用数学');

INSERT INTO admin(username, password, real_name, phone, role) VALUES
    ('admin', '<CHANGE_ME>', '系统管理员', '13800000001', 'ADMIN'),
    ('lib',   '<CHANGE_ME>', '图书管理员', '13800000002', 'LIBRARIAN');

INSERT INTO reader(reader_no, name, gender, phone, email, reader_type, max_borrow) VALUES
    ('R20250001', '张三', 1, '13900000001', 'zhangsan@example.com', '普通', 5),
    ('R20250002', '李四', 2, '13900000002', 'lisi@example.com',     '教师', 10),
    ('R20250003', '王五', 1, '13900000003', 'wangwu@example.com',   '普通', 5);

INSERT INTO book(isbn, title, author, publisher, category_id, total_stock, available_stock, price, publish_year, location) VALUES
    ('978-7-111-00001-1', 'Java 程序设计',      '张三', '机械工业出版社', 1, 3, 3, 59.00, 2023, 'A区-01'),
    ('978-7-111-00002-2', 'Java 并发编程实战',  '李四', '人民邮电出版社', 1, 2, 2, 88.00, 2022, 'A区-02'),
    ('978-7-111-00003-3', '数据结构与算法',     '王五', '清华大学出版社', 1, 4, 4, 72.00, 2021, 'A区-03'),
    ('978-7-111-00004-4', '红楼梦',             '曹雪芹', '人民文学出版社', 2, 2, 2, 45.00, 2019, 'B区-01'),
    ('978-7-111-00005-5', '中国通史',           '范文澜', '人民出版社',     3, 2, 2, 128.00, 2020, 'C区-01');

-- 验证
SELECT 'category' AS tbl, COUNT(*) AS cnt FROM category
UNION ALL SELECT 'admin',        COUNT(*) FROM admin
UNION ALL SELECT 'reader',       COUNT(*) FROM reader
UNION ALL SELECT 'book',         COUNT(*) FROM book
UNION ALL SELECT 'borrow_record',COUNT(*) FROM borrow_record
UNION ALL SELECT 'fine',         COUNT(*) FROM fine;
