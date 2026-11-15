package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.campusride.model.BusPass;
import com.campusride.service.ApplicationService;
import com.campusride.service.ServiceException;
import com.campusride.util.Session;

// "My Bus Pass" tab.
// Top: the pass drawn like a card (navy with a yellow stripe).
// Bottom: a table with all the student's passes.
public class PassViewPanel extends JPanel {

    private ApplicationService applicationService = new ApplicationService();

    private DefaultTableModel passModel = UiUtil.readOnlyModel(
            "Pass No", "Route", "Stop", "Pickup", "From", "To", "Status");
    private JTable passTable = UiUtil.table(passModel);
    private List<BusPass> passes = new ArrayList<>();

    // labels on the card
    private JLabel cardPassNo = cardLabel(22, Font.BOLD);
    private JLabel cardName = cardLabel(15, Font.BOLD);
    private JLabel cardRoll = cardLabel(13, Font.PLAIN);
    private JLabel cardRoute = cardLabel(13, Font.PLAIN);
    private JLabel cardStop = cardLabel(13, Font.PLAIN);
    private JLabel cardDates = cardLabel(13, Font.PLAIN);
    private JLabel cardStatus = cardLabel(15, Font.BOLD);

    public PassViewPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ----- the card -----
        JPanel card = new JPanel(new GridLayout(7, 1, 0, 2));
        card.setBackground(UiUtil.NAVY);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 10, 0, 0, UiUtil.YELLOW),   // yellow stripe on the left
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        card.setPreferredSize(new Dimension(520, 230));

        JLabel brand = cardLabel(12, Font.BOLD);
        brand.setText("CAMPUSRIDE  -  COLLEGE BUS PASS");
        brand.setForeground(UiUtil.YELLOW);
        card.add(brand);
        card.add(cardPassNo);
        card.add(cardName);
        card.add(cardRoll);
        card.add(cardRoute);
        card.add(cardStop);
        JPanel lastRow = new JPanel(new BorderLayout());
        lastRow.setOpaque(false);
        lastRow.add(cardDates, BorderLayout.WEST);
        lastRow.add(cardStatus, BorderLayout.EAST);
        card.add(lastRow);

        JPanel cardHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardHolder.setOpaque(false);
        cardHolder.add(card);
        add(cardHolder, BorderLayout.NORTH);

        // ----- table of all passes -----
        JPanel list = UiUtil.section("All my passes");
        list.setLayout(new BorderLayout());
        list.add(new JScrollPane(passTable), BorderLayout.CENTER);
        JButton refresh = UiUtil.secondaryButton("Refresh");
        refresh.addActionListener(e -> loadPasses());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(refresh);
        list.add(buttons, BorderLayout.SOUTH);
        add(list, BorderLayout.CENTER);

        // clicking a pass in the table shows it on the card
        passTable.getSelectionModel().addListSelectionListener(e -> {
            int row = passTable.getSelectedRow();
            if (!e.getValueIsAdjusting() && row >= 0) {
                showCard(passes.get(row));
            }
        });

        loadPasses();
    }

    // white label for the card
    private JLabel cardLabel(int size, int style) {
        JLabel label = new JLabel(" ");
        label.setForeground(Color.WHITE);
        label.setFont(new Font(Font.SANS_SERIF, style, size));
        return label;
    }

    public void loadPasses() {
        try {
            passes = applicationService.getMyPasses();
            passModel.setRowCount(0);
            for (BusPass p : passes) {
                passModel.addRow(new Object[] {p.getPassNo(), p.getRouteName(), p.getStopName(),
                        p.getPickupTime(), p.getValidFrom(), p.getValidTo(), p.getStatus()});
            }

            // show the first ACTIVE pass on the card
            BusPass activePass = null;
            for (BusPass p : passes) {
                if (p.getStatus().equals("ACTIVE")) {
                    activePass = p;
                    break;
                }
            }
            if (activePass != null) {
                showCard(activePass);
            } else {
                showEmptyCard();
            }
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showCard(BusPass p) {
        cardPassNo.setText(p.getPassNo());
        cardName.setText(p.getStudentName());
        cardRoll.setText("Roll no: " + p.getRollNo());
        cardRoute.setText("Route: " + p.getRouteName() + "   |   Bus: " + p.getBusRegNo());
        cardStop.setText("Boarding: " + p.getStopName() + " at " + p.getPickupTime());
        cardDates.setText("Valid " + p.getValidFrom() + "  to  " + p.getValidTo());
        cardStatus.setText(p.getStatus());
        if (p.getStatus().equals("ACTIVE")) {
            cardStatus.setForeground(UiUtil.YELLOW);
        } else {
            cardStatus.setForeground(new Color(0xFF, 0x9A, 0x9A));   // light red for CANCELLED / EXPIRED
        }
    }

    private void showEmptyCard() {
        cardPassNo.setText("No active pass");
        if (Session.getStudent() != null) {
            cardName.setText(Session.getStudent().getFullName());
        }
        cardRoll.setText("Apply from the 'Routes & Timings' or 'Apply for Pass' tab.");
        cardRoute.setText("Once the transport office approves, your pass appears here.");
        cardStop.setText(" ");
        cardDates.setText(" ");
        cardStatus.setText(" ");
    }
}
