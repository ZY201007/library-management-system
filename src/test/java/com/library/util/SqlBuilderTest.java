package com.library.util;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** 分页与检索字段白名单的单元测试：验证参数化 SQL 构造，无需数据库。 */
class SqlBuilderTest {

    @Test
    void appendPaginationAddsPlaceholders() {
        assertEquals("SELECT * FROM book LIMIT ? OFFSET ?",
                SqlBuilder.appendPagination("SELECT * FROM book"));
    }

    @Test
    void safeColumnRejectsInjectionAndUnknownColumns() {
        Set<String> allowed = new HashSet<String>(Arrays.asList("title", "author"));
        assertEquals("title", SqlBuilder.safeColumn("title; DROP TABLE book", allowed, "title"));
        assertEquals("author", SqlBuilder.safeColumn("author", allowed, "title"));
        assertEquals("title", SqlBuilder.safeColumn(null, allowed, "title"));
    }

    @Test
    void bookAndReaderColumnsUseWhitelist() {
        assertEquals("isbn", SqlBuilder.bookColumn("isbn"));
        assertEquals("title", SqlBuilder.bookColumn("bad_field"));
        assertEquals("reader_no", SqlBuilder.readerColumn("reader_no"));
        assertEquals("reader_no", SqlBuilder.readerColumn("password"));
    }

    @Test
    void countWrapsFromWhere() {
        assertEquals("SELECT COUNT(*) FROM book WHERE title LIKE ?",
                SqlBuilder.count("FROM book WHERE title LIKE ?"));
    }
}
