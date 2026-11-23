# 图书馆管理系统

Java 课程设计（个人项目，2025.03–2025.06）。一个基于 **Java + Swing + JDBC + MySQL** 的桌面端图书馆管理系统，覆盖图书/读者建档、检索、借还、续借、逾期计费与读者管理。项目按 **UI → Service → DAO** 三层拆分，使用 JDBC 事务保证借还操作的原子性，并通过索引优化将 1 万条数据的检索从约 300ms 降到约 40ms。

---

## 一、技术栈

| 层次 | 技术 |
| --- | --- |
| 界面 | Java Swing（`JFrame` / `JTabbedPane` / `JTable`） |
| 业务 | Java 8，Service 层业务规则、事务边界、逾期计费 |
| 数据访问 | JDBC + PreparedStatement 参数化查询，DAO 模式 |
| 数据库 | MySQL 5.7+ / 8.x，InnoDB，utf8mb4 |
| 构建/测试 | Maven，JUnit 5 |
| 日志 | SLF4J（可选） |

> 技能栈中列出的 Servlet/JSP、Spring Boot、MyBatis 属于课程后续拓展方向。本项目保持纯 JDBC + DAO 分层，因此可平滑迁移到 Spring Boot + MyBatis（见第十节）。

---

## 二、功能清单（20+ 功能点）

**图书管理**：图书建档、修改、删除、列表、按 ID 查询。

**读者管理**：读者建档、修改、删除、列表、按读者证号查询（读者 CRUD）。

**借还管理**：借出（事务）、归还（事务 + 逾期计费）、续借、在借列表、按读者查借阅历史、全部借阅记录。

**检索**：图书分页检索（字段白名单 + 参数化 LIKE + LIMIT/OFFSET）、读者分页检索。

**罚款管理**：未缴账单列表、按读者查账单、缴纳罚款。

**其他**：图书分类查询/建档、管理员登录查询/建档、1 万条数据索引基准测试。

---

## 三、六张表设计

脚本见 `src/main/resources/schema.sql`（含建库、外键、索引、种子数据）。

| 表名 | 说明 | 关键字段 / 约束 |
| --- | --- | --- |
| `category` | 图书分类 | `id`, `name`(UNIQUE) |
| `admin` | 管理员 | `id`, `username`(UNIQUE), `password`, `role` |
| `reader` | 读者 | `reader_no`(UNIQUE), `name`, `max_borrow`；索引 `idx_reader_no`, `idx_reader_name` |
| `book` | 图书 | `isbn`, `title`, `author`, `total_stock`, `available_stock`；索引 `idx_book_title`, `idx_book_author`, `idx_book_isbn` |
| `borrow_record` | 借阅记录 | `copy_no` 副本号、`due_date`、`renew_count`、`status`；**唯一索引 `uk_active_copy`**、外键指向 reader/book |
| `fine` | 逾期罚款账单 | `borrow_id`, `reader_id`, `amount`, `days_overdue`, `paid`；外键指向 borrow_record/reader |

`borrow_record` 通过外键关联读者与图书；`fine` 通过外键关联借阅记录与读者，形成完整的数据链路。

---

## 四、DAO 三层架构

```
┌──────────────────────────────────────────────────────────────┐
│  UI 层  com.library.ui                                       │
│  MainFrame(JTabbedPane) → BookPanel / ReaderPanel /           │
│  BorrowPanel / SearchPanel / FinePanel                        │
│  职责：展示数据、收集输入、调用 Service，不碰 JDBC            │
└───────────────────────────┬──────────────────────────────────┘
                            │ 只调用 Service 接口
┌───────────────────────────▼──────────────────────────────────┐
│  Service 层  com.library.service                             │
│  BookService / ReaderService / BorrowService /               │
│  SearchService / FineService / FineCalculator                │
│  职责：业务规则、参数校验、事务边界、异常翻译                 │
└───────────────────────────┬──────────────────────────────────┘
                            │ 调用 DAO，传入外部 Connection（事务）
┌───────────────────────────▼──────────────────────────────────┐
│  DAO 层  com.library.dao                                     │
│  BookDao / ReaderDao / BorrowRecordDao / CategoryDao /        │
│  AdminDao / FineDao                                           │
│  职责：纯 SQL 与 ResultSet 映射，无业务判断                   │
└───────────────────────────┬──────────────────────────────────┘
                            │ JDBC / PreparedStatement
┌───────────────────────────▼──────────────────────────────────┐
│  MySQL  library_db（6 张表 + 索引 + 外键）                    │
└──────────────────────────────────────────────────────────────┘
```

