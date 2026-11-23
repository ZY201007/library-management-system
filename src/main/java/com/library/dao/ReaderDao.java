package com.library.dao;

import com.library.model.Reader;
import com.library.util.DbUtil;
import com.library.util.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** 读者数据访问对象：读者建档、CRUD 与分页检索。 */
public class ReaderDao {

    private static final String COLS = "id,reader_no,name,gender,phone,email,reader_type,max_borrow";

    public int insert(Reader r) throws SQLException {
        String sql = "INSERT INTO reader(reader_no,name,gender,phone,email,reader_type,max_borrow) "
                + "VALUES(?,?,?,?,?,?,?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(ps, r, false);
            int rows = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    r.setId(keys.getInt(1));
                }
            }
            return rows;
        }
    }

    public int update(Reader r) throws SQLException {
        String sql = "UPDATE reader SET reader_no=?,name=?,gender=?,phone=?,email=?,reader_type=?,"
                + "max_borrow=? WHERE id=?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            bind(ps, r, true);
            return ps.executeUpdate();
        }
    }

    public int delete(int id) throws SQLException {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM reader WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    public Reader findById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM reader WHERE id=?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Reader findByReaderNo(String readerNo) throws SQLException {
        String sql = "SELECT " + COLS + " FROM reader WHERE reader_no=?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, readerNo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Reader> listAll() throws SQLException {
        String sql = "SELECT " + COLS + " FROM reader ORDER BY id";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Reader> list = new ArrayList<Reader>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    public List<Reader> search(String field, String keyword, int offset, int size) throws SQLException {
        String fromWhere = "FROM reader WHERE " + SqlBuilder.readerColumn(field) + " LIKE ?";
        String sql = SqlBuilder.appendPagination("SELECT " + COLS + " " + fromWhere + " ORDER BY id");
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (keyword == null ? "" : keyword) + "%");
            ps.setInt(2, size);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Reader> list = new ArrayList<Reader>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    public long count(String field, String keyword) throws SQLException {
        String fromWhere = "FROM reader WHERE " + SqlBuilder.readerColumn(field) + " LIKE ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(SqlBuilder.count(fromWhere))) {
            ps.setString(1, (keyword == null ? "" : keyword) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    private static void bind(PreparedStatement ps, Reader r, boolean withId) throws SQLException {
        int i = 1;
        ps.setString(i++, r.getReaderNo());
        ps.setString(i++, r.getName());
        ps.setObject(i++, r.getGender());
        ps.setString(i++, r.getPhone());
        ps.setString(i++, r.getEmail());
        ps.setString(i++, r.getReaderType());
        ps.setInt(i++, r.getMaxBorrow() == null ? 5 : r.getMaxBorrow());
        if (withId) {
            ps.setInt(i, r.getId());
        }
    }

    private static Reader mapRow(ResultSet rs) throws SQLException {
        Reader r = new Reader();
        r.setId(rs.getInt("id"));
        r.setReaderNo(rs.getString("reader_no"));
        r.setName(rs.getString("name"));
        int gender = rs.getInt("gender");
        r.setGender(rs.wasNull() ? null : gender);
        r.setPhone(rs.getString("phone"));
        r.setEmail(rs.getString("email"));
        r.setReaderType(rs.getString("reader_type"));
        r.setMaxBorrow(rs.getInt("max_borrow"));
        return r;
    }
}
