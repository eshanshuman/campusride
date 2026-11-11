package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.service.RouteService;
import com.campusride.service.ServiceException;

/**
 * Screen: "Routes & Timings".
 * Left: active routes with bus and capacity. Right: stops and pickup times of
 * the selected route. Top: a period, and the seats left on the route for it.
 * "Apply for this stop" opens ApplicationPanel already filled in.
 */
public class RoutesPanel extends JPanel {

    private final RouteService routeService = new RouteService();
    private final StudentDashboard dashboard;

    private final JSpinner fromDate = UiUtil.dateSpinner(LocalDate.now().plusDays(1));
    private final JSpinner toDate = UiUtil.dateSpinner(LocalDate.now().plusMonths(6));
    private final JLabel seatsLabel = new JLabel("Select a route to see seats left.");

    private final DefaultTableModel routeModel = UiUtil.readOnlyModel("Route", "Bus", "Capacity");
    private final JTable routeTable = UiUtil.table(routeModel);
    private final DefaultTableModel stopModel = UiUtil.readOnlyModel("#", "Stop", "Pickup time");
    private final JTable stopTable = UiUtil.table(stopModel);

    // The objects behind each table row (same order as the rows)
    private List<Route> routes = new ArrayList<>();
    private List<Stop> stops = new ArrayList<>();

    public RoutesPanel(StudentDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout(10, 10));
        setBackground(UiUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---- top: period + seats left ----
        JPanel top = UiUtil.section("Travel period");
        top.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 2));
        top.add(new JLabel("From"));
        top.add(fromDate);
        top.add(new JLabel("To"));
        top.add(toDate);
        JButton check = UiUtil.secondaryButton("Check seats");
        check.addActionListener(e -> updateSeats());
        top.add(check);
        seatsLabel.setFont(seatsLabel.getFont().deriveFont(Font.BOLD));
        top.add(seatsLabel);
        add(top, BorderLayout.NORTH);

        // ---- centre: routes | stops ----
        JPanel routesBox = UiUtil.section("Bus routes");
        routesBox.setLayout(new BorderLayout());
        routesBox.add(UiUtil.scroll(routeTable), BorderLayout.CENTER);

        JPanel stopsBox = UiUtil.section("Stops and pickup times");
        stopsBox.setLayout(new BorderLayout());
        stopsBox.add(UiUtil.scroll(stopTable), BorderLayout.CENTER);
        JButton apply = UiUtil.primaryButton("Apply for this stop");
        apply.addActionListener(e -> applyForSelectedStop());
        JPanel applyRow = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        applyRow.setOpaque(false);
        applyRow.add(apply);
        stopsBox.add(applyRow, BorderLayout.SOUTH);

        JPanel centre = new JPanel(new GridLayout(1, 2, 10, 0));
        centre.setOpaque(false);
        centre.add(routesBox);
        centre.add(stopsBox);
        add(centre, BorderLayout.CENTER);

        // When a route is clicked, load its stops and seats
        routeTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                loadStops();
                updateSeats();
            }
        });

        loadRoutes();
    }

    /** (Re)loads the list of active routes. */
    public void loadRoutes() {
        try {
            routes = routeService.getActiveRoutes();
            routeModel.setRowCount(0);
            for (Route r : routes) {
                routeModel.addRow(new Object[] {r.getRouteName(), r.getBusRegNo(), r.getCapacity()});
            }
            stopModel.setRowCount(0);
            seatsLabel.setText("Select a route to see seats left.");
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
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
            stops = routeService.getStops(r.getRouteId());
            for (Stop s : stops) {
                stopModel.addRow(new Object[] {s.getStopSeq(), s.getStopName(), s.getPickupTime()});
            }
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void updateSeats() {
        Route r = selectedRoute();
        if (r == null) {
            seatsLabel.setText("Select a route to see seats left.");
            return;
        }
        try {
            int left = routeService.getSeatsLeft(r.getRouteId(), UiUtil.getDate(fromDate), UiUtil.getDate(toDate));
            seatsLabel.setText("Seats left on " + r.getRouteName() + ": " + left + " of " + r.getCapacity());
            seatsLabel.setForeground(left > 0 ? UiUtil.GREEN : UiUtil.RED);
        } catch (ServiceException ex) {
            seatsLabel.setText(ex.getMessage());
            seatsLabel.setForeground(UiUtil.RED);
        }
    }

    private void applyForSelectedStop() {
        Route r = selectedRoute();
        int row = stopTable.getSelectedRow();
        if (r == null || row < 0) {
            UiUtil.error(this, "Please select a route and then a stop.");
            return;
        }
        dashboard.openApplicationFor(r, stops.get(row), UiUtil.getDate(fromDate), UiUtil.getDate(toDate));
    }
}
