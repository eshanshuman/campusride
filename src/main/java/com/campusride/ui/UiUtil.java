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
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableModel;

/**
 * Shared colours and small helpers, so every screen looks the same and the
 * screen classes stay short. Plain Swing only.
 */
public final class UiUtil {

    // ---- project colours ----
    public static final Color NAVY       = new Color(0x17, 0x3B, 0x63);  // #173B63 headers
    public static final Color STEEL_BLUE = new Color(0x2F, 0x6D, 0xB2);  // #2F6DB2 main buttons
    public static final Color YELLOW     = new Color(0xF4, 0xC5, 0x42);  // #F4C542 highlights
    public static final Color BACKGROUND = new Color(0xF4, 0xF6, 0xF9);  // light grey page
    public static final Color GREEN      = new Color(0x1E, 0x7D, 0x32);
    public static final Color RED        = new Color(0xC6, 0x28, 0x28);

    public static final String DATE_PATTERN = "yyyy-MM-dd";

    private UiUtil() { }

    /** Called once from Main: a slightly larger default font for every component. */
    public static void applyTheme() {
        Font base = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        for (Object key : UIManager.getDefaults().keySet().toArray()) {
            if (UIManager.get(key) instanceof FontUIResource) {
                UIManager.put(key, new FontUIResource(base));
            }
        }
    }

    /** Navy bar at the top of a window with a title and a smaller subtitle. */
    public static JPanel header(String title, String subtitle) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(NAVY);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 4, 0, YELLOW),     // yellow underline
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));
        JLabel t = new JLabel(title);
        t.setForeground(Color.WHITE);
        t.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        JLabel s = new JLabel(subtitle);
        s.setForeground(new Color(0xD6, 0xE2, 0xF0));
        JPanel text = new JPanel(new BorderLayout());
        text.setOpaque(false);
        text.add(t, BorderLayout.NORTH);
        text.add(s, BorderLayout.SOUTH);
        bar.add(text, BorderLayout.WEST);
        return bar;
    }

    /** Steel-blue button for the main action of a form. */
    public static JButton primaryButton(String text) {
        return button(text, STEEL_BLUE, Color.WHITE);
    }

    /** Plain white button for secondary actions. */
    public static JButton secondaryButton(String text) {
        JButton b = button(text, Color.WHITE, NAVY);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(STEEL_BLUE),
                BorderFactory.createEmptyBorder(6, 14, 6, 14)));
        return b;
    }

    private static JButton button(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createEmptyBorder(7, 15, 7, 15));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    /** Light card with a navy title border, used to group parts of a screen. */
    public static JPanel section(String title) {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        TitledBorder tb = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(0xC9, 0xD4, 0xE3)), title);
        tb.setTitleColor(NAVY);
        tb.setTitleFont(new Font(Font.SANS_SERIF, Font.BOLD, 13));
        p.setBorder(BorderFactory.createCompoundBorder(tb, BorderFactory.createEmptyBorder(6, 8, 8, 8)));
        return p;
    }

    /** A table model whose cells cannot be edited by the user (read-only). */
    public static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    /** JTable for a read-only model: single row selection, navy header. */
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

    public static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Date picker made from a JSpinner (no typing errors possible). */
    public static JSpinner dateSpinner(LocalDate initial) {
        Date d = Date.from(initial.atStartOfDay(ZoneId.systemDefault()).toInstant());
        JSpinner sp = new JSpinner(new SpinnerDateModel(d, null, null, java.util.Calendar.DAY_OF_MONTH));
        sp.setEditor(new JSpinner.DateEditor(sp, DATE_PATTERN));
        return sp;
    }

    /** Reads the LocalDate shown in a spinner made by dateSpinner(). */
    public static LocalDate getDate(JSpinner spinner) {
        Date d = (Date) spinner.getValue();
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    public static void setDate(JSpinner spinner, LocalDate date) {
        spinner.setValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    /**
     * Adds "label : field" as one row of a GridBagLayout form.
     * Column 0 = right-aligned label, column 1 = field that stretches.
     */
    public static void addRow(JPanel form, int row, String label, Component field) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.anchor = GridBagConstraints.LINE_END;
        c.insets = new Insets(5, 5, 5, 10);
        form.add(new JLabel(label), c);

        c.gridx = 1;
        c.anchor = GridBagConstraints.LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1.0;
        c.insets = new Insets(5, 0, 5, 5);
        form.add(field, c);
    }

    /** Puts a component across both columns of a GridBagLayout form. */
    public static void addWide(JPanel form, int row, Component comp) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(10, 5, 5, 5);
        form.add(comp, c);
    }

    // ---- friendly message boxes (never stack traces) ----

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "CampusRide", JOptionPane.WARNING_MESSAGE);
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "CampusRide", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Please confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    /** Text for a possibly-null value in a table cell. */
    public static String text(Object value) {
        return value == null ? "" : value.toString();
    }
}
