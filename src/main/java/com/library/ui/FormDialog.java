package com.library.ui;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.GridLayout;

/** 简易表单对话框：避免每个面板重复编写输入弹窗代码。 */
public final class FormDialog {

    private FormDialog() {
    }

    /** 显示表单，用户取消返回 null。 */
    public static String[] show(JComponent parent, String title, String[] labels) {
        return show(parent, title, labels, null);
    }

    /** 带初始值的表单，用于「修改」场景。 */
    public static String[] show(JComponent parent, String title, String[] labels, String[] initial) {
        JPanel panel = new JPanel(new GridLayout(labels.length, 2, 6, 6));
        JTextField[] fields = new JTextField[labels.length];
        for (int i = 0; i < labels.length; i++) {
            panel.add(new JLabel(labels[i]));
            fields[i] = new JTextField(18);
            if (initial != null && i < initial.length && initial[i] != null) {
                fields[i].setText(initial[i]);
            }
            panel.add(fields[i]);
        }
        int result = JOptionPane.showConfirmDialog(parent, panel, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        String[] values = new String[labels.length];
        for (int i = 0; i < labels.length; i++) {
            values[i] = fields[i].getText().trim();
        }
        return values;
    }
}
