package com.library.dao;

import com.library.model.Book;
import com.library.util.DbUtil;
import com.library.util.SqlBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** 图书数据访问对象：只负责 SQL，不含业务判断。 */
public class BookDao {

    private static final String COLS =
            "id,isbn,title,author,publisher,category_id,total_stock,available_stock,price,publish_year,location";

    /** 新增图书并回填自增主键。 */
    public int insert(Book b) throws SQLException {
        int total = b.getTotalStock() == null ? 1 : b.getTotalStock();
        int available = b.getAvailableStock() == null ? total : b.getAvailableStock();
        String sql = "INSERT INTO book(isbn,title,author,publisher,category_id,total_stock,"
                + "available_stock,price,publish_year,location) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, b.getIsbn());
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getAuthor());
            ps.setString(4, b.getPublisher());
            ps.setObject(5, b.getCategoryId());
            ps.setInt(6, total);
            ps.setInt(7, available);
            ps.setObject(8, b.getPrice());
            ps.setObject(9, b.getPublishYear());
            ps.setString(10, b.getLocation());
            int rows = ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    b.setId(keys.getInt(1));
                }
            }
            return rows;
        }
    }

    /** 修改图书基本信息（不在此处调整 available_stock，库存变更只能由借阅事务完成）。 */
    public int update(Book b) throws SQLException {
        String sql = "UPDATE book SET isbn=?,title=?,author=?,publisher=?,category_id=?,"
                + "total_stock=?,price=?,publish_year=?,location=? WHERE id=?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getIsbn());
            ps.setString(2, b.getTitle());
            ps.setString(3, b.getAuthor());
            ps.setString(4, b.getPublisher());
            ps.setObject(5, b.getCategoryId());
            ps.setInt(6, b.getTotalStock() == null ? 1 : b.getTotalStock());
            ps.setObject(7, b.getPrice());
            ps.setObject(8, b.getPublishYear());
            ps.setString(9, b.getLocation());
            ps.setInt(10, b.getId());
            return ps.executeUpdate();
        }
    }

    public int delete(int id) throws SQLException {
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("DELETE FROM book WHERE id=?")) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    public Book findById(int id) throws SQLException {
        String sql = "SELECT " + COLS + " FROM book WHERE id=?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public List<Book> listAll() throws SQLException {
        String sql = "SELECT " + COLS + " FROM book ORDER BY id";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Book> list = new ArrayList<Book>();
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        }
    }

    /** 参数化检索 + 分页（前缀匹配，可命中 idx_book_title/author/isbn）。 */
    public List<Book> search(String field, String keyword, int offset, int size) throws SQLException {
        String fromWhere = "FROM book WHERE " + SqlBuilder.bookColumn(field) + " LIKE ?";
        String sql = SqlBuilder.appendPagination("SELECT " + COLS + " " + fromWhere + " ORDER BY id");
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, (keyword == null ? "" : keyword) + "%");
            ps.setInt(2, size);
            ps.setInt(3, offset);
            try (ResultSet rs = ps.executeQuery()) {
                List<Book> list = new ArrayList<Book>();
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
                return list;
            }
        }
    }

    /** 与 search 使用相同条件的总数统计，供分页计算总页数。 */
    public long count(String field, String keyword) throws SQLException {
        String fromWhere = "FROM book WHERE " + SqlBuilder.bookColumn(field) + " LIKE ?";
        try (Connection conn = DbUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(SqlBuilder.count(fromWhere))) {
            ps.setString(1, (keyword == null ? "" : keyword) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        }
    }

    /** 借阅事务内原子扣减库存；available_stock>0 条件防止超借（乐观锁式校验）。 */
    public boolean decreaseStock(int bookId, Connection conn) throws SQLException {
        String sql = "UPDATE book SET available_stock = available_stock - 1 "
                + "WHERE id = ? AND available_stock > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() == 1;
        }
    }

    /** 归还事务内回滚库存。 */
    public boolean increaseStock(int bookId, Connection conn) throws SQLException {
        String sql = "UPDATE book SET available_stock = available_stock + 1 "
                + "WHERE id = ? AND available_stock < total_stock";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() == 1;
        }
    }

    private static Book mapRow(ResultSet rs) throws SQLException {
        Book b = new Book();
        b.setId(rs.getInt("id"));
        b.setIsbn(rs.getString("isbn"));
        b.setTitle(rs.getString("title"));
        b.setAuthor(rs.getString("author"));
        b.setPublisher(rs.getString("publisher"));
        b.setCategoryId((Integer) rs.getObject("category_id"));
        b.setTotalStock(rs.getInt("total_stock"));
        b.setAvailableStock(rs.getInt("available_stock"));
        b.setPrice(rs.getBigDecimal("price"));
        int year = rs.getInt("publish_year");
        b.setPublishYear(rs.wasNull() ? null : year);
        b.setLocation(rs.getString("location"));
        return b;
    }
}
