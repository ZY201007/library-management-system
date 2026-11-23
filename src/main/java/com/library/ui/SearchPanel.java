package com.library.ui;

import com.library.model.Book;
import com.library.service.SearchService;
import com.library.service.ServiceException;
import com.library.util.Page;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.util.List;

/** 检索面板：字段白名单 + 参数化 LIKE + LIMIT/OFFSET 分页。 */
public class SearchPanel extends JPanel {

    private final SearchService service;

    /** 默认使用真实数据库 Service。 */
    public SearchPanel() {
        this(new SearchService());
    }

    private final JComboBox<String> fieldBox =
            new JComboBox<String>(new String[]{"title", "author", "isbn"});
    private final JTextField keywordField = new JTextField(16);
    private final JTextField pageSizeField = new JTextField("10", 4);
    private final JLabel pageInfo = new JLabel("尚未查询");

    private int pageNo = 1;

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"ID", "ISBN", "书名", "作者", "出版社", "可借库存"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private final JTable table = new JTable(model);

    public SearchPanel(SearchService service) {
        this.service = service;
        setLayout(new BorderLayout());

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("检索字段:"));
        top.add(fieldBox);
        top.add(new JLabel("关键字:"));
        top.add(keywordField);
        top.add(new JLabel("每页:"));
        top.add(pageSizeField);
        JButton search = new JButton("查询");
        top.add(search);
        add(top, BorderLayout.NORTH);

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton prev = new JButton("上一页");
        JButton next = new JButton("下一页");
        bar.add(prev);
        bar.add(next);
        bar.add(pageInfo);
        add(bar, BorderLayout.SOUTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        search.addActionListener(e -> {
            pageNo = 1;
            doSearch();
        });
        prev.addActionListener(e -> {
            if (pageNo > 1) {
                pageNo--;
                doSearch();
            }
        });
        next.addActionListener(e -> {
            pageNo++;
            doSearch();
        });
    }

    private void doSearch() {
        try {
            int pageSize = Integer.parseInt(pageSizeField.getText().trim());
            String field = (String) fieldBox.getSelectedItem();
            Page<Book> page = service.searchBooks(field, keywordField.getText().trim(), pageNo, pageSize);

            model.setRowCount(0);
            List<Book> records = page.getRecords();
            for (Book b : records) {
                model.addRow(new Object[]{
                        b.getId(), b.getIsbn(), b.getTitle(), b.getAuthor(), b.getPublisher(), b.getAvailableStock()
                });
            }
            pageInfo.setText("第 " + page.getPageNo() + " / " + Math.max(1, page.getTotalPages())
                    + " 页，共 " + page.getTotal() + " 条");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "每页条数必须是数字", "输入错误", JOptionPane.ERROR_MESSAGE);
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "检索失败", JOptionPane.ERROR_MESSAGE);
        }
    }
}
