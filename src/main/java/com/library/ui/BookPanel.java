package com.library.ui;

import com.library.model.Book;
import com.library.service.BookService;
import com.library.service.ServiceException;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.util.List;

/** 图书管理面板：建档、修改、删除、列表。 */
public class BookPanel extends JPanel {

    private final BookService service;

    /** 默认使用真实数据库 Service。 */
    public BookPanel() {
        this(new BookService());
    }

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "ISBN", "书名", "作者", "出版社", "总册数", "可借", "定价"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public BookPanel(BookService service) {
        this.service = service;
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("刷新");
        JButton add = new JButton("新增图书");
        JButton edit = new JButton("修改");
        JButton delete = new JButton("删除");
        bar.add(refresh);
        bar.add(add);
        bar.add(edit);
        bar.add(delete);
        add(bar, BorderLayout.SOUTH);

        refresh.addActionListener(e -> refresh());
        add.addActionListener(e -> onAdd());
        edit.addActionListener(e -> onEdit());
        delete.addActionListener(e -> onDelete());

        // 启动阶段静默加载：无数据库时不弹模态框，避免连续弹窗阻塞界面
        silentRefresh();
    }

    private void refresh() {
        loadRows(true);
    }

    private void silentRefresh() {
        loadRows(false);
    }

    private void loadRows(boolean warn) {
        model.setRowCount(0);
        try {
            List<Book> books = service.listAll();
            for (Book b : books) {
                model.addRow(new Object[]{
                        b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getPublisher(),
                        b.getTotalStock(), b.getAvailableStock(), b.getPrice()
                });
            }
        } catch (ServiceException ex) {
            if (warn) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "数据库未就绪", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onAdd() {
        String[] labels = {"ISBN", "书名", "作者", "出版社", "分类ID", "总册数", "定价", "出版年份", "馆藏位置"};
        String[] v = FormDialog.show(this, "新增图书", labels);
        if (v == null) {
            return;
        }
        try {
            Book b = new Book();
            b.setIsbn(required(v[0], "ISBN"));
            b.setTitle(required(v[1], "书名"));
            b.setAuthor(v[2]);
            b.setPublisher(v[3]);
            b.setCategoryId(optionalInt(v[4]));
            b.setTotalStock(requiredInt(v[5], "总册数"));
            b.setPrice(optionalDecimal(v[6]));
            b.setPublishYear(optionalInt(v[7]));
            b.setLocation(v[8]);
            service.createBook(b);
            refresh();
            JOptionPane.showMessageDialog(this, "新增成功，图书ID=" + b.getId());
        } catch (ServiceException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "新增失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onEdit() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选中一行");
            return;
        }
        int id = (Integer) model.getValueAt(row, 0);
        String[] labels = {"ISBN", "书名", "作者", "出版社", "分类ID", "总册数", "定价", "出版年份", "馆藏位置"};
        String[] initial = {
                str(model.getValueAt(row, 1)), str(model.getValueAt(row, 2)), str(model.getValueAt(row, 3)),
                str(model.getValueAt(row, 4)), "", str(model.getValueAt(row, 5)),
                str(model.getValueAt(row, 7)), "", ""
        };
        String[] v = FormDialog.show(this, "修改图书（ID=" + id + "）", labels, initial);
        if (v == null) {
            return;
        }
        try {
            Book b = new Book();
            b.setId(id);
            b.setIsbn(required(v[0], "ISBN"));
            b.setTitle(required(v[1], "书名"));
            b.setAuthor(v[2]);
            b.setPublisher(v[3]);
            b.setCategoryId(optionalInt(v[4]));
            b.setTotalStock(requiredInt(v[5], "总册数"));
            b.setPrice(optionalDecimal(v[6]));
            b.setPublishYear(optionalInt(v[7]));
            b.setLocation(v[8]);
            if (service.updateBook(b)) {
                refresh();
            }
        } catch (ServiceException | NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "修改失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onDelete() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选中一行");
            return;
        }
        int id = (Integer) model.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "确认删除图书 ID=" + id + "？",
                "删除确认", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (service.deleteBook(id)) {
                refresh();
            }
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "删除失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private static String required(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new ServiceException(name + "不能为空");
        }
        return value;
    }

    private static Integer requiredInt(String value, String name) {
        if (value == null || value.isEmpty()) {
            throw new ServiceException(name + "不能为空");
        }
        return Integer.valueOf(value);
    }

    private static Integer optionalInt(String value) {
        return value == null || value.isEmpty() ? null : Integer.valueOf(value);
    }

    private static BigDecimal optionalDecimal(String value) {
        return value == null || value.isEmpty() ? null : new BigDecimal(value);
    }
}
