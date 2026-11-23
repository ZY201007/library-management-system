package com.library.dao;

import com.library.model.Admin;
import com.library.util.DbUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** 管理员数据访问对象：登录查询与管理员建档。 */
public class AdminDao {

    /** 按登录名查询管理员（示例项目使用明文口令，生产环境应改为加盐哈希）。 */
    public Admin findByUsername(String username) throws SQLException {
        String sql = "SELECT id,username,password,real_name,phone,role FROM admin WHERE username=?";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return mapRow(rs);
            }
        }
    }

    public int insert(Admin a) throws SQLException {
        String sql = "INSERT INTO admin(username,password,real_name,phone,role) VALUES(?,?,?,?,?)";
        try (Connection conn = DbUtil.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.getUsername());
            ps.setString(2, a.getPassword());
            ps.setString(3, a.getRealName());
            ps.setString(4, a.getPhone());
            ps.setString(5, a.getRole() == null ? "LIBRARIAN" : a.getRole());
            return ps.executeUpdate();
        }
    }

    private static Admin mapRow(ResultSet rs) throws SQLException {
        Admin a = new Admin();
        a.setId(rs.getInt("id"));
        a.setUsername(rs.getString("username"));
        a.setPassword(rs.getString("password"));
        a.setRealName(rs.getString("real_name"));
        a.setPhone(rs.getString("phone"));
        a.setRole(rs.getString("role"));
        return a;
    }
}
