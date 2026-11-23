package com.library.dao;

import com.library.model.BorrowRecord;
import com.library.util.DbUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** 借阅记录数据访问对象：借出/归还/续借均支持传入外部 Connection 以纳入同一事务。 */
public class BorrowRecordDao {

    private static final String COLS =
            "id,reader_id,book_id,copy_no,borrow_date,due_date,return_date,renew_count,status,fine_amount";

    /** 借出：在外部事务连接上插入记录（重复副本由 uk_active_copy 唯一约束拦截）。 */
    public int insert(BorrowRecord r, Connection conn) throws SQLException {
        String sql = "INSERT INTO borrow_record(reader_id,book_id,copy_no,borrow_date,due_date,renew_count,status) "
                + "VALUES(?,?,?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, r.getReaderId());
            ps.setInt(2, r.getBookId());
            ps.setString(3, r.getCopyNo());
            ps.setDate(4, Date.valueOf(r.getBorrowDate()));
            ps.setDate(5, Date.valueOf(r.getDueDate()));
            ps.setInt(6, r.getRenewCount() == null ? 0 : r.getRenewCount());
            ps.setString(7, r.getStatus() == null ? "BORROWED" : r.getStatus());
            int rows = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getInt(1));
                }
            }
            return rows;
        }
    }

    public BorrowRecord findById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM borrow_record WHERE id=?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /** 查询某副本当前是否在借（利用生成列 active_copy）。 */
    public BorrowRecord findActiveByCopyNo(String copyNo) throws SQLException {
        String sql = "SELECT " + COLS + " FROM borrow_record WHERE active_copy=? AND status='BORROWED'";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, copyNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<BorrowRecord> listByReader(int readerId) throws SQLException {
        String sql = "SELECT " + COLS + " FROM borrow_record WHERE reader_id=? ORDER BY id DESC";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, readerId);
            try (ResultSet rs = ps.executeQuery()) {
                return collect(rs);
            }
        }
    }

    public List<BorrowRecord> listActive() throws SQLException {
        String sql = "SELECT " + COLS + " FROM borrow_record WHERE status='BORROWED' ORDER BY due_date, id";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return collect(rs);
        }
    }

    public List<BorrowRecord> listAll() throws SQLException {
        String sql = "SELECT " + COLS + " FROM borrow_record ORDER BY id DESC";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return collect(rs);
        }
    }

    /** 统计读者当前在借数量，用于「最大借阅数量」业务校验。 */
    public int countActiveByReader(int readerId, Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM borrow_record WHERE reader_id=? AND status='BORROWED'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, readerId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /** 归还：状态改为 RETURNED 后，生成列 active_copy 自动变为 NULL，副本可再次借出。 */
    public int markReturned(int id, java.time.LocalDate returnDate, BigDecimal fine, Connection conn)
            throws SQLException {
        String sql = "UPDATE borrow_record SET status='RETURNED', return_date=?, fine_amount=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(returnDate));
            ps.setBigDecimal(2, fine);
            ps.setInt(3, id);
            return ps.executeUpdate();
        }
    }

    /** 续借：延长应还日期并累加续借次数。 */
    public int renew(int id, java.time.LocalDate newDueDate, int renewCount) throws SQLException {
        String sql = "UPDATE borrow_record SET due_date=?, renew_count=? WHERE id=?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(newDueDate));
            ps.setInt(2, renewCount);
            ps.setInt(3, id);
            return ps.executeUpdate();
        }
    }

    private static List<BorrowRecord> collect(ResultSet rs) throws SQLException {
        List<BorrowRecord> list = new ArrayList<BorrowRecord>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }

    private static BorrowRecord mapRow(ResultSet rs) throws SQLException {
        BorrowRecord r = new BorrowRecord();
        r.setId(rs.getInt("id"));
        r.setReaderId(rs.getInt("reader_id"));
        r.setBookId(rs.getInt("book_id"));
        r.setCopyNo(rs.getString("copy_no"));
        Date borrow = rs.getDate("borrow_date");
        Date due = rs.getDate("due_date");
        Date returned = rs.getDate("return_date");
        r.setBorrowDate(borrow == null ? null : borrow.toLocalDate());
        r.setDueDate(due == null ? null : due.toLocalDate());
        r.setReturnDate(returned == null ? null : returned.toLocalDate());
        r.setRenewCount(rs.getInt("renew_count"));
        r.setStatus(rs.getString("status"));
        r.setFineAmount(rs.getBigDecimal("fine_amount"));
        return r;
    }
}
