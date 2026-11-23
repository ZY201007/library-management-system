package com.library.demo;

import com.library.config.AppConfig;
import com.library.model.Book;
import com.library.model.Fine;
import com.library.model.Reader;
import com.library.service.FineCalculator;
import com.library.service.ServiceException;
import com.library.util.Page;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示模式内存数据仓库：不依赖 MySQL，内置示例数据，所有操作在内存中生效。
 *
 * <p>用途：① 无数据库时仍能让 GUI 真正"点起来"（可增删改查，关闭程序后数据不保留）；
 * ② --gui-selftest 用假数据构造界面，验证 GUI 可启动。业务规则（借期、续借上限、
 * 逾期费率、封顶金额、前缀匹配分页）与真实 Service 保持一致。</p>
 *
 * <p>本类只被 demo 包使用，不改动 DAO 三层、事务与索引基准逻辑。</p>
 */
public final class DemoData {

    private static final DemoData INSTANCE = new DemoData();

    private final Map<Integer, Book> books = new LinkedHashMap<Integer, Book>();
    private final Map<Integer, Reader> readers = new LinkedHashMap<Integer, Reader>();
    private final Map<Integer, com.library.model.BorrowRecord> borrows =
            new LinkedHashMap<Integer, com.library.model.BorrowRecord>();
    private final Map<Integer, Fine> fines = new LinkedHashMap<Integer, Fine>();

    private int bookSeq = 0;
    private int readerSeq = 0;
    private int borrowSeq = 0;
    private int fineSeq = 0;

    private DemoData() {
        seed();
    }

    public static DemoData get() {
        return INSTANCE;
    }

    /** 初始化示例数据：5 本书、3 位读者、1 条在借（已逾期）、1 张未缴罚单。 */
    private void seed() {
        Book b1 = newBook("978-7-111-21379-9", "Java 核心技术（卷 I）", "Cay S. Horstmann",
                "机械工业出版社", 5, new BigDecimal("128.00"), 2022, "A-01");
        newBook("978-7-115-40531-6", "Effective Java", "Joshua Bloch",
                "人民邮电出版社", 3, new BigDecimal("99.00"), 2021, "A-02");
        Book b3 = newBook("978-7-121-12345-6", "深入理解 Java 虚拟机", "周志明",
                "电子工业出版社", 4, new BigDecimal("109.00"), 2020, "B-01");
        newBook("978-7-302-45678-9", "数据库系统概念", "Abraham Silberschatz",
                "清华大学出版社", 6, new BigDecimal("88.00"), 2019, "B-02");
        newBook("978-7-111-45678-1", "计算机网络：自顶向下方法", "James F. Kurose",
                "机械工业出版社", 2, new BigDecimal("75.00"), 2018, "C-01");

        Reader r1 = newReader("R2025001", "张三", 1, "13800000001",
                "zhangsan@example.com", "学生", 5);
        Reader r2 = newReader("R2025002", "李四", 2, "13800000002",
                "lisi@example.com", "学生", 5);
        newReader("R2025003", "王五", 1, "13800000003",
                "wangwu@example.com", "教师", 10);

        // 在借记录：已逾期 10 天，可在「借还管理」中续借/归还
        LocalDate borrowed = LocalDate.now().minusDays(40);
        com.library.model.BorrowRecord active = new com.library.model.BorrowRecord();
        active.setId(++borrowSeq);
        active.setReaderId(r1.getId());
        active.setBookId(b1.getId());
        active.setCopyNo("C-001");
        active.setBorrowDate(borrowed);
        active.setDueDate(borrowed.plusDays(AppConfig.LOAN_DAYS));
        active.setStatus("BORROWED");
        active.setRenewCount(0);
        borrows.put(active.getId(), active);
        b1.setAvailableStock(b1.getAvailableStock() - 1);

        // 已归还且产生逾期罚款的记录，对应一张未缴罚单，可在「罚款管理」中缴纳
        LocalDate oldBorrow = LocalDate.now().minusDays(60);
        LocalDate oldDue = oldBorrow.plusDays(AppConfig.LOAN_DAYS);
        com.library.model.BorrowRecord returned = new com.library.model.BorrowRecord();
        returned.setId(++borrowSeq);
        returned.setReaderId(r2.getId());
        returned.setBookId(b3.getId());
        returned.setCopyNo("C-003");
        returned.setBorrowDate(oldBorrow);
        returned.setDueDate(oldDue);
        returned.setReturnDate(oldDue.plusDays(20));
        returned.setStatus("RETURNED");
        returned.setRenewCount(0);
        BigDecimal amount = FineCalculator.calculate(oldDue, returned.getReturnDate());
        returned.setFineAmount(amount);
        borrows.put(returned.getId(), returned);

        Fine fine = new Fine();
        fine.setId(++fineSeq);
        fine.setBorrowId(returned.getId());
        fine.setReaderId(r2.getId());
        fine.setAmount(amount);
        fine.setDaysOverdue(FineCalculator.overdueDays(oldDue, returned.getReturnDate()));
        fine.setPaid(false);
        fines.put(fine.getId(), fine);
    }

