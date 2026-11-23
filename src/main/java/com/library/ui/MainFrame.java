package com.library.ui;

import com.library.service.BookService;
import com.library.service.BorrowService;
import com.library.service.FineService;
import com.library.service.ReaderService;
import com.library.service.SearchService;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/** 主窗口：以 Tab 页组织五大功能，所有面板只调用 Service 层。 */
public class MainFrame extends JFrame {

    /** 默认使用真实数据库 Service，供 IDE 直接运行 MainFrame.main() 时使用。 */
    public MainFrame() {
        this(new BookService(), new ReaderService(), new BorrowService(),
                new SearchService(), new FineService());
    }

    /**
     * 可注入 Service 的构造方法：演示模式与 GUI 自检传入内存 Demo Service，
     * 正常模式传入真实 Service。UI 层不关心数据来自 MySQL 还是内存。
     */
    public MainFrame(BookService bookService, ReaderService readerService,
                     BorrowService borrowService, SearchService searchService,
                     FineService fineService) {
        super("图书馆管理系统  —  Java + Swing + JDBC + MySQL");
        setSize(1080, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("图书管理", new BookPanel(bookService));
        tabs.addTab("读者管理", new ReaderPanel(readerService));
        tabs.addTab("借还管理", new BorrowPanel(borrowService));
        tabs.addTab("检索", new SearchPanel(searchService));
        tabs.addTab("罚款管理", new FinePanel(fineService));

        setLayout(new BorderLayout());
        add(tabs, BorderLayout.CENTER);
    }

    public static void main(String[] args) {
        // 委托给 Main，统一享受"数据库预检 + 友好提示 + 演示模式 + 自检"启动流程
        com.library.Main.main(args);
    }
}
