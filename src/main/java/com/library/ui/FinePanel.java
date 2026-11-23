package com.library.ui;

import com.library.model.Fine;
import com.library.service.FineService;
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

/** 罚款面板：查看未缴账单、缴纳罚款。 */
public class FinePanel extends JPanel {

    private final FineService service;

    /** 默认使用真实数据库 Service。 */
    public FinePanel() {
        this(new FineService());
    }

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"账单ID", "借阅ID", "读者ID", "金额(元)", "逾期天数", "状态"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public FinePanel(FineService service) {
        this.service = service;
        setLayout(new BorderLayout());
        add(new JScrollPane(table), BorderLayout.CENTER);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton pay = new JButton("缴纳选中账单");
        JButton refresh = new JButton("刷新");
        bar.add(pay);
        bar.add(refresh);
        add(bar, BorderLayout.SOUTH);

        pay.addActionListener(e -> onPay());
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
            List<Fine> fines = service.listUnpaid();
            for (Fine f : fines) {
                model.addRow(new Object[]{
                        f.getId(), f.getBorrowId(), f.getReaderId(), f.getAmount(),
                        f.getDaysOverdue(), Boolean.TRUE.equals(f.getPaid()) ? "已缴" : "未缴"
                });
            }
        } catch (ServiceException ex) {
            if (warn) {
                JOptionPane.showMessageDialog(this, ex.getMessage(), "数据库未就绪", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void onPay() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选中一条未缴账单");
            return;
        }
        int id = (Integer) model.getValueAt(row, 0);
        try {
            if (service.pay(id)) {
                JOptionPane.showMessageDialog(this, "缴纳成功，账单ID=" + id);
                refresh();
            }
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "缴纳失败", JOptionPane.ERROR_MESSAGE);
        }
    }
}