    private Book newBook(String isbn, String title, String author, String publisher,
                         int total, BigDecimal price, int year, String location) {
        Book b = new Book();
        b.setId(++bookSeq);
        b.setIsbn(isbn);
        b.setTitle(title);
        b.setAuthor(author);
        b.setPublisher(publisher);
        b.setCategoryId(1);
        b.setTotalStock(total);
        b.setAvailableStock(total);
        b.setPrice(price);
        b.setPublishYear(year);
        b.setLocation(location);
        books.put(b.getId(), b);
        return b;
    }

    private Reader newReader(String no, String name, int gender, String phone,
                             String email, String type, int maxBorrow) {
        Reader r = new Reader();
        r.setId(++readerSeq);
        r.setReaderNo(no);
        r.setName(name);
        r.setGender(gender);
        r.setPhone(phone);
        r.setEmail(email);
        r.setReaderType(type);
        r.setMaxBorrow(maxBorrow);
        readers.put(r.getId(), r);
        return r;
    }

    // ---------------- 图书 ----------------

    public synchronized List<Book> listBooks() {
        return new ArrayList<Book>(books.values());
    }

    public synchronized int addBook(Book b) {
        b.setId(++bookSeq);
        if (b.getTotalStock() == null || b.getTotalStock() < 1) {
            b.setTotalStock(1);
        }
        if (b.getAvailableStock() == null) {
            b.setAvailableStock(b.getTotalStock());
        }
        books.put(b.getId(), b);
        return b.getId();
    }

    public synchronized boolean updateBook(Book b) {
        Book old = books.get(b.getId());
        if (old == null) {
            return false;
        }
        // 库存只能由借阅事务变更，修改基本信息时保留原库存
        b.setAvailableStock(old.getAvailableStock());
        books.put(b.getId(), b);
        return true;
    }

    public synchronized boolean deleteBook(int id) {
        for (com.library.model.BorrowRecord r : borrows.values()) {
            if ("BORROWED".equals(r.getStatus()) && r.getBookId() == id) {
                return false;
            }
        }
        return books.remove(id) != null;
    }

    public synchronized Book findBook(int id) {
        return books.get(id);
    }

    // ---------------- 读者 ----------------

    public synchronized List<Reader> listReaders() {
        return new ArrayList<Reader>(readers.values());
    }

    public synchronized int addReader(Reader r) {
        r.setId(++readerSeq);
        readers.put(r.getId(), r);
        return r.getId();
    }

    public synchronized boolean updateReader(Reader r) {
        if (!readers.containsKey(r.getId())) {
            return false;
        }
        readers.put(r.getId(), r);
        return true;
    }

    public synchronized boolean deleteReader(int id) {
        for (com.library.model.BorrowRecord br : borrows.values()) {
            if (br.getReaderId() == id) {
                return false;
            }
        }
        for (Fine f : fines.values()) {
            if (f.getReaderId() == id) {
                return false;
            }
        }
        return readers.remove(id) != null;
    }

    public synchronized Reader findReader(int id) {
        return readers.get(id);
    }

    // ---------------- 借阅 ----------------

    public synchronized List<com.library.model.BorrowRecord> listActiveBorrows() {
        List<com.library.model.BorrowRecord> list = new ArrayList<com.library.model.BorrowRecord>();
        for (com.library.model.BorrowRecord r : borrows.values()) {
            if ("BORROWED".equals(r.getStatus())) {
                list.add(r);
            }
        }
        return list;
    }

    private int countActiveByReader(int readerId) {
        int count = 0;
        for (com.library.model.BorrowRecord r : borrows.values()) {
            if ("BORROWED".equals(r.getStatus()) && r.getReaderId() == readerId) {
                count++;
            }
        }
        return count;
    }

