package com.library.service;

import com.library.config.AppConfig;
import com.library.dao.BookDao;
import com.library.dao.BorrowRecordDao;
import com.library.dao.FineDao;
import com.library.dao.ReaderDao;
import com.library.model.Book;
import com.library.model.BorrowRecord;
import com.library.model.Fine;
import com.library.model.Reader;
import com.library.util.DbUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDate;
import java.util.List;

/**
 * 借阅业务层：借出、归还、续借。
 *
 * <p>借出使用 JDBC 事务保证「库存扣减 + 借阅记录插入」原子性：
 * setAutoCommit(false) → 扣库存 → 插记录 → commit；任一步失败 rollback。
 * 同一副本重复借出会触发 uk_active_copy 唯一约束(1062)，回滚后转换为业务异常。</p>
 */
public class BorrowService {

    private final BookDao bookDao = new BookDao();
    private final ReaderDao readerDao = new ReaderDao();
    private final BorrowRecordDao borrowDao = new BorrowRecordDao();
    private final FineDao fineDao = new FineDao();

    /** 借出：事务内原子完成「库存 -1」与「新增借阅记录」。 */
    public BorrowRecord borrow(int readerId, int bookId, String copyNo) {
        if (copyNo == null || copyNo.trim().isEmpty()) {
            throw new ServiceException("馆藏副本号不能为空");
        }
        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            Reader reader = readerDao.findById(readerId);
            if (reader == null) {
                throw new ServiceException("读者不存在: " + readerId);
            }
            Book book = bookDao.findById(bookId);
            if (book == null) {
                throw new ServiceException("图书不存在: " + bookId);
            }
            if (borrowDao.countActiveByReader(readerId, conn) >= reader.getMaxBorrow()) {
                throw new ServiceException("该读者已达到最大借阅数量: " + reader.getMaxBorrow());
            }
            // 库存不足时抛业务异常，事务回滚，不会出现「有记录无库存」
            if (!bookDao.decreaseStock(bookId, conn)) {
                throw new ServiceException("库存不足，无法借出（图书ID=" + bookId + "）");
            }

            LocalDate today = LocalDate.now();
            BorrowRecord record = new BorrowRecord();
            record.setReaderId(readerId);
            record.setBookId(bookId);
            record.setCopyNo(copyNo.trim());
            record.setBorrowDate(today);
            record.setDueDate(today.plusDays(AppConfig.LOAN_DAYS));
            record.setStatus("BORROWED");
            record.setRenewCount(0);
            // 若此副本已在借，INSERT 违反 uk_active_copy，抛出 1062 唯一约束异常
            borrowDao.insert(record, conn);

            conn.commit();
            return record;
        } catch (SQLIntegrityConstraintViolationException e) {
            DbUtil.rollbackQuietly(conn);
            throw new DuplicateBorrowException("该馆藏副本已借出，违反唯一约束: " + copyNo, e);
        } catch (SQLException e) {
            DbUtil.rollbackQuietly(conn);
            if (isDuplicateKey(e)) {
                throw new DuplicateBorrowException("该馆藏副本已借出: " + copyNo, e);
            }
            throw new ServiceException("借阅失败: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            DbUtil.rollbackQuietly(conn);
            throw e;
        } finally {
            restoreAutoCommit(conn);
            DbUtil.close(conn);
        }
    }

    /** 归还：事务内计算逾期罚款、更新记录、回滚库存、生成账单。 */
    public BigDecimal returnBook(int borrowId) {
        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            conn.setAutoCommit(false);

            BorrowRecord record = borrowDao.findById(borrowId);
            if (record == null) {
                throw new ServiceException("借阅记录不存在: " + borrowId);
            }
            if (!"BORROWED".equals(record.getStatus())) {
                throw new ServiceException("该记录已归还或状态异常: " + borrowId);
            }

            LocalDate today = LocalDate.now();
            BigDecimal fine = FineCalculator.calculate(record.getDueDate(), today);
            int overdueDays = FineCalculator.overdueDays(record.getDueDate(), today);

            borrowDao.markReturned(borrowId, today, fine, conn);
            bookDao.increaseStock(record.getBookId(), conn);

            if (fine.compareTo(BigDecimal.ZERO) > 0) {
                Fine bill = new Fine();
                bill.setBorrowId(borrowId);
                bill.setReaderId(record.getReaderId());
                bill.setAmount(fine);
                bill.setDaysOverdue(overdueDays);
                fineDao.insert(bill, conn);
            }

            conn.commit();
            return fine;
        } catch (SQLException e) {
            DbUtil.rollbackQuietly(conn);
            throw new ServiceException("归还失败: " + e.getMessage(), e);
        } catch (RuntimeException e) {
            DbUtil.rollbackQuietly(conn);
            throw e;
        } finally {
            restoreAutoCommit(conn);
            DbUtil.close(conn);
        }
    }

    /** 续借：在应还日期上顺延一个借期，超过最大续借次数则拒绝。 */
    public LocalDate renew(int borrowId) {
        try {
            BorrowRecord record = borrowDao.findById(borrowId);
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
            LocalDate newDueDate = record.getDueDate().plusDays(AppConfig.LOAN_DAYS);
            if (borrowDao.renew(borrowId, newDueDate, used + 1) != 1) {
                throw new ServiceException("续借更新失败，请重试");
            }
            return newDueDate;
        } catch (SQLException e) {
            throw new ServiceException("续借失败: " + e.getMessage(), e);
        }
    }

    public List<BorrowRecord> listActive() {
        try {
            return borrowDao.listActive();
        } catch (SQLException e) {
            throw new ServiceException("查询在借记录失败: " + e.getMessage(), e);
        }
    }

    public List<BorrowRecord> listByReader(int readerId) {
        try {
            return borrowDao.listByReader(readerId);
        } catch (SQLException e) {
            throw new ServiceException("查询读者借阅记录失败: " + e.getMessage(), e);
        }
    }

    public List<BorrowRecord> listAll() {
        try {
            return borrowDao.listAll();
        } catch (SQLException e) {
            throw new ServiceException("查询借阅记录失败: " + e.getMessage(), e);
        }
    }

    /** MySQL 重复键：errorCode 1062，SQLState 以 23 开头。 */
    private static boolean isDuplicateKey(SQLException e) {
        return e.getErrorCode() == 1062
                || (e.getSQLState() != null && e.getSQLState().startsWith("23"));
    }

    private static void restoreAutoCommit(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }
}
