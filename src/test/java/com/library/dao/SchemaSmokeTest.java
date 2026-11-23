package com.library.dao;

import com.library.util.DbUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 依赖真实 MySQL 的 Schema 烟测。默认跳过，避免 `mvn test` 因没有数据库而失败。
 * 启用方式：先导入 schema.sql、配置 db.properties，再执行
 *   mvn test -Dlibrary.db.test=true
 */
@EnabledIfSystemProperty(named = "library.db.test", matches = "true")
class SchemaSmokeTest {

    @Test
    void sixTablesExistInDatabase() throws SQLException {
        Set<String> tables = new HashSet<String>();
        try (Connection conn = DbUtil.getConnection();
             ResultSet rs = conn.getMetaData().getTables(
                     "library_db", null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                tables.add(rs.getString("TABLE_NAME"));
            }
        }
        String[] expected = {"category", "admin", "reader", "book", "borrow_record", "fine"};
        for (String table : expected) {
            assertTrue(tables.contains(table), "数据库中缺少表: " + table);
        }
    }

    @Test
    void activeCopyUniqueIndexExists() throws SQLException {
        boolean found = false;
        try (Connection conn = DbUtil.getConnection();
             ResultSet rs = conn.getMetaData().getIndexInfo(null, "library_db", "borrow_record", true, false)) {
            while (rs.next()) {
                if ("uk_active_copy".equalsIgnoreCase(rs.getString("INDEX_NAME"))) {
                    found = true;
                    break;
                }
            }
        }
        assertTrue(found, "borrow_record 上应存在唯一索引 uk_active_copy");
    }

    @Test
    void searchIndexesExist() throws SQLException {
        Set<String> indexes = new HashSet<String>();
        try (Connection conn = DbUtil.getConnection();
             ResultSet rs = conn.getMetaData().getIndexInfo(null, "library_db", "book", false, false)) {
            while (rs.next()) {
                indexes.add(rs.getString("INDEX_NAME"));
            }
        }
        assertTrue(indexes.contains("idx_book_title"), "缺少 idx_book_title");
        assertTrue(indexes.contains("idx_book_author"), "缺少 idx_book_author");
        assertTrue(indexes.contains("idx_book_isbn"), "缺少 idx_book_isbn");
    }
}
