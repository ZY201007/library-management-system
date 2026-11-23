package com.library.dao;

import com.library.model.Fine;
import com.library.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** 罚款账单数据访问对象。 */
public class FineDao {

    private static final String COLS = "id,borrow_id,reader_id,amount,days_overdue,paid,paid_at";

    /** 归还事务内生成逾期账单。 */
    public int insert(Fine f, Connection conn) throws SQLException {
        String sql = "INSERT INTO fine(borrow_id,reader_id,amount,days_overdue) VALUES(?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, f.getBorrowId());
            ps.setInt(2, f.getReaderId());
            ps.setBigDecimal(3, f.getAmount());
            ps.setInt(4, f.getDaysOverdue() == null ? 0 : f.getDaysOverdue());
            return ps.executeUpdate();
        }
    }

    public List<Fine> listUnpaid() throws SQLException {
        String sql = "SELECT " + COLS + " FROM fine WHERE paid=0 ORDER BY id";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return collect(rs);
        }
    }

    public List<Fine> listByReader(int readerId) throws SQLException {
        String sql = "SELECT " + COLS + " FROM fine WHERE reader_id=? ORDER BY id DESC";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, readerId);
            try (ResultSet rs = ps.executeQuery()) {
                return collect(rs);
            }
        }
    }

    /** 缴纳罚款：标记已缴并记录缴费时间。 */
    public int markPaid(int id) throws SQLException {
        String sql = "UPDATE fine SET paid=1, paid_at=? WHERE id=? AND paid=0";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            ps.setInt(2, id);
            return ps.executeUpdate();
        }
    }

    private static List<Fine> collect(ResultSet rs) throws SQLException {
        List<Fine> list = new ArrayList<Fine>();
        while (rs.next()) {
            Fine f = new Fine();
            f.setId(rs.getInt("id"));
            f.setBorrowId(rs.getInt("borrow_id"));
            f.setReaderId(rs.getInt("reader_id"));
            f.setAmount(rs.getBigDecimal("amount"));
            f.setDaysOverdue(rs.getInt("days_overdue"));
            f.setPaid(rs.getBoolean("paid"));
            Timestamp paidAt = rs.getTimestamp("paid_at");
            f.setPaidAt(paidAt == null ? null : paidAt.toLocalDateTime());
            list.add(f);
        }
        return list;
    }
}
