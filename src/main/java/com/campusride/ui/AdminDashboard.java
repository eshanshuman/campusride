package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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

// Main window for the transport admin. It has 3 tabs:
//   1. Approvals       - SUBMITTED applications, capacity check, approve / reject
//   2. Routes & Buses  - add and delete buses, routes and stops
//   3. Passenger Lists - who travels on each route
public class AdminDashboard extends JFrame implements ActionListener {

    private AdminService adminService = new AdminService();
    private JTabbedPane tabs = new JTabbedPane();
    private JButton logoutBtn = UiUtil.secondaryButton("Logout");

    // ----- Approvals tab -----
    private DefaultTableModel pendingModel = UiUtil.readOnlyModel(new String[] {
            "ID", "Student", "Roll no", "Route", "Stop", "From", "To", "Applied on", "Student remarks"});
    private JTable pendingTable = UiUtil.table(pendingModel);
    private Vector<Application> pending = new Vector<Application>();
    private JLabel capRoute = new JLabel("-");
    private JLabel capCapacity = new JLabel("-");
    private JLabel capSeatsUsed = new JLabel("-");
    private JLabel capSeatsLeft = new JLabel("-");
    private JLabel capStudentHasPass = new JLabel("-");
    private JLabel capResult = new JLabel("Select an application.");
    private JTextArea adminRemarks = new JTextArea(3, 22);
    private JButton refreshPendingBtn = UiUtil.secondaryButton("Refresh");
    private JButton rejectBtn = UiUtil.secondaryButton("Reject");
    private JButton approveBtn = UiUtil.primaryButton("Approve & issue pass");

    // ----- Routes & Buses tab -----
    private DefaultTableModel busModel = UiUtil.readOnlyModel(new String[] {"Reg no", "Capacity"});
    private JTable busTable = UiUtil.table(busModel);
    private Vector<Bus> buses = new Vector<Bus>();
    private JTextField newBusReg = new JTextField(10);
    private JTextField newBusCapacity = new JTextField(4);
    private JButton addBusBtn = UiUtil.primaryButton("Add bus");
    private JButton setCapacityBtn = UiUtil.secondaryButton("Set capacity");
    private JButton deleteBusBtn = UiUtil.secondaryButton("Delete");

    private DefaultTableModel routeModel = UiUtil.readOnlyModel(new String[] {"Route", "Bus", "Seats", "Active"});
    private JTable routeTable = UiUtil.table(routeModel);
    private Vector<Route> routes = new Vector<Route>();
    private JTextField newRouteName = new JTextField(12);
    private JComboBox<Bus> freeBusBox = new JComboBox<Bus>();
    private JButton addRouteBtn = UiUtil.primaryButton("Add route");
    private JButton toggleRouteBtn = UiUtil.secondaryButton("Active on/off");
    private JButton deleteRouteBtn = UiUtil.secondaryButton("Delete");

    private DefaultTableModel stopModel = UiUtil.readOnlyModel(new String[] {"#", "Stop", "Pickup"});
    private JTable stopTable = UiUtil.table(stopModel);
    private Vector<Stop> stops = new Vector<Stop>();
    private JTextField newStopName = new JTextField(12);
    private JTextField newStopSeq = new JTextField(3);
    private JTextField newStopTime = new JTextField(5);
    private JButton addStopBtn = UiUtil.primaryButton("Add stop");
    private JButton deleteStopBtn = UiUtil.secondaryButton("Delete");

    // ----- Passenger Lists tab -----
    private JComboBox<Route> passengerRouteBox = new JComboBox<Route>();
    private DefaultTableModel passengerModel = UiUtil.readOnlyModel(new String[] {
            "Pass no", "Student", "Roll no", "Phone", "#", "Stop", "Pickup", "From", "To"});
    private JTable passengerTable = UiUtil.table(passengerModel);
    private Vector<BusPass> passengers = new Vector<BusPass>();
    private JLabel passengerSummary = new JLabel(" ");
    private JButton refreshPassengersBtn = UiUtil.secondaryButton("Refresh");
    private JButton cancelPassBtn = UiUtil.secondaryButton("Cancel selected pass");

