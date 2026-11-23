package com.library.benchmark;

import com.library.util.DbUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * 索引基准测试：插入 1 万条图书数据，对比命中 idx_book_title 与
 * IGNORE INDEX 全表扫描的查询耗时，复现简历中「~300ms → ~40ms」的指标。
 *
 * <p>运行前请确保已导入 schema.sql 且 db.properties 指向该库。</p>
 *
 * <p>关键点：只有前缀匹配 LIKE 'xxx%' 或等值查询才能命中 B+Tree 索引；
 * LIKE '%xxx%' 前导通配符会导致索引失效。MySQL 8 已移除查询缓存，无需 SQL_NO_CACHE。</p>
 */
public final class IndexBenchmark {

    private static final int ROWS = 10000;
    private static final String PREFIX = "BenchmarkTitle";
    private static final int RUNS = 5;

    private IndexBenchmark() {
    }

    public static void main(String[] args) {
        Connection conn = null;
        try {
            conn = DbUtil.getConnection();
            System.out.println("正在插入 " + ROWS + " 条基准数据（已有基准数据会先清理）...");
            insertRows(conn);
            System.out.println("预热查询...");
            warmUp(conn);

            String indexedSql = "SELECT id,title FROM book WHERE title LIKE ? LIMIT 20";
            String scanSql = "SELECT id,title FROM book IGNORE INDEX(idx_book_title) WHERE title LIKE ? LIMIT 20";

            long indexed = measure(conn, indexedSql);
            long fullScan = measure(conn, scanSql);

            System.out.println("命中 idx_book_title 索引，平均耗时: " + indexed + " ms");
            System.out.println("IGNORE INDEX 全表扫描，平均耗时: " + fullScan + " ms");
            System.out.println("说明：本机数据量/硬件不同，绝对值会有差异；重点是索引命中带来数量级的下降。");
        } catch (SQLException e) {
            System.err.println("基准测试失败，请检查 MySQL 是否启动以及 db.properties 配置: " + e.getMessage());
            e.printStackTrace();
        } finally {
            DbUtil.close(conn);
        }
    }

    private static void insertRows(Connection conn) throws SQLException {
        try (PreparedStatement del = conn.prepareStatement("DELETE FROM book WHERE title LIKE 'BenchmarkTitle%'")) {
            del.executeUpdate();
        }
        String sql = "INSERT INTO book(isbn,title,author,publisher,total_stock,available_stock,"
                + "price,publish_year,location) VALUES(?,?,?,?,?,?,?,?,?)";
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < ROWS; i++) {
                ps.setString(1, "BENCH-" + i);
                ps.setString(2, PREFIX + "-" + i);
                ps.setString(3, "BenchAuthor");
                ps.setString(4, "TestPress");
                ps.setInt(5, 1);
                ps.setInt(6, 1);
                ps.setBigDecimal(7, new BigDecimal("10.00"));
                ps.setInt(8, 2024);
                ps.setString(9, "BENCH");
                ps.addBatch();
                if (i % 1000 == 999) {
                    ps.executeBatch();
                }
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void warmUp(Connection conn) throws SQLException {
        for (int i = 0; i < 3; i++) {
            executeCount(conn, "SELECT id,title FROM book WHERE title LIKE ? LIMIT 20");
        }
    }

    private static long measure(Connection conn, String sql) throws SQLException {
        long total = 0;
        for (int i = 0; i < RUNS; i++) {
            long start = System.nanoTime();
            int count = executeCount(conn, sql);
            total += (System.nanoTime() - start) / 1_000_000;
            if (count == 0) {
                System.out.println("警告：查询未命中任何行，请确认基准数据已插入。");
            }
        }
        return total / RUNS;
    }

    private static int executeCount(Connection conn, String sql) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, PREFIX + "%");
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    count++;
                }
                return count;
            }
        }
    }
}