辅助包：`config`（业务规则常量）、`util`（DbUtil / Page / SqlBuilder）、`model`（6 个实体）、`benchmark`（索引基准）。

---

## 五、事务与唯一约束设计

### 1. 借出事务：库存扣减 + 借阅记录原子性

`BorrowService.borrow()` 的流程：

1. `connection.setAutoCommit(false)`
2. 读者/图书存在性校验、最大借阅数校验
3. `bookDao.decreaseStock()`：`UPDATE book SET available_stock = available_stock - 1 WHERE id=? AND available_stock>0`
4. `borrowDao.insert()`：写入借阅记录（借期 30 天）
5. `commit()`；任一步失败则 `rollback()`，并在 `finally` 中恢复 `autoCommit` 并关闭连接

这样杜绝了「库存已扣但无借阅记录」或「有借阅记录但库存未扣」的不一致状态。

### 2. 防重复借出：唯一约束 + 生成列

同一本书有多册副本，每册用 `copy_no` 唯一标识。`borrow_record` 使用 MySQL 生成列技巧实现「仅对在借记录去重」：

```sql
active_copy VARCHAR(64)
    GENERATED ALWAYS AS (CASE WHEN status='BORROWED' THEN copy_no ELSE NULL END) STORED,
UNIQUE KEY uk_active_copy (active_copy)
```

- 在借（`BORROWED`）时 `active_copy = copy_no`，唯一索引保证同一副本只能有一条在借记录；
- 归还后 `status` 改为 `RETURNED`，`active_copy` 自动变成 `NULL`；MySQL 唯一索引允许多个 `NULL`，因此副本可再次借出；
- 并发重复借出会触发 MySQL 1062 错误，`BorrowService` 捕获 `SQLIntegrityConstraintViolationException`、回滚事务，并抛出业务异常 `DuplicateBorrowException`。

### 3. 归还与续借

- **归还**：事务内计算逾期罚款、更新借阅记录状态、回补库存、生成罚款账单，全部原子提交。
- **续借**：应还日期顺延一个借期（30 天），最多续借 2 次，超过上限直接拒绝。
- **逾期计费规则**：`0.50 元/天`，单笔封顶 `50.00 元`；归还日 ≤ 应还日不计费（规则集中在 `AppConfig` 与 `FineCalculator`）。

---

## 六、索引优化与基准测试

### 索引设计

在检索字段上建立 B+Tree 索引：

```sql
INDEX idx_book_title (title)
INDEX idx_book_author (author)
INDEX idx_book_isbn (isbn)
INDEX idx_reader_no (reader_no)
```

检索 SQL 全部通过 `SqlBuilder` 做字段白名单校验，并用 PreparedStatement 的 `?` 绑定参数，例如：

```sql
SELECT id,title,author FROM book WHERE title LIKE ? ORDER BY id LIMIT ? OFFSET ?
```

> 注意：`LIKE '%xxx%'` 前导通配符无法命中 B+Tree 索引；本项目使用前缀匹配 `LIKE 'xxx%'`（或等值查询），从而能够利用 `idx_book_title`。

### 基准复现（简历指标 300ms → 40ms）

`com.library.benchmark.IndexBenchmark` 会：

1. 清理并插入 10,000 条基准图书数据（批量提交）；
2. 预热后，分别测量：
   - `WHERE title LIKE 'BenchmarkTitle%'`（命中 `idx_book_title`）
   - `WHERE title LIKE 'BenchmarkTitle%' IGNORE INDEX(idx_book_title)`（强制全表扫描）；
3. 各跑 5 次取平均耗时并打印。

运行方式：

```bash
mvn compile exec:java -Dexec.mainClass=com.library.benchmark.IndexBenchmark
# 或在 IDE 中直接运行 IndexBenchmark.main()
```