    public synchronized com.library.model.BorrowRecord borrow(int readerId, int bookId, String copyNo) {
        Reader reader = readers.get(readerId);
        if (reader == null) {
            throw new ServiceException("读者不存在: " + readerId);
        }
        Book book = books.get(bookId);
        if (book == null) {
            throw new ServiceException("图书不存在: " + bookId);
        }
        if (countActiveByReader(readerId) >= reader.getMaxBorrow()) {
            throw new ServiceException("该读者已达到最大借阅数量: " + reader.getMaxBorrow());
        }
        if (book.getAvailableStock() == null || book.getAvailableStock() <= 0) {
            throw new ServiceException("库存不足，无法借出（图书ID=" + bookId + "）");
        }
        for (com.library.model.BorrowRecord r : borrows.values()) {
            if ("BORROWED".equals(r.getStatus()) && copyNo.equals(r.getCopyNo())) {
                throw new ServiceException("该馆藏副本已借出: " + copyNo);
            }
        }

        book.setAvailableStock(book.getAvailableStock() - 1);
        LocalDate today = LocalDate.now();
        com.library.model.BorrowRecord record = new com.library.model.BorrowRecord();
        record.setId(++borrowSeq);
        record.setReaderId(readerId);
        record.setBookId(bookId);
        record.setCopyNo(copyNo.trim());
        record.setBorrowDate(today);
        record.setDueDate(today.plusDays(AppConfig.LOAN_DAYS));
        record.setStatus("BORROWED");
        record.setRenewCount(0);
        borrows.put(record.getId(), record);
        return record;
    }

    public synchronized BigDecimal returnBook(int borrowId) {
        com.library.model.BorrowRecord record = borrows.get(borrowId);
        if (record == null) {
            throw new ServiceException("借阅记录不存在: " + borrowId);
        }
        if (!"BORROWED".equals(record.getStatus())) {
            throw new ServiceException("该记录已归还或状态异常: " + borrowId);
        }
        LocalDate today = LocalDate.now();
        BigDecimal fine = FineCalculator.calculate(record.getDueDate(), today);
        int daysOverdue = FineCalculator.overdueDays(record.getDueDate(), today);

        record.setStatus("RETURNED");
        record.setReturnDate(today);
        record.setFineAmount(fine);
        Book book = books.get(record.getBookId());
        if (book != null) {
            book.setAvailableStock(book.getAvailableStock() + 1);
        }
        if (fine.compareTo(BigDecimal.ZERO) > 0) {
            Fine bill = new Fine();
            bill.setId(++fineSeq);
            bill.setBorrowId(borrowId);
            bill.setReaderId(record.getReaderId());
            bill.setAmount(fine);
            bill.setDaysOverdue(daysOverdue);
            bill.setPaid(false);
            fines.put(bill.getId(), bill);
        }
        return fine;
    }

    public synchronized LocalDate renew(int borrowId) {
        com.library.model.BorrowRecord record = borrows.get(borrowId);
        if (record == null) {
            throw new ServiceException("借阅记录不存在: " + borrowId);
        }
        if (!"BORROWED".equals(record.getStatus())) {
            throw new ServiceException("该记录已归还，无法续借: " + borrowId);
        }
        int used = record.getRenewCount() == null ? 0 : record.getRenewCount();
        if (used >= AppConfig.MAX_RENEW_TIMES) {
            throw new ServiceException("续借次数已达上限 " + AppConfig.MAX_RENEW_TIMES + " 次");
        }
        LocalDate newDue = record.getDueDate().plusDays(AppConfig.LOAN_DAYS);
        record.setDueDate(newDue);
        record.setRenewCount(used + 1);
        return newDue;
    }

    // ---------------- 罚款 ----------------

    public synchronized List<Fine> listUnpaidFines() {
        List<Fine> list = new ArrayList<Fine>();
        for (Fine f : fines.values()) {
            if (!Boolean.TRUE.equals(f.getPaid())) {
                list.add(f);
            }
        }
        return list;
    }

    public synchronized boolean payFine(int id) {
        Fine fine = fines.get(id);
        if (fine == null) {
            return false;
        }
        fine.setPaid(true);
        fine.setPaidAt(LocalDateTime.now());
        return true;
    }

    // ---------------- 检索（字段白名单 + 前缀匹配 + 分页） ----------------

    public synchronized Page<Book> searchBooks(String field, String keyword, int pageNo, int pageSize) {
        String kw = keyword == null ? "" : keyword;
        List<Book> matched = new ArrayList<Book>();
        for (Book b : books.values()) {
            String value = null;
            if ("title".equals(field)) {
                value = b.getTitle();
            } else if ("author".equals(field)) {
                value = b.getAuthor();
            } else if ("isbn".equals(field)) {
                value = b.getIsbn();
            }
            if (value != null && value.startsWith(kw)) {
                matched.add(b);
            }
        }

        Page<Book> page = new Page<Book>(pageNo, pageSize);
        page.setTotal(matched.size());
        int from = page.getOffset();
        int to = Math.min(from + page.getPageSize(), matched.size());
        List<Book> records = new ArrayList<Book>();
        if (from < to) {
            for (int i = from; i < to; i++) {
                records.add(matched.get(i));
            }
        }
        page.setRecords(records);
        return page;
    }
}
