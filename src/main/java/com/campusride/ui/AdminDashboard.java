package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

import com.campusride.model.Application;
import com.campusride.model.Bus;
import com.campusride.model.BusPass;
import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.service.AdminService;
import com.campusride.service.AuthService;
import com.campusride.service.CapacityInfo;
import com.campusride.service.ServiceException;
import com.campusride.util.Session;

/**
 * Screen: the transport admin's window, with three tabs:
 *   1. Approvals        - SUBMITTED applications + capacity panel + approve/reject
 *   2. Routes & Buses   - maintain buses, routes and stops
 *   3. Passenger Lists  - who travels on each route
 * Each tab is built by its own build...Tab() method below.
 */
public class AdminDashboard extends JFrame {

    private final AdminService adminService = new AdminService();
    private final JTabbedPane tabs = new JTabbedPane();

    // ===== Approvals tab =====
    private final DefaultTableModel pendingModel = UiUtil.readOnlyModel(
            "ID", "Student", "Roll no", "Route", "Stop", "From", "To", "Applied on", "Student remarks");
    private final JTable pendingTable = UiUtil.table(pendingModel);
    private List<Application> pending = new ArrayList<>();
    private final JLabel capRoute = new JLabel("-");
    private final JLabel capCapacity = new JLabel("-");
    private final JLabel capActive = new JLabel("-");
    private final JLabel capSeatsLeft = new JLabel("-");
    private final JLabel capOverlap = new JLabel("-");
    private final JLabel capVerdict = new JLabel("Select an application.");
    private final JTextArea adminRemarks = new JTextArea(3, 22);

    // ===== Routes & Buses tab =====
    private final DefaultTableModel busModel = UiUtil.readOnlyModel("Reg no", "Capacity");
    private final JTable busTable = UiUtil.table(busModel);
    private List<Bus> buses = new ArrayList<>();
    private final JTextField newBusReg = new JTextField(10);
    private final JTextField newBusCap = new JTextField(4);

    private final DefaultTableModel routeModel = UiUtil.readOnlyModel("Route", "Bus", "Seats", "Active");
    private final JTable routeTable = UiUtil.table(routeModel);
    private List<Route> routes = new ArrayList<>();
    private final JTextField newRouteName = new JTextField(12);
    private final JComboBox<Bus> freeBusBox = new JComboBox<>();

    private final DefaultTableModel stopModel = UiUtil.readOnlyModel("#", "Stop", "Pickup");
    private final JTable stopTable = UiUtil.table(stopModel);
    private List<Stop> stops = new ArrayList<>();
    private final JTextField newStopName = new JTextField(12);
    private final JTextField newStopSeq = new JTextField(3);
    private final JTextField newStopTime = new JTextField(5);

    // ===== Passenger Lists tab =====
    private final JComboBox<Route> passengerRouteBox = new JComboBox<>();
    private final DefaultTableModel passengerModel = UiUtil.readOnlyModel(
            "Pass no", "Student", "Roll no", "Phone", "#", "Stop", "Pickup", "From", "To");
    private final JTable passengerTable = UiUtil.table(passengerModel);
    private List<BusPass> passengers = new ArrayList<>();
    private final JLabel passengerSummary = new JLabel(" ");

    public AdminDashboard() {
        super("CampusRide - Transport Admin");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel header = UiUtil.header("CampusRide", "Transport office - approvals, routes and passenger lists");
        JLabel who = new JLabel("Logged in as " + Session.getUser().getUsername());
        who.setForeground(Color.WHITE);
        JButton logout = UiUtil.secondaryButton("Logout");
        logout.addActionListener(e -> logout());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        right.setOpaque(false);
        right.add(who);
        right.add(logout);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tabs.addTab("Approvals", buildApprovalsTab());
        tabs.addTab("Routes & Buses", buildManageTab());
        tabs.addTab("Passenger Lists", buildPassengerTab());
        // Reload a tab's data whenever it is opened, so it is never stale
        tabs.addChangeListener(e -> {
            switch (tabs.getSelectedIndex()) {
                case 0: loadPending(); break;
                case 1: loadBusesAndRoutes(); break;
                default: loadPassengerRoutes(); break;
            }
        });
        add(tabs, BorderLayout.CENTER);

        loadPending();
        setSize(1200, 720);
        setLocationRelativeTo(null);
    }

