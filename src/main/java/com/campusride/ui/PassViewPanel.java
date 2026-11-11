package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.campusride.model.BusPass;
import com.campusride.service.ApplicationService;
import com.campusride.service.ServiceException;
import com.campusride.util.Session;

/**
 * Screen: "My Bus Pass".
 * A pass "card" (navy with a yellow stripe) for the selected pass, and a
 * table of all the student's passes (ACTIVE / CANCELLED / EXPIRED).
 */
public class PassViewPanel extends JPanel {

    private final ApplicationService applicationService = new ApplicationService();

    private final DefaultTableModel passModel = UiUtil.readOnlyModel(
            "Pass No", "Route", "Stop", "Pickup", "From", "To", "Status");
    private final JTable passTable = UiUtil.table(passModel);
    private List<BusPass> passes = new ArrayList<>();

    // labels on the pass card
    private final JLabel cardPassNo = cardLabel(22, Font.BOLD);
    private final JLabel cardName = cardLabel(15, Font.BOLD);
    private final JLabel cardRoll = cardLabel(13, Font.PLAIN);
    private final JLabel cardRoute = cardLabel(13, Font.PLAIN);
    private final JLabel cardStop = cardLabel(13, Font.PLAIN);
    private final JLabel cardDates = cardLabel(13, Font.PLAIN);
    private final JLabel cardStatus = cardLabel(15, Font.BOLD);

    public PassViewPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---- the pass card ----
        JPanel card = new JPanel(new GridLayout(7, 1, 0, 2));
        card.setBackground(UiUtil.NAVY);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 10, 0, 0, UiUtil.YELLOW),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));
        JLabel brand = cardLabel(12, Font.BOLD);
        brand.setText("CAMPUSRIDE  -  COLLEGE BUS PASS");
        brand.setForeground(UiUtil.YELLOW);
        card.add(brand);
        card.add(cardPassNo);
        card.add(cardName);
        card.add(cardRoll);
        card.add(cardRoute);
        card.add(cardStop);
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);
        bottomRow.add(cardDates, BorderLayout.WEST);
        bottomRow.add(cardStatus, BorderLayout.EAST);
        card.add(bottomRow);

        JPanel cardHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        cardHolder.setOpaque(false);
        card.setPreferredSize(new java.awt.Dimension(520, 230));
        cardHolder.add(card);
        add(cardHolder, BorderLayout.NORTH);

        // ---- table of all passes ----
        JPanel list = UiUtil.section("All my passes");
        list.setLayout(new BorderLayout());
        list.add(UiUtil.scroll(passTable), BorderLayout.CENTER);
        JButton refresh = UiUtil.secondaryButton("Refresh");
        refresh.addActionListener(e -> loadPasses());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(refresh);
        list.add(buttons, BorderLayout.SOUTH);
        add(list, BorderLayout.CENTER);

        passTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && passTable.getSelectedRow() >= 0) {
                showCard(passes.get(passTable.getSelectedRow()));
            }
        });

        loadPasses();
    }

    private static JLabel cardLabel(int size, int style) {
        JLabel l = new JLabel(" ");
        l.setForeground(Color.WHITE);
        l.setFont(new Font(Font.SANS_SERIF, style, size));
        return l;
    }

    /** Loads passes; shows the first ACTIVE one on the card (or a message). */
    public void loadPasses() {
        try {
            passes = applicationService.getMyPasses();
            passModel.setRowCount(0);
            for (BusPass p : passes) {
                passModel.addRow(new Object[] {p.getPassNo(), p.getRouteName(), p.getStopName(),
                        p.getPickupTime(), p.getValidFrom(), p.getValidTo(), p.getStatus()});
            }
            BusPass show = null;
            for (BusPass p : passes) {
                if ("ACTIVE".equals(p.getStatus())) {
                    show = p;
                    break;
                }
            }
            if (show != null) {
                showCard(show);
            } else {
                showEmptyCard();
            }
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
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
        cardStatus.setForeground("ACTIVE".equals(p.getStatus()) ? UiUtil.YELLOW : new Color(0xFF, 0x9A, 0x9A));
    }

    private void showEmptyCard() {
        cardPassNo.setText("No active pass");
        cardName.setText(Session.getStudent() == null ? "" : Session.getStudent().getFullName());
        cardRoll.setText("Apply from the 'Routes & Timings' or 'Apply for Pass' tab.");
        cardRoute.setText("Once the transport office approves, your pass appears here.");
        cardStop.setText(" ");
        cardDates.setText(" ");
        cardStatus.setText(" ");
    }
}
