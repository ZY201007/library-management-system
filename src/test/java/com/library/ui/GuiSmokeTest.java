package com.library.ui;

import com.library.demo.DemoServices;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import java.awt.Component;
import java.awt.Container;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GUI 冒烟测试：用演示 Service 构造五个面板，验证在无数据库环境下
 * GUI 类可实例化、表格能加载数据，不抛出任何异常。
 *
 * <p>JPanel/JTable 是轻量级 Swing 组件，构造不依赖显示设备，故 headless 环境亦可运行。</p>
 */
class GuiSmokeTest {

    @Test
    void demoPanelsCanBeConstructedWithoutDatabase() {
        assertTableRows(new BookPanel(new DemoServices.Books()), 5, "图书管理");
        assertTableRows(new ReaderPanel(new DemoServices.Readers()), 3, "读者管理");
        assertTableRows(new BorrowPanel(new DemoServices.Borrows()), 1, "借还管理");
        assertTableRows(new FinePanel(new DemoServices.Fines()), 1, "罚款管理");
        assertNotNull(new SearchPanel(new DemoServices.Search()), "检索面板应能构造");
    }

    private static void assertTableRows(Container panel, int expectedMin, String name) {
        JTable table = findTable(panel);
        assertNotNull(table, name + "应包含表格");
        assertTrue(table.getRowCount() >= expectedMin,
                name + "应加载出示例数据，实际行数=" + table.getRowCount());
    }

    private static JTable findTable(Container container) {
        for (Component c : container.getComponents()) {
            if (c instanceof JTable) {
                return (JTable) c;
            }
            if (c instanceof Container) {
                JTable found = findTable((Container) c);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
