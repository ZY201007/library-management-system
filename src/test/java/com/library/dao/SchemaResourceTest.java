package com.library.dao;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 无需数据库的 schema.sql 静态烟测：确认六张表、唯一约束与索引都已写入脚本。 */
class SchemaResourceTest {

    private String schema() throws Exception {
        InputStream in = getClass().getClassLoader().getResourceAsStream("schema.sql");
        assertNotNull(in, "schema.sql 应位于 src/main/resources 并被打入 classpath");
        try (Scanner scanner = new Scanner(in, "UTF-8")) {
            return scanner.useDelimiter("\\A").next();
        }
    }

    @Test
    void schemaDefinesSixTables() throws Exception {
        String sql = schema();
        String[] tables = {"category", "admin", "reader", "book", "borrow_record", "fine"};
        for (String table : tables) {
            assertTrue(sql.contains("CREATE TABLE " + table), "缺少表: " + table);
        }
    }

    @Test
    void schemaDefinesUniqueConstraintAndSearchIndexes() throws Exception {
        String sql = schema();
        assertTrue(sql.contains("UNIQUE KEY uk_active_copy"), "缺少防重复借出的唯一约束");
        assertTrue(sql.contains("idx_book_title"), "缺少书名索引");
        assertTrue(sql.contains("idx_book_author"), "缺少作者索引");
        assertTrue(sql.contains("idx_book_isbn"), "缺少 ISBN 索引");
        assertTrue(sql.contains("idx_reader_no"), "缺少读者证号索引");
        assertTrue(sql.contains("GENERATED ALWAYS AS"), "缺少生成列定义");
    }

    @Test
    void schemaDefinesForeignKeys() throws Exception {
        String sql = schema();
        assertTrue(sql.contains("fk_book_category"));
        assertTrue(sql.contains("fk_borrow_reader"));
        assertTrue(sql.contains("fk_borrow_book"));
        assertTrue(sql.contains("fk_fine_borrow"));
    }
}