    // =====================================================================
    //  TAB 1: APPROVALS
    // =====================================================================

    private JPanel buildApprovalsTab() {
        JPanel tab = new JPanel(new BorderLayout(10, 10));
        tab.setBackground(UiUtil.BACKGROUND);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel list = UiUtil.section("Applications waiting for a decision (SUBMITTED)");
        list.setLayout(new BorderLayout());
        UiUtil.narrowColumn(pendingTable, 0, 40);
        list.add(UiUtil.scroll(pendingTable), BorderLayout.CENTER);
        JButton refresh = UiUtil.secondaryButton("Refresh");
        refresh.addActionListener(e -> loadPending());
        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        listButtons.setOpaque(false);
        listButtons.add(refresh);
        list.add(listButtons, BorderLayout.SOUTH);
        tab.add(list, BorderLayout.CENTER);

        // ---- capacity panel on the right ----
        JPanel cap = UiUtil.section("Capacity check");
        cap.setLayout(new GridBagLayout());
        int row = 0;
        UiUtil.addRow(cap, row++, "Route / bus", capRoute);
        UiUtil.addRow(cap, row++, "Bus capacity", capCapacity);
        UiUtil.addRow(cap, row++, "Active passes in period", capActive);
        UiUtil.addRow(cap, row++, "Seats left", capSeatsLeft);
        UiUtil.addRow(cap, row++, "Student has overlapping pass?", capOverlap);
        capVerdict.setFont(capVerdict.getFont().deriveFont(Font.BOLD, 14f));
        UiUtil.addWide(cap, row++, capVerdict);
        adminRemarks.setLineWrap(true);
        adminRemarks.setWrapStyleWord(true);
        UiUtil.addRow(cap, row++, "Admin remarks", UiUtil.scroll(adminRemarks));

        JButton approve = UiUtil.primaryButton("Approve & issue pass");
        approve.addActionListener(e -> approveSelected());
        JButton reject = UiUtil.secondaryButton("Reject");
        reject.addActionListener(e -> rejectSelected());
        JPanel decide = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        decide.setOpaque(false);
        decide.add(reject);
        decide.add(approve);
        UiUtil.addWide(cap, row, decide);

        JPanel capHolder = new JPanel(new BorderLayout());
        capHolder.setOpaque(false);
        capHolder.add(cap, BorderLayout.NORTH);
        tab.add(capHolder, BorderLayout.EAST);

        pendingTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showCapacity();
            }
        });
        return tab;
    }

    private void loadPending() {
        try {
            pending = adminService.getPendingApplications();
            pendingModel.setRowCount(0);
            for (Application a : pending) {
                pendingModel.addRow(new Object[] {a.getApplicationId(), a.getStudentName(), a.getRollNo(),
                        a.getRouteName(), a.getStopName(), a.getValidFrom(), a.getValidTo(),
                        a.getAppliedAt().toLocalDate(), UiUtil.text(a.getRemarks())});
            }
            showCapacity();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private Application selectedPending() {
        int row = pendingTable.getSelectedRow();
        return row < 0 ? null : pending.get(row);
    }

    /** Fills the capacity panel for the selected application. */
    private void showCapacity() {
        Application a = selectedPending();
        if (a == null) {
            for (JLabel l : new JLabel[] {capRoute, capCapacity, capActive, capSeatsLeft, capOverlap}) {
                l.setText("-");
            }
            capVerdict.setText("Select an application.");
            capVerdict.setForeground(UiUtil.NAVY);
            return;
        }
        try {
            CapacityInfo info = adminService.getCapacityInfo(a);
            capRoute.setText(info.getRouteName() + " / " + info.getBusRegNo());
            capCapacity.setText(String.valueOf(info.getCapacity()));
            capActive.setText(String.valueOf(info.getActiveOverlapping()));
            capSeatsLeft.setText(String.valueOf(info.getSeatsLeft()));
            capOverlap.setText(info.isStudentHasOverlap() ? "YES" : "No");
            capOverlap.setForeground(info.isStudentHasOverlap() ? UiUtil.RED : UiUtil.GREEN);
            if (info.canApprove()) {
                capVerdict.setText("OK to approve");
                capVerdict.setForeground(UiUtil.GREEN);
            } else if (info.isStudentHasOverlap()) {
                capVerdict.setText("Cannot approve: student already has a pass");
                capVerdict.setForeground(UiUtil.RED);
            } else {
                capVerdict.setText("Cannot approve: bus is full");
                capVerdict.setForeground(UiUtil.RED);
            }
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void approveSelected() {
        Application a = selectedPending();
        if (a == null) {
            UiUtil.error(this, "Please select an application first.");
            return;
        }
        try {
            // The service re-checks everything inside ONE transaction.
            String passNo = adminService.approve(a.getApplicationId(), adminRemarks.getText());
            UiUtil.info(this, "Approved. Pass " + passNo + " issued to " + a.getStudentName() + ".");
            adminRemarks.setText("");
        } catch (ServiceException ex) {
            UiUtil.error(this, "Not approved: " + ex.getMessage());
        }
        loadPending();
    }

    private void rejectSelected() {
        Application a = selectedPending();
        if (a == null) {
            UiUtil.error(this, "Please select an application first.");
            return;
        }
        if (adminRemarks.getText().trim().isEmpty()) {
            UiUtil.error(this, "Please type the reason for rejection in 'Admin remarks'.");
            return;
        }
        try {
            adminService.reject(a.getApplicationId(), adminRemarks.getText());
            UiUtil.info(this, "Application #" + a.getApplicationId() + " rejected.");
            adminRemarks.setText("");
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
        loadPending();
    }

    // =====================================================================
    //  TAB 2: ROUTES & BUSES
    // =====================================================================

    private JPanel buildManageTab() {
        JPanel tab = new JPanel(new GridLayout(1, 3, 10, 0));
        tab.setBackground(UiUtil.BACKGROUND);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---- buses ----
        JPanel busBox = UiUtil.section("Buses");
        busBox.setLayout(new BorderLayout(0, 6));
        busBox.add(UiUtil.scroll(busTable), BorderLayout.CENTER);
        JPanel busForm = new JPanel(new GridBagLayout());
        busForm.setOpaque(false);
        UiUtil.addRow(busForm, 0, "Reg no", newBusReg);
        UiUtil.addRow(busForm, 1, "Capacity", newBusCap);
        busForm.add(buttonRow(
                button("Add bus", true, e -> addBus()),
                button("Set capacity", false, e -> changeCapacity()),
                button("Delete", false, e -> deleteBus())), wideAt(2));
        busBox.add(busForm, BorderLayout.SOUTH);
        tab.add(busBox);

        // ---- routes ----
        JPanel routeBox = UiUtil.section("Routes (one bus each)");
        routeBox.setLayout(new BorderLayout(0, 6));
        routeBox.add(UiUtil.scroll(routeTable), BorderLayout.CENTER);
        JPanel routeForm = new JPanel(new GridBagLayout());
        routeForm.setOpaque(false);
        UiUtil.addRow(routeForm, 0, "Route name", newRouteName);
        UiUtil.addRow(routeForm, 1, "Free bus", freeBusBox);
        routeForm.add(buttonRow(
                button("Add route", true, e -> addRoute()),
                button("Active on/off", false, e -> toggleRoute()),
                button("Delete", false, e -> deleteRoute())), wideAt(2));
        routeBox.add(routeForm, BorderLayout.SOUTH);
        tab.add(routeBox);

        // ---- stops of the selected route ----
        JPanel stopBox = UiUtil.section("Stops of selected route");
        stopBox.setLayout(new BorderLayout(0, 6));
        UiUtil.narrowColumn(stopTable, 0, 40);
        stopBox.add(UiUtil.scroll(stopTable), BorderLayout.CENTER);
        JPanel stopForm = new JPanel(new GridBagLayout());
        stopForm.setOpaque(false);
        UiUtil.addRow(stopForm, 0, "Stop name", newStopName);
        UiUtil.addRow(stopForm, 1, "Order (1, 2, ...)", newStopSeq);
        UiUtil.addRow(stopForm, 2, "Pickup (HH:MM)", newStopTime);
        stopForm.add(buttonRow(
                button("Add stop", true, e -> addStop()),
                button("Delete", false, e -> deleteStop())), wideAt(3));
        stopBox.add(stopForm, BorderLayout.SOUTH);
        tab.add(stopBox);

        routeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadStops();
            }
        });
        return tab;
    }

    // small layout helpers for this tab
    private JButton button(String text, boolean primary, java.awt.event.ActionListener action) {
        JButton b = primary ? UiUtil.primaryButton(text) : UiUtil.secondaryButton(text);
        b.addActionListener(action);
        return b;
    }

    private JPanel buttonRow(JButton... buttons) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        p.setOpaque(false);
        for (JButton b : buttons) {
            p.add(b);
        }
        return p;
    }

    private java.awt.GridBagConstraints wideAt(int row) {
        java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
        c.gridx = 0;
        c.gridy = row;
        c.gridwidth = 2;
        c.anchor = java.awt.GridBagConstraints.LINE_END;
        c.insets = new java.awt.Insets(6, 0, 0, 0);
        return c;
    }

    private void loadBusesAndRoutes() {
        try {
            buses = adminService.getAllBuses();
            busModel.setRowCount(0);
            for (Bus b : buses) {
                busModel.addRow(new Object[] {b.getRegNo(), b.getCapacity()});
            }
            routes = adminService.getAllRoutes();
            routeModel.setRowCount(0);
            for (Route r : routes) {
                routeModel.addRow(new Object[] {r.getRouteName(), r.getBusRegNo(), r.getCapacity(),
                        r.isActive() ? "Yes" : "No"});
            }
            freeBusBox.removeAllItems();
            for (Bus b : adminService.getUnassignedBuses()) {
                freeBusBox.addItem(b);
            }
            stopModel.setRowCount(0);
            stops = new ArrayList<>();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private Bus selectedBus() {
        int row = busTable.getSelectedRow();
        return row < 0 ? null : buses.get(row);
    }

    private Route selectedRoute() {
        int row = routeTable.getSelectedRow();
        return row < 0 ? null : routes.get(row);
    }

    private void loadStops() {
        stopModel.setRowCount(0);
        stops = new ArrayList<>();
        Route r = selectedRoute();
        if (r == null) {
            return;
        }
        try {
            stops = adminService.getStops(r.getRouteId());
            for (Stop s : stops) {
                stopModel.addRow(new Object[] {s.getStopSeq(), s.getStopName(), s.getPickupTime()});
            }
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void addBus() {
        try {
            adminService.addBus(newBusReg.getText(), newBusCap.getText());
            newBusReg.setText("");
            newBusCap.setText("");
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void changeCapacity() {
        Bus b = selectedBus();
        if (b == null) {
            UiUtil.error(this, "Please select a bus first.");
            return;
        }
        String value = JOptionPane.showInputDialog(this, "New capacity for " + b.getRegNo() + ":", b.getCapacity());
        if (value == null) {
            return;   // cancelled
        }
        try {
            adminService.changeCapacity(b.getBusId(), value);
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void deleteBus() {
        Bus b = selectedBus();
        if (b == null) {
            UiUtil.error(this, "Please select a bus first.");
            return;
        }
        if (!UiUtil.confirm(this, "Delete bus " + b.getRegNo() + "?")) return;
        try {
            adminService.deleteBus(b.getBusId());
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void addRoute() {
        try {
            adminService.addRoute(newRouteName.getText(), (Bus) freeBusBox.getSelectedItem());
            newRouteName.setText("");
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void toggleRoute() {
        Route r = selectedRoute();
        if (r == null) {
            UiUtil.error(this, "Please select a route first.");
            return;
        }
        try {
            adminService.setRouteActive(r.getRouteId(), !r.isActive());
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void deleteRoute() {
        Route r = selectedRoute();
        if (r == null) {
            UiUtil.error(this, "Please select a route first.");
            return;
        }
        if (!UiUtil.confirm(this, "Delete route " + r.getRouteName() + "?")) return;
        try {
            adminService.deleteRoute(r.getRouteId());
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void addStop() {
        Route r = selectedRoute();
        try {
            adminService.addStop(r, newStopName.getText(), newStopSeq.getText(), newStopTime.getText());
            newStopName.setText("");
            newStopSeq.setText("");
            newStopTime.setText("");
            loadStops();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void deleteStop() {
        int row = stopTable.getSelectedRow();
        if (row < 0) {
            UiUtil.error(this, "Please select a stop first.");
            return;
        }
        Stop s = stops.get(row);
        if (!UiUtil.confirm(this, "Delete stop " + s.getStopName() + "?")) return;
        try {
            adminService.deleteStop(s.getStopId());
            loadStops();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    // =====================================================================
    //  TAB 3: PASSENGER LISTS
    // =====================================================================

    private JPanel buildPassengerTab() {
        JPanel tab = new JPanel(new BorderLayout(10, 10));
        tab.setBackground(UiUtil.BACKGROUND);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = UiUtil.section("Route");
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 2));
        top.add(new JLabel("Show passengers of"));
        top.add(passengerRouteBox);
        JButton refresh = UiUtil.secondaryButton("Refresh");
        refresh.addActionListener(e -> loadPassengers());
        top.add(refresh);
        passengerSummary.setFont(passengerSummary.getFont().deriveFont(Font.BOLD));
        top.add(passengerSummary);
        tab.add(top, BorderLayout.NORTH);

        JPanel list = UiUtil.section("ACTIVE passes (current and upcoming), in stop order");
        list.setLayout(new BorderLayout());
        UiUtil.narrowColumn(passengerTable, 4, 40);
        list.add(UiUtil.scroll(passengerTable), BorderLayout.CENTER);
        JButton cancel = UiUtil.secondaryButton("Cancel selected pass");
        cancel.addActionListener(e -> cancelSelectedPass());
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(cancel);
        list.add(buttons, BorderLayout.SOUTH);
        tab.add(list, BorderLayout.CENTER);

        passengerRouteBox.addActionListener(e -> loadPassengers());
        return tab;
    }

    private void loadPassengerRoutes() {
        try {
            Route previous = (Route) passengerRouteBox.getSelectedItem();
            List<Route> all = adminService.getAllRoutes();
            passengerRouteBox.removeAllItems();   // fires the listener; harmless
            for (Route r : all) {
                passengerRouteBox.addItem(r);
                if (previous != null && r.getRouteId() == previous.getRouteId()) {
                    passengerRouteBox.setSelectedItem(r);
                }
            }
            loadPassengers();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void loadPassengers() {
        passengerModel.setRowCount(0);
        passengers = new ArrayList<>();
        Route r = (Route) passengerRouteBox.getSelectedItem();
        if (r == null) {
            passengerSummary.setText(" ");
            return;
        }
        try {
            passengers = adminService.getPassengers(r.getRouteId());
            for (BusPass p : passengers) {
                passengerModel.addRow(new Object[] {p.getPassNo(), p.getStudentName(), p.getRollNo(), p.getPhone(),
                        p.getStopSeq(), p.getStopName(), p.getPickupTime(), p.getValidFrom(), p.getValidTo()});
            }
            passengerSummary.setText(passengers.size() + " passenger(s) - bus " + r.getBusRegNo()
                    + ", capacity " + r.getCapacity());
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void cancelSelectedPass() {
        int row = passengerTable.getSelectedRow();
        if (row < 0) {
            UiUtil.error(this, "Please select a pass first.");
            return;
        }
        BusPass p = passengers.get(row);
        if (!UiUtil.confirm(this, "Cancel pass " + p.getPassNo() + " of " + p.getStudentName()
                + "? The seat becomes free again.")) return;
        try {
            adminService.cancelPass(p.getPassId());
            loadPassengers();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void logout() {
        new AuthService().logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
