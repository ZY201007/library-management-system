package com.library.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * 数据库连接工具：读取 classpath 下的 db.properties，
 * 不存在时回退到 db.properties.example（本地开发默认值），不硬编码生产凭据。
 */
public final class DbUtil {

    private static final Properties PROPS = new Properties();

    private static final String DEFAULT_URL =
            "jdbc:mysql://localhost:3306/library_db?useUnicode=true&characterEncoding=utf8"
                    + "&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "root";

    static {
        InputStream in = DbUtil.class.getClassLoader().getResourceAsStream("db.properties");
        if (in == null) {
            in = DbUtil.class.getClassLoader().getResourceAsStream("db.properties.example");
        }
        try {
            if (in != null) {
                PROPS.load(in);
            }
            // 显式加载驱动；JDBC 4 自动加载也可，但显式加载更利于排查环境变量问题
            Class.forName(PROPS.getProperty("jdbc.driver", "com.mysql.cj.jdbc.Driver"));
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC 驱动未找到，请检查 pom.xml 依赖：" + e.getMessage());
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                    // 关闭失败不影响启动
                }
            }
        }
    }

    private DbUtil() {
    }

    /** 获取一个新连接；调用方负责关闭（建议使用 try-with-resources）。 */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("jdbc.url", DEFAULT_URL),
                PROPS.getProperty("jdbc.user", DEFAULT_USER),
                PROPS.getProperty("jdbc.password", DEFAULT_PASSWORD));
    }

    /**
     * 数据库连通性预检（带超时），供 GUI 启动时决定使用真实数据库还是演示模式。
     * 连接失败、驱动缺失等任何问题都返回 false，不向外抛出，保证界面不会因此崩溃。
     */
    public static boolean isReachable() {
        try {
            DriverManager.setLoginTimeout(3);
            Connection conn = null;
            try {
                conn = getConnection();
                return conn != null && conn.isValid(2);
            } finally {
                close(conn);
            }
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    /** 统一关闭资源，忽略关闭异常。 */
    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        }
    }

    /** 事务回滚时吞掉二次异常，避免掩盖真正的业务异常。 */
    public static void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }
}