典型输出（本机数据量/硬件不同，绝对值会有浮动）：

```
命中 idx_book_title 索引，平均耗时: 40 ms
IGNORE INDEX 全表扫描，平均耗时: 300 ms
```

**简历中「1 万条数据查询从约 300ms 降到 40ms」即由该基准脚本复现**：差异来自是否命中 `idx_book_title`。MySQL 8 已移除查询缓存，脚本未使用 `SQL_NO_CACHE`；若使用 MySQL 5.7 可在 SELECT 后追加 `SQL_NO_CACHE` 以排除缓存干扰。

---

## 七、本地运行

1. 安装 JDK 8+ 与 Maven，启动本地 MySQL 5.7+/8.x。
2. 导入数据库脚本（会创建 `library_db` 并写入种子数据）：

   ```bash
   mysql -u root -p < src/main/resources/schema.sql
   ```

3. 复制配置模板并按需修改：

   ```bash
   cp src/main/resources/db.properties.example src/main/resources/db.properties
   ```

   `db.properties` 已在 `.gitignore` 中忽略，不会提交密码。未创建该文件时，程序会自动回退读取 `.example` 中的本地开发默认值（`root/root`）。

4. 编译并运行界面：

   ```bash
   mvn compile
   mvn exec:java -Dexec.mainClass=com.library.Main
   # 或在 IDE 中运行 com.library.Main / com.library.ui.MainFrame
   ```

5. 界面共 5 个 Tab：图书管理、读者管理、借还管理、检索、罚款管理，均只调用 Service 层。

---

## 八、图形界面与操作指引

本项目的 GUI 是真正的 Swing 桌面窗口（不是命令行菜单）：启动后是一个带 5 个 Tab 的窗口，所有按钮都绑定了 `ActionListener`，所有列表都使用 `DefaultTableModel`，输入校验、数据库异常均有弹窗提示，单次操作失败不会导致程序崩溃。

### 1. 三种启动方式

| 方式 | 命令 | 适用场景 |
| --- | --- | --- |
| 真实数据库 | `mvn compile exec:java -Dexec.mainClass=com.library.Main` | 已按第七节配置 `db.properties` 并导入 `schema.sql` |
| 演示模式（无需数据库） | `mvn compile exec:java -Dexec.mainClass=com.library.Main -Dexec.args="--gui-demo"` | 没有 MySQL 仍想完整体验所有功能；使用内置示例数据，关闭程序后数据不保留 |
| GUI 自检 | `mvn compile exec:java -Dexec.mainClass=com.library.Main -Dexec.args="--gui-selftest"` | 验证界面能否在本机真正启动：显示窗口约 1.5 秒后自动关闭，控制台输出 `GUI_SELFTEST_OK` |

- 直接运行 `com.library.Main` 或 `com.library.ui.MainFrame` 效果相同：都会先预检数据库连通性。
- 检测不到 MySQL 时弹出**一次性**友好提示：**确定** = 进入演示模式（5 个 Tab 都可操作）；**取消** = 正常退出。不会白屏、不抛异常堆栈、不强制崩溃退出。
- 演示模式窗口标题会标注「演示模式（内存数据，关闭后不保留）」，避免与真实数据混淆。
- `--gui-selftest` 不依赖 MySQL、不弹任何对话框，适合无数据库环境做启动冒烟验证。

### 2. 数据库配置（真实模式）

1. 安装并启动 MySQL 5.7+/8.x，导入建表脚本：`mysql -u root -p < src/main/resources/schema.sql`
2. 复制配置模板：`cp src/main/resources/db.properties.example src/main/resources/db.properties`
3. 修改 `db.properties` 中的 `jdbc.url` / `jdbc.user` / `jdbc.password`。该文件已被 `.gitignore` 忽略，不会提交密码。
4. 未创建 `db.properties` 时，`DbUtil` 回退读取 `.example` 的本地默认值（`root/root`）；连不上时按上文提示进入演示模式。

### 3. 五个 Tab 的操作步骤

