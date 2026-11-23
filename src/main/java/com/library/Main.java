package com.library;

import com.library.demo.DemoServices;
import com.library.ui.MainFrame;
import com.library.util.DbUtil;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.HeadlessException;

/**
 * 程序入口：启动 Swing 图形界面。
 *
 * <p>启动策略：</p>
 * <ul>
 *   <li>默认：先预检 MySQL 连通性；不可达时弹一次友好提示，可进入「演示模式」（内存数据）
 *       或取消退出，绝不直接崩溃；无显示设备（headless）时无法弹框，降级为控制台提示并
 *       友好退出，同样不会抛异常；</li>
 *   <li>{@code --gui-demo}：跳过数据库，直接用内置示例数据启动，所有功能页可操作；</li>
 *   <li>{@code --gui-selftest}：用假数据构造并显示 MainFrame，短暂延时后自动关闭，
 *       用于无数据库环境下验证 GUI 能真正启动。</li>
 * </ul>
 */
public class Main {

    private static final String ARG_DEMO = "--gui-demo";
    private static final String ARG_SELFTEST = "--gui-selftest";

    public static void main(String[] args) {
        final boolean selftest = hasArg(args, ARG_SELFTEST);
        final boolean demo = hasArg(args, ARG_DEMO) || selftest;

        // 数据库预检与 JOptionPane 都必须放在 EDT 内：Swing 组件只能由事件分发线程访问，
        // 且 HeadlessException 必须在这里捕获，否则 headless 环境会在 main 线程直接崩溃。
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                boolean useDemo = demo;
                try {
                    if (!useDemo && !DbUtil.isReachable()) {
                        try {
                            int choice = JOptionPane.showConfirmDialog(null,
                                    "未检测到可用的 MySQL 数据库（localhost:3306/library_db）。\n\n"
                                            + "【确定】进入演示模式：内置示例数据，5 个功能页均可操作，关闭程序后数据不保留。\n"
                                            + "【取消】退出程序：请按 README 配置 db.properties 并导入 schema.sql 后重试。\n\n"
                                            + "也可使用 --gui-demo 参数直接启动演示模式。",
                                    "数据库未就绪 — 选择启动方式",
                                    JOptionPane.OK_CANCEL_OPTION,
                                    JOptionPane.WARNING_MESSAGE);
                            if (choice != JOptionPane.OK_OPTION) {
                                System.out.println("已取消启动。请按 README 配置 db.properties 并导入 schema.sql 后重试。");
                                return;
                            }
                            useDemo = true;
                        } catch (HeadlessException he) {
                            // 无显示设备时无法弹框：降级为控制台提示并友好退出，不构造任何 GUI，
                            // 避免再次触发 HeadlessException。
                            System.out.println("当前环境无显示设备，无法弹出选择框。");
                            System.out.println("未检测到可用的 MySQL 数据库（localhost:3306/library_db），"
                                    + "本可自动进入演示模式（内存数据），但当前环境无显示设备，"
                                    + "跳过图形界面并正常退出。");
                            System.out.println("如需真实数据库，请按 README 配置 db.properties 并导入 schema.sql 后重试。");
                            return;
                        }
                    }

                    MainFrame frame = createFrame(useDemo);
                    if (useDemo) {
                        frame.setTitle("图书馆管理系统 — 演示模式（内存数据，关闭后不保留）");
                    }
                    frame.setVisible(true);

                    if (selftest) {
                        // 让窗口真正显示一小段时间，验证不是"构造成功但显示即崩溃"
                        Timer timer = new Timer(1500, e -> {
                            frame.dispose();
                            System.out.println("GUI_SELFTEST_OK: MainFrame instantiated and displayed successfully");
                            System.exit(0);
                        });
                        timer.setRepeats(false);
                        timer.start();
                    }
                } catch (HeadlessException e) {
                    System.err.println("当前环境无显示设备，图形界面无法启动: " + e.getMessage());
                    if (selftest) {
                        System.out.println("GUI_SELFTEST_HEADLESS");
                        System.exit(0);
                    }
                } catch (Throwable t) {
                    System.err.println("图形界面启动失败: " + t);
                    t.printStackTrace();
                    if (selftest) {
                        System.exit(1);
                    }
                }
            }
        });
    }

    private static MainFrame createFrame(boolean demo) {
        if (!demo) {
            return new MainFrame();
        }
        return new MainFrame(
                new DemoServices.Books(),
                new DemoServices.Readers(),
                new DemoServices.Borrows(),
                new DemoServices.Search(),
                new DemoServices.Fines());
    }

    private static boolean hasArg(String[] args, String target) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (target.equalsIgnoreCase(arg)) {
                return true;
            }
        }
        return false;
    }
}
