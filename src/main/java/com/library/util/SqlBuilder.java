package com.library.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * SQL 片段构造器：集中处理「检索字段白名单」与「分页占位符」，
 * 所有值均通过 PreparedStatement 的 ? 参数绑定，避免 SQL 注入。
 */
public final class SqlBuilder {

    /** 图书表允许被检索的字段（与 schema.sql 中的索引列保持一致） */
    private static final Set<String> BOOK_SEARCH_COLUMNS =
            new HashSet<String>(Arrays.asList("title", "author", "isbn"));

    /** 读者表允许被检索的字段 */
    private static final Set<String> READER_SEARCH_COLUMNS =
            new HashSet<String>(Arrays.asList("name", "reader_no", "phone"));

    private SqlBuilder() {
    }

    /** 字段白名单校验：不在集合中的字段一律回退为默认列，杜绝拼接注入。 */
    public static String safeColumn(String candidate, Set<String> allowed, String defaultColumn) {
        return candidate != null && allowed.contains(candidate) ? candidate : defaultColumn;
    }

    public static String bookColumn(String candidate) {
        return safeColumn(candidate, BOOK_SEARCH_COLUMNS, "title");
    }

    public static String readerColumn(String candidate) {
        return safeColumn(candidate, READER_SEARCH_COLUMNS, "reader_no");
    }

    /** 追加分页子句，参数顺序为：LIMIT 的 pageSize、OFFSET 的 offset。 */
    public static String appendPagination(String sql) {
        return sql + " LIMIT ? OFFSET ?";
    }

    /** 构造 count 查询，fromWhere 形如 "FROM book WHERE title LIKE ?"。 */
    public static String count(String fromWhere) {
        return "SELECT COUNT(*) " + fromWhere;
    }
}
