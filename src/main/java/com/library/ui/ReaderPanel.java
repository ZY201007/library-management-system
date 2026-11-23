package com.library.ui;

import com.library.model.Reader;
import com.library.service.ReaderService;
import com.library.service.ServiceException;

import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/** 读者管理面板：读者建档与 CRUD。 */
public class ReaderPanel extends JPanel {

    private final ReaderService service;

    /** 默认使用真实数据库 Service。 */
    public ReaderPanel() {
        this(new ReaderService());
    }

    private static final String[] LABELS =
            {"读者证号", "姓名", "性别(1男/2女)", "电话", "邮箱", "读者类型", "最大借阅数"};

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "证号", "姓名", "性别", "电话", "邮箱", "类型", "最大借阅"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public ReaderPanel(ReaderService service) {
        this.service = service;
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton refresh = new JButton("刷新");
        JButton add = new JButton("新增读者");
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
            for (Reader r : service.listAll()) {
                model.addRow(new Object[]{
                        r.getId(), r.getReaderNo(), r.getName(), r.getGender(), r.getPhone(),
                        r.getEmail(), r.getReaderType(), r.getMaxBorrow()
                });
            }
        } catch (ServiceException ex) {
            if (warn) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "数据库未就绪", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onAdd() {
        String[] v = FormDialog.show(this, "读者建档", LABELS);
        if (v == null) {
            return;
        }
        try {
            Reader r = toReader(null, v);
            service.createReader(r);
            refresh();
            JOptionPane.showMessageDialog(this, "新增成功，读者ID=" + r.getId());
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
        String[] initial = new String[LABELS.length];
        for (int i = 0; i < LABELS.length; i++) {
            initial[i] = model.getValueAt(row, i + 1) == null ? "" : String.valueOf(model.getValueAt(row, i + 1));
        }
        String[] v = FormDialog.show(this, "修改读者（ID=" + id + "）", LABELS, initial);
        if (v == null) {
            return;
        }
        try {
            if (service.updateReader(toReader(id, v))) {
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
        if (JOptionPane.showConfirmDialog(this, "确认删除读者 ID=" + id + "？",
                "删除确认", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            if (service.deleteReader(id)) {
                refresh();
            }
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "删除失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private static Reader toReader(Integer id, String[] v) {
        Reader r = new Reader();
        r.setId(id);
        r.setReaderNo(v[0]);
        r.setName(v[1]);
        r.setGender(v[2].isEmpty() ? 0 : Integer.valueOf(v[2]));
        r.setPhone(v[3]);
        r.setEmail(v[4]);
        r.setReaderType(v[5].isEmpty() ? "普通" : v[5]);
        r.setMaxBorrow(v[6].isEmpty() ? 5 : Integer.valueOf(v[6]));
        return r;
    }
}