    public AdminDashboard() {
        super("CampusRide - Transport Admin");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel header = UiUtil.header("CampusRide", "Transport office - approvals, routes and passenger lists");
        JLabel who = new JLabel("Logged in as " + Session.getUser().getUsername());
        who.setForeground(Color.WHITE);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 6));
        right.setOpaque(false);
        right.add(who);
        right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        tabs.addTab("Approvals", buildApprovalsTab());
        tabs.addTab("Routes & Buses", buildManageTab());
        tabs.addTab("Passenger Lists", buildPassengerTab());

        // reload a tab's data every time it is opened
        tabs.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent e) {
                int tab = tabs.getSelectedIndex();
                if (tab == 0) {
                    loadPending();
                } else if (tab == 1) {
                    loadBusesAndRoutes();
                } else {
                    loadPassengerRoutes();
                }
            }
        });
        add(tabs, BorderLayout.CENTER);

        // every button (and the passenger route combo box) is handled in actionPerformed below
        JButton[] allButtons = {logoutBtn, refreshPendingBtn, rejectBtn, approveBtn,
                addBusBtn, setCapacityBtn, deleteBusBtn, addRouteBtn, toggleRouteBtn, deleteRouteBtn,
                addStopBtn, deleteStopBtn, refreshPassengersBtn, cancelPassBtn};
        for (int i = 0; i < allButtons.length; i++) {
            allButtons[i].addActionListener(this);
        }
        passengerRouteBox.addActionListener(this);

        loadPending();
        setSize(1200, 720);
        setLocationRelativeTo(null);
    }

    public void actionPerformed(ActionEvent e) {
        Object source = e.getSource();
        if (source == logoutBtn) {
            new AuthService().logout();
            dispose();
            new LoginFrame().setVisible(true);
        }
        // approvals tab
        else if (source == refreshPendingBtn) {
            loadPending();
        } else if (source == approveBtn) {
            approveSelected();
        } else if (source == rejectBtn) {
            rejectSelected();
        }
        // routes & buses tab
        else if (source == addBusBtn) {
            addBus();
        } else if (source == setCapacityBtn) {
            changeCapacity();
        } else if (source == deleteBusBtn) {
            deleteBus();
        } else if (source == addRouteBtn) {
            addRoute();
        } else if (source == toggleRouteBtn) {
            toggleRoute();
        } else if (source == deleteRouteBtn) {
            deleteRoute();
        } else if (source == addStopBtn) {
            addStop();
        } else if (source == deleteStopBtn) {
            deleteStop();
        }
        // passenger lists tab
        else if (source == refreshPassengersBtn || source == passengerRouteBox) {
            loadPassengers();
        } else if (source == cancelPassBtn) {
            cancelSelectedPass();
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // =====================================================================
    // TAB 1: APPROVALS
    // =====================================================================

    private JPanel buildApprovalsTab() {
        JPanel tab = new JPanel(new BorderLayout(10, 10));
        tab.setBackground(UiUtil.BACKGROUND);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // left: list of SUBMITTED applications
        JPanel list = UiUtil.section("Applications waiting for a decision (SUBMITTED)");
        list.setLayout(new BorderLayout());
        pendingTable.getColumnModel().getColumn(0).setMaxWidth(40);   // ID column
        list.add(new JScrollPane(pendingTable), BorderLayout.CENTER);
        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        listButtons.setOpaque(false);
        listButtons.add(refreshPendingBtn);
        list.add(listButtons, BorderLayout.SOUTH);
        tab.add(list, BorderLayout.CENTER);

        // right: capacity check box with approve / reject buttons
        JPanel cap = UiUtil.section("Capacity check");
        cap.setLayout(new GridBagLayout());
        UiUtil.addRow(cap, 0, "Route / bus", capRoute);
        UiUtil.addRow(cap, 1, "Bus capacity", capCapacity);
        UiUtil.addRow(cap, 2, "Active passes in period", capSeatsUsed);
        UiUtil.addRow(cap, 3, "Seats left", capSeatsLeft);
        UiUtil.addRow(cap, 4, "Student has overlapping pass?", capStudentHasPass);
        capResult.setFont(capResult.getFont().deriveFont(Font.BOLD, 14f));
        UiUtil.addWide(cap, 5, capResult);
        adminRemarks.setLineWrap(true);
        adminRemarks.setWrapStyleWord(true);
        UiUtil.addRow(cap, 6, "Admin remarks", new JScrollPane(adminRemarks));

        JPanel decide = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        decide.setOpaque(false);
        decide.add(rejectBtn);
        decide.add(approveBtn);
        UiUtil.addWide(cap, 7, decide);

        JPanel capHolder = new JPanel(new BorderLayout());
        capHolder.setOpaque(false);
        capHolder.add(cap, BorderLayout.NORTH);
        tab.add(capHolder, BorderLayout.EAST);

        // when the admin clicks an application, fill the capacity box
        pendingTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    showCapacity();
                }
            }
        });
        return tab;
    }

    private void loadPending() {
        try {
            pending = adminService.getPendingApplications();
            pendingModel.setRowCount(0);
            for (int i = 0; i < pending.size(); i++) {
                Application a = pending.get(i);
                Vector<Object> row = new Vector<Object>();
                row.add(a.getApplicationId());
                row.add(a.getStudentName());
                row.add(a.getRollNo());
                row.add(a.getRouteName());
                row.add(a.getStopName());
                row.add(a.getValidFrom());
                row.add(a.getValidTo());
                row.add(a.getAppliedAt().toLocalDate());
                row.add(a.getRemarks());
                pendingModel.addRow(row);
            }
            showCapacity();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    // returns null if nothing is selected
    private Application getSelectedApplication() {
        int row = pendingTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return pending.get(row);
    }

    private void showCapacity() {
        Application a = getSelectedApplication();
        if (a == null) {
            capRoute.setText("-");
            capCapacity.setText("-");
            capSeatsUsed.setText("-");
            capSeatsLeft.setText("-");
            capStudentHasPass.setText("-");
            capResult.setText("Select an application.");
            capResult.setForeground(UiUtil.NAVY);
            return;
        }
        try {
            CapacityInfo info = adminService.getCapacityInfo(a);
            capRoute.setText(info.getRouteName() + " / " + info.getBusRegNo());
            capCapacity.setText("" + info.getCapacity());
            capSeatsUsed.setText("" + info.getSeatsUsed());
            capSeatsLeft.setText("" + info.getSeatsLeft());
            if (info.isStudentHasPass()) {
                capStudentHasPass.setText("YES");
                capStudentHasPass.setForeground(UiUtil.RED);
            } else {
                capStudentHasPass.setText("No");
                capStudentHasPass.setForeground(UiUtil.GREEN);
            }

            if (info.canApprove()) {
                capResult.setText("OK to approve");
                capResult.setForeground(UiUtil.GREEN);
            } else if (info.isStudentHasPass()) {
                capResult.setText("Cannot approve: student already has a pass");
                capResult.setForeground(UiUtil.RED);
            } else {
                capResult.setText("Cannot approve: bus is full");
                capResult.setForeground(UiUtil.RED);
            }
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void approveSelected() {
        Application a = getSelectedApplication();
        if (a == null) {
            JOptionPane.showMessageDialog(this, "Please select an application first.");
            return;
        }
        try {
            // approve() checks everything again inside one transaction
            String passNo = adminService.approve(a.getApplicationId(), adminRemarks.getText());
            JOptionPane.showMessageDialog(this, "Approved. Pass " + passNo + " issued to " + a.getStudentName() + ".");
            adminRemarks.setText("");
        } catch (ServiceException ex) {
            showError("Not approved: " + ex.getMessage());
        }
        loadPending();
    }

    private void rejectSelected() {
        Application a = getSelectedApplication();
        if (a == null) {
            JOptionPane.showMessageDialog(this, "Please select an application first.");
            return;
        }
        if (adminRemarks.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please type the reason for rejection in 'Admin remarks'.");
            return;
        }
        try {
            adminService.reject(a.getApplicationId(), adminRemarks.getText());
            JOptionPane.showMessageDialog(this, "Application #" + a.getApplicationId() + " rejected.");
            adminRemarks.setText("");
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
        loadPending();
    }

    // =====================================================================
    // TAB 2: ROUTES & BUSES
    // =====================================================================

    private JPanel buildManageTab() {
        JPanel tab = new JPanel(new GridLayout(1, 3, 10, 0));   // 3 columns: buses, routes, stops
        tab.setBackground(UiUtil.BACKGROUND);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ----- buses -----
        JPanel busBox = UiUtil.section("Buses");
        busBox.setLayout(new BorderLayout(0, 6));
        busBox.add(new JScrollPane(busTable), BorderLayout.CENTER);

        JPanel busForm = new JPanel(new GridBagLayout());
        busForm.setOpaque(false);
        UiUtil.addRow(busForm, 0, "Reg no", newBusReg);
        UiUtil.addRow(busForm, 1, "Capacity", newBusCapacity);
        JPanel busButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        busButtons.setOpaque(false);
        busButtons.add(addBusBtn);
        busButtons.add(setCapacityBtn);
        busButtons.add(deleteBusBtn);
        UiUtil.addWide(busForm, 2, busButtons);
        busBox.add(busForm, BorderLayout.SOUTH);
        tab.add(busBox);

        // ----- routes -----
        JPanel routeBox = UiUtil.section("Routes (one bus each)");
        routeBox.setLayout(new BorderLayout(0, 6));
        routeBox.add(new JScrollPane(routeTable), BorderLayout.CENTER);

        JPanel routeForm = new JPanel(new GridBagLayout());
        routeForm.setOpaque(false);
        UiUtil.addRow(routeForm, 0, "Route name", newRouteName);
        UiUtil.addRow(routeForm, 1, "Free bus", freeBusBox);
        JPanel routeButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        routeButtons.setOpaque(false);
        routeButtons.add(addRouteBtn);
        routeButtons.add(toggleRouteBtn);
        routeButtons.add(deleteRouteBtn);
        UiUtil.addWide(routeForm, 2, routeButtons);
        routeBox.add(routeForm, BorderLayout.SOUTH);
        tab.add(routeBox);

        // ----- stops of the selected route -----
        JPanel stopBox = UiUtil.section("Stops of selected route");
        stopBox.setLayout(new BorderLayout(0, 6));
        stopTable.getColumnModel().getColumn(0).setMaxWidth(40);   // "#" column
        stopBox.add(new JScrollPane(stopTable), BorderLayout.CENTER);

        JPanel stopForm = new JPanel(new GridBagLayout());
        stopForm.setOpaque(false);
        UiUtil.addRow(stopForm, 0, "Stop name", newStopName);
        UiUtil.addRow(stopForm, 1, "Order (1, 2, ...)", newStopSeq);
        UiUtil.addRow(stopForm, 2, "Pickup (HH:MM)", newStopTime);
        JPanel stopButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        stopButtons.setOpaque(false);
        stopButtons.add(addStopBtn);
        stopButtons.add(deleteStopBtn);
        UiUtil.addWide(stopForm, 3, stopButtons);
        stopBox.add(stopForm, BorderLayout.SOUTH);
        tab.add(stopBox);

        // clicking a route shows its stops
        routeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    loadStops();
                }
            }
        });
        return tab;
    }

    private void loadBusesAndRoutes() {
        try {
            buses = adminService.getAllBuses();
            busModel.setRowCount(0);
            for (int i = 0; i < buses.size(); i++) {
                Bus b = buses.get(i);
                Vector<Object> row = new Vector<Object>();
                row.add(b.getRegNo());
                row.add(b.getCapacity());
                busModel.addRow(row);
            }

            routes = adminService.getAllRoutes();
            routeModel.setRowCount(0);
            for (int i = 0; i < routes.size(); i++) {
                Route r = routes.get(i);
                Vector<Object> row = new Vector<Object>();
                row.add(r.getRouteName());
                row.add(r.getBusRegNo());
                row.add(r.getCapacity());
                if (r.isActive()) {
                    row.add("Yes");
                } else {
                    row.add("No");
                }
                routeModel.addRow(row);
            }

            // the combo box shows the buses straight from the Vector
            Vector<Bus> freeBuses = adminService.getUnassignedBuses();
            freeBusBox.setModel(new DefaultComboBoxModel<Bus>(freeBuses));

            stopModel.setRowCount(0);
            stops = new Vector<Stop>();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private Bus getSelectedBus() {
        int row = busTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return buses.get(row);
    }

    private Route getSelectedRoute() {
        int row = routeTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        return routes.get(row);
    }

    private void loadStops() {
        stopModel.setRowCount(0);
        stops = new Vector<Stop>();
        Route r = getSelectedRoute();
        if (r == null) {
            return;
        }
        try {
            stops = adminService.getStops(r.getRouteId());
            for (int i = 0; i < stops.size(); i++) {
                Stop s = stops.get(i);
                Vector<Object> row = new Vector<Object>();
                row.add(s.getStopSeq());
                row.add(s.getStopName());
                row.add(s.getPickupTime());
                stopModel.addRow(row);
            }
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void addBus() {
        try {
            adminService.addBus(newBusReg.getText(), newBusCapacity.getText());
            newBusReg.setText("");
            newBusCapacity.setText("");
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void changeCapacity() {
        Bus b = getSelectedBus();
        if (b == null) {
            JOptionPane.showMessageDialog(this, "Please select a bus first.");
            return;
        }
        String value = JOptionPane.showInputDialog(this, "New capacity for " + b.getRegNo() + ":", b.getCapacity());
        if (value == null) {
            return;   // admin pressed Cancel
        }
        try {
            adminService.changeCapacity(b.getBusId(), value);
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteBus() {
        Bus b = getSelectedBus();
        if (b == null) {
            JOptionPane.showMessageDialog(this, "Please select a bus first.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete bus " + b.getRegNo() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            adminService.deleteBus(b.getBusId());
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void addRoute() {
        try {
            adminService.addRoute(newRouteName.getText(), (Bus) freeBusBox.getSelectedItem());
            newRouteName.setText("");
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void toggleRoute() {
        Route r = getSelectedRoute();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Please select a route first.");
            return;
        }
        try {
            adminService.setRouteActive(r.getRouteId(), !r.isActive());
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteRoute() {
        Route r = getSelectedRoute();
        if (r == null) {
            JOptionPane.showMessageDialog(this, "Please select a route first.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete route " + r.getRouteName() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            adminService.deleteRoute(r.getRouteId());
            loadBusesAndRoutes();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void addStop() {
        try {
            adminService.addStop(getSelectedRoute(), newStopName.getText(), newStopSeq.getText(), newStopTime.getText());
            newStopName.setText("");
            newStopSeq.setText("");
            newStopTime.setText("");
            loadStops();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteStop() {
        int row = stopTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a stop first.");
            return;
        }
        Stop s = stops.get(row);
        int answer = JOptionPane.showConfirmDialog(this, "Delete stop " + s.getStopName() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            adminService.deleteStop(s.getStopId());
            loadStops();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    // =====================================================================
    // TAB 3: PASSENGER LISTS
    // =====================================================================

    private JPanel buildPassengerTab() {
        JPanel tab = new JPanel(new BorderLayout(10, 10));
        tab.setBackground(UiUtil.BACKGROUND);
        tab.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JPanel top = UiUtil.section("Route");
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 2));
        top.add(new JLabel("Show passengers of"));
        top.add(passengerRouteBox);
        top.add(refreshPassengersBtn);
        passengerSummary.setFont(passengerSummary.getFont().deriveFont(Font.BOLD));
        top.add(passengerSummary);
        tab.add(top, BorderLayout.NORTH);

        JPanel list = UiUtil.section("ACTIVE passes (current and upcoming), in stop order");
        list.setLayout(new BorderLayout());
        passengerTable.getColumnModel().getColumn(4).setMaxWidth(40);   // "#" column
        list.add(new JScrollPane(passengerTable), BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.setOpaque(false);
        buttons.add(cancelPassBtn);
        list.add(buttons, BorderLayout.SOUTH);
        tab.add(list, BorderLayout.CENTER);
        return tab;
    }

    // fills the route combo box, and keeps the route that was selected before
    private void loadPassengerRoutes() {
        try {
            Route before = (Route) passengerRouteBox.getSelectedItem();
            Vector<Route> allRoutes = adminService.getAllRoutes();
            passengerRouteBox.removeAllItems();
            for (int i = 0; i < allRoutes.size(); i++) {
                Route r = allRoutes.get(i);
                passengerRouteBox.addItem(r);
                if (before != null && r.getRouteId() == before.getRouteId()) {
                    passengerRouteBox.setSelectedItem(r);
                }
            }
            loadPassengers();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void loadPassengers() {
        passengerModel.setRowCount(0);
        passengers = new Vector<BusPass>();
        Route r = (Route) passengerRouteBox.getSelectedItem();
        if (r == null) {
            passengerSummary.setText(" ");
            return;
        }
        try {
            passengers = adminService.getPassengers(r.getRouteId());
            for (int i = 0; i < passengers.size(); i++) {
                BusPass p = passengers.get(i);
                Vector<Object> row = new Vector<Object>();
                row.add(p.getPassNo());
                row.add(p.getStudentName());
                row.add(p.getRollNo());
                row.add(p.getPhone());
                row.add(p.getStopSeq());
                row.add(p.getStopName());
                row.add(p.getPickupTime());
                row.add(p.getValidFrom());
                row.add(p.getValidTo());
                passengerModel.addRow(row);
            }
            passengerSummary.setText(passengers.size() + " passenger(s) - bus " + r.getBusRegNo()
                    + ", capacity " + r.getCapacity());
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }

    private void cancelSelectedPass() {
        int row = passengerTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a pass first.");
            return;
        }
        BusPass p = passengers.get(row);
        int answer = JOptionPane.showConfirmDialog(this, "Cancel pass " + p.getPassNo() + " of " + p.getStudentName()
                + "? The seat becomes free again.", "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            adminService.cancelPass(p.getPassId());
            loadPassengers();
        } catch (ServiceException ex) {
            showError(ex.getMessage());
        }
    }
}