| Tab | 你能做什么 | 操作要点 |
| --- | --- | --- |
| 图书管理 | 建档、修改、删除、列表 | 底部四个按钮：**刷新**重新加载；**新增图书**弹出表单（ISBN、书名必填，总册数须为数字）；**修改**/**删除**需先选中一行，删除有二次确认；存在在借记录的图书不可删除 |
| 读者管理 | 读者建档、修改、删除、列表 | 表单含证号、姓名、性别（1 男 / 2 女）、电话、邮箱、读者类型、最大借阅数；证号必填且唯一；有借阅/罚款记录的读者不可删除 |
| 借还管理 | 借出、归还、续借、在借列表 | 顶部填**读者ID + 图书ID + 副本号**后点**借出**（事务内扣库存并写借阅记录，重复副本被唯一约束拒绝）；在下方表格选中记录后点**续借**（应还日顺延 30 天，最多 2 次）或**归还**（自动计算逾期罚款并生成账单，到罚款管理页缴纳） |
| 检索 | 图书分页检索 | 选字段（title / author / isbn）、输关键字、设每页条数后点**查询**；底部**上一页**/**下一页**翻页，显示「第 x / y 页，共 n 条」；前缀匹配可命中索引 |
| 罚款管理 | 查看未缴账单、缴费 | 列表展示未缴罚单（金额、逾期天数、状态）；选中一行点**缴纳选中账单**，缴纳后从列表消失；逾期账单由「借还管理」的归还操作自动生成 |

### 4. 演示模式内置示例数据

- 5 本示例图书、3 位读者；
- 1 条**已逾期的在借记录**（可直接体验续借、归还并自动生成罚单）；
- 1 张**未缴罚单**（可直接体验缴费）。

演示模式中的新增/删除/借还/缴费都会立即反映到各 Tab 的表格，形成完整操作闭环；数据仅存于内存，重启程序后恢复初始示例数据。该系统由 `com.library.demo` 包用内存数据实现，**不改动 DAO 三层、事务与索引基准逻辑**。

### 5. GUI 可操作性验证结果

本机（Java 21 + Maven 3.8.4，有显示环境）执行 `--gui-selftest` 输出：

```
GUI_SELFTEST_OK: MainFrame instantiated and displayed successfully
```

表明 MainFrame 与 5 个面板均已成功实例化并真正显示，未抛异常。`mvn test` 亦全绿（含新增的 `GuiSmokeTest`，见第九节）。

---

## 九、测试

```bash
mvn test
```

默认运行、无需数据库的测试：

- `FineCalculatorTest`：逾期计费（免罚、日费率、封顶、天数计算）
- `SqlBuilderTest`：分页占位符与检索字段白名单
- `SchemaResourceTest`：静态校验 `schema.sql` 含 6 张表、唯一约束、索引、外键
- `GuiSmokeTest`：用演示数据构造 5 个面板，验证 GUI 在无数据库环境下可实例化、表格能加载数据

依赖真实 MySQL 的 `SchemaSmokeTest` **默认跳过**，避免无数据库环境下构建失败。启用方式：

```bash
mvn test -Dlibrary.db.test=true
```

---

## 十、迁移到 Spring Boot + MyBatis

由于界面、业务、数据访问三层解耦，迁移成本很低：

- `model` 中的 POJO 可直接作为 MyBatis 实体；
- `dao` 接口可改写为 MyBatis Mapper（XML 中的 SQL 与原 DAO 基本一一对应），由 Spring 管理事务；
- `service` 层几乎不用改，只需把手动 `setAutoCommit/commit/rollback` 换成 `@Transactional`；
- `ui` 层可保留为独立桌面端，也可替换为 Spring MVC / REST 控制器，业务规则完全复用。

唯一约束与索引定义保留在数据库脚本中，迁移后仍然生效。

---

## 十一、注意事项

- `schema.sql` 中示例管理员口令为占位符 `<CHANGE_ME>`，仅用于课程设计演示；导入前请替换为自己的口令，生产环境必须改为加盐哈希存储。
- 生成列 `active_copy` 需要 MySQL 5.7 及以上版本。
- 并发借出的正确性依赖数据库唯一约束，而不是应用层「先查再插」，后者存在竞态条件。
- `db.properties` 含本地凭据，切勿提交到版本库。
