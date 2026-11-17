package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

// Colours and small pieces we use on every screen, so all screens look the same.
public class UiUtil {

    // our colours
    public static final Color NAVY = new Color(0x17, 0x3B, 0x63);        // #173B63 header
    public static final Color STEEL_BLUE = new Color(0x2F, 0x6D, 0xB2);  // #2F6DB2 main buttons
    public static final Color YELLOW = new Color(0xF4, 0xC5, 0x42);      // #F4C542 highlights
    public static final Color BACKGROUND = new Color(0xF4, 0xF6, 0xF9);  // light grey
    public static final Color GREEN = new Color(0x1E, 0x7D, 0x32);
    public static final Color RED = new Color(0xC6, 0x28, 0x28);

    // called once in Main: makes the default font a bit bigger
    public static void applyTheme() {
        Font font = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        UIManager.put("Label.font", font);
        UIManager.put("Button.font", font);
        UIManager.put("TextField.font", font);
        UIManager.put("PasswordField.font", font);
        UIManager.put("TextArea.font", font);
        UIManager.put("ComboBox.font", font);
        UIManager.put("List.font", font);
        UIManager.put("Spinner.font", font);
        UIManager.put("FormattedTextField.font", font);
        UIManager.put("Table.font", font);
        UIManager.put("TableHeader.font", font);
        UIManager.put("TabbedPane.font", font);
        UIManager.put("TitledBorder.font", font);
        UIManager.put("OptionPane.messageFont", font);
        UIManager.put("OptionPane.buttonFont", font);
    }

    // navy bar at the top of each window, with a yellow line under it
    public static JPanel header(String title, String subtitle) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(NAVY);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, YELLOW),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        JLabel subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setForeground(new Color(0xD6, 0xE2, 0xF0));

        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(false);
        text.add(titleLabel, BorderLayout.NORTH);
        text.add(subtitleLabel, BorderLayout.SOUTH);
        bar.add(text, BorderLayout.WEST);
        return bar;
    }

    // blue button for the main action on a screen
    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(STEEL_BLUE);
        b.setForeground(Color.WHITE);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 15, 7, 15));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // white button with a blue border for the other actions
    public static JButton secondaryButton(String text) {
        JButton b = new JButton(text);
        b.setBackground(Color.WHITE);
        b.setForeground(NAVY);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STEEL_BLUE),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // white box with a title on its border, used to group parts of a screen
    public static JPanel section(String title) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0xC9, 0xD4, 0xE3)), title);
        border.setTitleColor(NAVY);
        border.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        p.setBorder(BorderFactory.createCompoundBorder(border, BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        return p;
    }

    // table model where the user can't edit the cells (our tables are read-only)
    public static DefaultTableModel readOnlyModel(String[] columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    // JTable with our look: one row selected at a time, navy header
    public static JTable table(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setRowHeight(24);
        t.setFillsViewportHeight(true);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setBackground(NAVY);
        t.getTableHeader().setForeground(Color.WHITE);
        t.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        t.setSelectionBackground(new Color(0xFF, 0xEB, 0xB0));   // pale yellow
        t.setSelectionForeground(Color.BLACK);
        return t;
    }

    // A JSpinner used as a date picker, so the user can't type a wrong date format.
    // JSpinner works with java.util.Date, so we convert from/to LocalDate.
    public static JSpinner dateSpinner(LocalDate startDate) {
        Date date = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner spinner = new JSpinner(new SpinnerDateModel(date, null, null, Calendar.DAY_OF_MONTH));
        spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
        return spinner;
    }

    public static LocalDate getDate(JSpinner spinner) {
        Date date = (Date) spinner.getValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void setDate(JSpinner spinner, LocalDate newDate) {
        spinner.setValue(Date.from(newDate.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    // Adds one "label  [field]" row to a form that uses GridBagLayout.
    // Column 0 is the label, column 1 is the field (the field stretches).
    public static void addRow(JPanel form, int row, String label, Component field) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.anchor = GridBagConstraints.LINE_END;
        gbc.insets = new Insets(5, 5, 5, 10);
        form.add(new JLabel(label), gbc);

        gbc.gridx = 1;
        gbc.anchor = GridBagConstraints.LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(5, 0, 5, 5);
        form.add(field, gbc);
    }

    // puts something (usually a row of buttons) across both columns of the form
    public static void addWide(JPanel form, int row, Component comp) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 5, 5, 5);
        form.add(comp, gbc);
    }
}
