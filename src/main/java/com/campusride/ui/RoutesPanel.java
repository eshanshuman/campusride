package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.service.RouteService;
import com.campusride.service.ServiceException;

// "Routes & Timings" tab.
// Left: running routes. Right: stops of the selected route.
// Top: pick a period to see how many seats are left on that route.
public class RoutesPanel extends JPanel implements ActionListener {

    private RouteService routeService = new RouteService();
    private StudentDashboard dashboard;

    private JSpinner fromDate = UiUtil.dateSpinner(LocalDate.now().plusDays(1));
    private JSpinner toDate = UiUtil.dateSpinner(LocalDate.now().plusMonths(6));
    private JLabel seatsLabel = new JLabel("Select a route to see seats left.");
    private JButton checkBtn = UiUtil.secondaryButton("Check seats");
    private JButton applyBtn = UiUtil.primaryButton("Apply for this stop");

    private DefaultTableModel routeModel = UiUtil.readOnlyModel(new String[] {"Route", "Bus", "Capacity"});
    private JTable routeTable = UiUtil.table(routeModel);
    private DefaultTableModel stopModel = UiUtil.readOnlyModel(new String[] {"#", "Stop", "Pickup time"});
    private JTable stopTable = UiUtil.table(stopModel);

    // the objects shown in each table, in the same order as the rows
    private Vector<Route> routes = new Vector<Route>();
    private Vector<Stop> stops = new Vector<Stop>();

    public RoutesPanel(StudentDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout(10, 10));
        setBackground(UiUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // top: travel period and seats left
        JPanel top = UiUtil.section("Travel period");
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 2));
        top.add(new JLabel("From"));
        top.add(fromDate);
        top.add(new JLabel("To"));
        top.add(toDate);
        checkBtn.addActionListener(this);
        top.add(checkBtn);
        seatsLabel.setFont(seatsLabel.getFont().deriveFont(Font.BOLD));
        top.add(seatsLabel);
        add(top, BorderLayout.NORTH);

        // left: routes
        JPanel routesBox = UiUtil.section("Bus routes");
        routesBox.setLayout(new BorderLayout());
        routesBox.add(new JScrollPane(routeTable), BorderLayout.CENTER);

        // right: stops and the Apply button
        JPanel stopsBox = UiUtil.section("Stops and pickup times");
        stopsBox.setLayout(new BorderLayout());
        stopTable.getColumnModel().getColumn(0).setMaxWidth(40);   // "#" column doesn't need much space
        stopsBox.add(new JScrollPane(stopTable), BorderLayout.CENTER);
        applyBtn.addActionListener(this);
        JPanel applyRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        applyRow.setOpaque(false);
        applyRow.add(applyBtn);
        stopsBox.add(applyRow, BorderLayout.SOUTH);

        JPanel centre = new JPanel(new GridLayout(1, 2, 10, 0));
        centre.setOpaque(false);
        centre.add(routesBox);
        centre.add(stopsBox);
        add(centre, BorderLayout.CENTER);

        // when the student clicks a route, show its stops and seats
        routeTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (!e.getValueIsAdjusting()) {
                    loadStops();
                    updateSeats();
                }
            }
        });

        loadRoutes();
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == checkBtn) {
            updateSeats();
        } else if (e.getSource() == applyBtn) {
            applyForSelectedStop();
        }
    }

    public void loadRoutes() {
        try {
            routes = routeService.getActiveRoutes();
            routeModel.setRowCount(0);
            for (int i = 0; i < routes.size(); i++) {
                Route r = routes.get(i);
                Vector<Object> row = new Vector<Object>();
                row.add(r.getRouteName());
                row.add(r.getBusRegNo());
                row.add(r.getCapacity());
                routeModel.addRow(row);
            }
            stopModel.setRowCount(0);
            seatsLabel.setText("Select a route to see seats left.");
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // returns null if no route is selected
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
            stops = routeService.getStops(r.getRouteId());
            for (int i = 0; i < stops.size(); i++) {
                Stop s = stops.get(i);
                Vector<Object> row = new Vector<Object>();
                row.add(s.getStopSeq());
                row.add(s.getStopName());
                row.add(s.getPickupTime());
                stopModel.addRow(row);
            }
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSeats() {
        Route r = getSelectedRoute();
        if (r == null) {
            seatsLabel.setText("Select a route to see seats left.");
            return;
        }
        try {
            int seatsLeft = routeService.getSeatsLeft(r.getRouteId(), UiUtil.getDate(fromDate), UiUtil.getDate(toDate));
            seatsLabel.setText("Seats left on " + r.getRouteName() + ": " + seatsLeft + " of " + r.getCapacity());
            if (seatsLeft > 0) {
                seatsLabel.setForeground(UiUtil.GREEN);
            } else {
                seatsLabel.setForeground(UiUtil.RED);
            }
        } catch (ServiceException ex) {
            // e.g. wrong dates - show it in the label instead of a popup
            seatsLabel.setText(ex.getMessage());
            seatsLabel.setForeground(UiUtil.RED);
        }
    }

    private void applyForSelectedStop() {
        Route r = getSelectedRoute();
        int stopRow = stopTable.getSelectedRow();
        if (r == null || stopRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a route and then a stop.");
            return;
        }
        dashboard.openApplicationFor(r, stops.get(stopRow), UiUtil.getDate(fromDate), UiUtil.getDate(toDate));
    }
}
