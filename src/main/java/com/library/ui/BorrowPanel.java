package com.library.ui;

import com.library.model.BorrowRecord;
import com.library.service.BorrowService;
import com.library.service.ServiceException;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 借还面板：借出（事务）、归还（逾期计费）、续借。 */
public class BorrowPanel extends JPanel {

    private final BorrowService service;

    /** 默认使用真实数据库 Service。 */
    public BorrowPanel() {
        this(new BorrowService());
    }

    private final JTextField readerField = new JTextField(6);
    private final JTextField bookField = new JTextField(6);
    private final JTextField copyField = new JTextField(12);

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"借阅ID", "读者ID", "图书ID", "副本号", "借出日", "应还日", "续借次数"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public BorrowPanel(BorrowService service) {
        this.service = service;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("读者ID:"));
        top.add(readerField);
        top.add(new JLabel("图书ID:"));
        top.add(bookField);
        top.add(new JLabel("副本号:"));
        top.add(copyField);
        JButton borrow = new JButton("借出");
        top.add(borrow);
        add(top, BorderLayout.NORTH);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton renew = new JButton("续借");
        JButton returnBtn = new JButton("归还");
        JButton refresh = new JButton("刷新在借列表");
        bar.add(renew);
        bar.add(returnBtn);
        bar.add(refresh);
        add(bar, BorderLayout.SOUTH);

        add(new JScrollPane(table), BorderLayout.CENTER);

        borrow.addActionListener(e -> onBorrow());
        renew.addActionListener(e -> onRenew());
        returnBtn.addActionListener(e -> onReturn());
        refresh.addActionListener(e -> refresh());

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
            List<BorrowRecord> records = service.listActive();
            for (BorrowRecord r : records) {
                model.addRow(new Object[]{
                        r.getId(), r.getReaderId(), r.getBookId(), r.getCopyNo(),
                        r.getBorrowDate(), r.getDueDate(), r.getRenewCount()
                });
            }
        } catch (ServiceException ex) {
            if (warn) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "数据库未就绪", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onBorrow() {
        try {
            int readerId = Integer.parseInt(readerField.getText().trim());
            int bookId = Integer.parseInt(bookField.getText().trim());
            BorrowRecord record = service.borrow(readerId, bookId, copyField.getText());
            JOptionPane.showMessageDialog(this,
                    "借出成功！借阅ID=" + record.getId() + "，应还日期=" + record.getDueDate());
            copyField.setText("");
            refresh();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "读者ID和图书ID必须是数字", "输入错误", JOptionPane.ERROR_MESSAGE);
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "借出失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onReturn() {
        int borrowId = selectedBorrowId();
        if (borrowId < 0) {
            return;
        }
        try {
            BigDecimal fine = service.returnBook(borrowId);
            String msg = fine.compareTo(BigDecimal.ZERO) > 0
                    ? "归还成功，产生逾期罚款 " + fine + " 元（请到罚款管理页查看）"
                    : "归还成功，无逾期罚款。";
            JOptionPane.showMessageDialog(this, msg);
            refresh();
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "归还失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onRenew() {
        int borrowId = selectedBorrowId();
        if (borrowId < 0) {
            return;
        }
        try {
            LocalDate newDue = service.renew(borrowId);
            JOptionPane.showMessageDialog(this, "续借成功，新的应还日期=" + newDue);
            refresh();
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "续借失败", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int selectedBorrowId() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先在列表中选择一条借阅记录");
            return -1;
        }
        return (Integer) model.getValueAt(row, 0);
    }
}
