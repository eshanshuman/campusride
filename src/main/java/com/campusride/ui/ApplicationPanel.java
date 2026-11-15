package com.campusride.ui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import com.campusride.model.Application;
import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.service.ApplicationService;
import com.campusride.service.RouteService;
import com.campusride.service.ServiceException;

// "Apply for Pass" tab.
// Top: form to apply for a stop and dates (save as DRAFT or SUBMIT).
// Bottom: the student's applications with their status, and buttons for drafts.
public class ApplicationPanel extends JPanel {

    private ApplicationService applicationService = new ApplicationService();
    private RouteService routeService = new RouteService();

    private JComboBox<Route> routeBox = new JComboBox<>();
    private JComboBox<Stop> stopBox = new JComboBox<>();
    private JSpinner fromDate = UiUtil.dateSpinner(LocalDate.now().plusDays(1));
    private JSpinner toDate = UiUtil.dateSpinner(LocalDate.now().plusMonths(6));
    private JTextArea remarks = new JTextArea(3, 24);
    private JLabel seatsLabel = new JLabel(" ");

    private DefaultTableModel appModel = UiUtil.readOnlyModel(
            "ID", "Route", "Stop", "From", "To", "Status", "Applied on", "Admin remarks");
    private JTable appTable = UiUtil.table(appModel);
    private List<Application> myApps = new ArrayList<>();

    // When we fill the route combo box from code, its listener would fire for every item.
    // While this is true the listener does nothing.
    private boolean loading = false;

    public ApplicationPanel() {
        setLayout(new BorderLayout(10, 10));
        setBackground(UiUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ----- the form -----
        JPanel form = UiUtil.section("New bus pass application");
        form.setLayout(new GridBagLayout());
        remarks.setLineWrap(true);
        remarks.setWrapStyleWord(true);
        seatsLabel.setFont(seatsLabel.getFont().deriveFont(Font.BOLD));
        UiUtil.addRow(form, 0, "Route *", routeBox);
        UiUtil.addRow(form, 1, "Stop *", stopBox);
        UiUtil.addRow(form, 2, "Valid from *", fromDate);
        UiUtil.addRow(form, 3, "Valid to *", toDate);
        UiUtil.addRow(form, 4, "Remarks (optional)", new JScrollPane(remarks));
        UiUtil.addRow(form, 5, "Seats left", seatsLabel);

        JButton draftBtn = UiUtil.secondaryButton("Save as draft");
        draftBtn.addActionListener(e -> createApplication(false));
        JButton submitBtn = UiUtil.primaryButton("Submit application");
        submitBtn.addActionListener(e -> createApplication(true));
        JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        formButtons.setOpaque(false);
        formButtons.add(draftBtn);
        formButtons.add(submitBtn);
        UiUtil.addWide(form, 6, formButtons);

        JPanel formHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        formHolder.setOpaque(false);
        formHolder.add(form);
        add(formHolder, BorderLayout.NORTH);

        // ----- my applications -----
        JPanel list = UiUtil.section("My applications (status tracking)");
        list.setLayout(new BorderLayout());
        appTable.getColumnModel().getColumn(0).setMaxWidth(45);   // ID column
        list.add(new JScrollPane(appTable), BorderLayout.CENTER);

        JButton refresh = UiUtil.secondaryButton("Refresh");
        refresh.addActionListener(e -> loadMyApplications());
        JButton deleteDraft = UiUtil.secondaryButton("Delete selected draft");
        deleteDraft.addActionListener(e -> deleteSelectedDraft());
        JButton submitDraft = UiUtil.primaryButton("Submit selected draft");
        submitDraft.addActionListener(e -> submitSelectedDraft());
        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        listButtons.setOpaque(false);
        listButtons.add(refresh);
        listButtons.add(deleteDraft);
        listButtons.add(submitDraft);
        list.add(listButtons, BorderLayout.SOUTH);
        add(list, BorderLayout.CENTER);

        // when the route changes, load its stops; when anything changes, update seats left
        routeBox.addActionListener(e -> {
            if (!loading) {
                loadStops((Route) routeBox.getSelectedItem(), 0);
                updateSeats();
            }
        });
        fromDate.addChangeListener(e -> updateSeats());
        toDate.addChangeListener(e -> updateSeats());

        loadRoutes();
        updateSeats();
        loadMyApplications();
    }

    // Called from RoutesPanel ("Apply for this stop"): fill in the form for the student.
    public void prefill(Route route, Stop stop, LocalDate from, LocalDate to) {
        loadRoutes();
        loading = true;
        for (int i = 0; i < routeBox.getItemCount(); i++) {
            if (routeBox.getItemAt(i).getRouteId() == route.getRouteId()) {
                routeBox.setSelectedIndex(i);
            }
        }
        loading = false;
        loadStops(route, stop.getStopId());
        UiUtil.setDate(fromDate, from);
        UiUtil.setDate(toDate, to);
        remarks.setText("");
        updateSeats();
    }

    private void loadRoutes() {
        loading = true;
        routeBox.removeAllItems();
        try {
            List<Route> routes = routeService.getActiveRoutes();
            for (Route r : routes) {
                routeBox.addItem(r);
            }
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        loading = false;
        loadStops((Route) routeBox.getSelectedItem(), 0);
    }

    // fills the stop combo box for a route; selects stopIdToSelect if it is not 0
    private void loadStops(Route route, int stopIdToSelect) {
        stopBox.removeAllItems();
        if (route == null) {
            return;
        }
        try {
            List<Stop> stops = routeService.getStops(route.getRouteId());
            for (Stop s : stops) {
                stopBox.addItem(s);
                if (s.getStopId() == stopIdToSelect) {
                    stopBox.setSelectedItem(s);
                }
            }
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSeats() {
        Route r = (Route) routeBox.getSelectedItem();
        if (r == null) {
            seatsLabel.setText(" ");
            return;
        }
        try {
            int seatsLeft = routeService.getSeatsLeft(r.getRouteId(), UiUtil.getDate(fromDate), UiUtil.getDate(toDate));
            if (seatsLeft > 0) {
                seatsLabel.setText(seatsLeft + " of " + r.getCapacity());
                seatsLabel.setForeground(UiUtil.GREEN);
            } else {
                seatsLabel.setText(seatsLeft + " of " + r.getCapacity() + "  (bus full for these dates)");
                seatsLabel.setForeground(UiUtil.RED);
            }
        } catch (ServiceException ex) {
            seatsLabel.setText(ex.getMessage());
            seatsLabel.setForeground(UiUtil.RED);
        }
    }

    // submitNow = true for "Submit application", false for "Save as draft"
    private void createApplication(boolean submitNow) {
        Stop stop = (Stop) stopBox.getSelectedItem();
        Integer stopId = null;
        if (stop != null) {
            stopId = stop.getStopId();
        }
        try {
            int id = applicationService.createApplication(stopId, UiUtil.getDate(fromDate),
                    UiUtil.getDate(toDate), remarks.getText(), submitNow);
            if (submitNow) {
                JOptionPane.showMessageDialog(this, "Application #" + id + " submitted. The transport office will review it.");
            } else {
                JOptionPane.showMessageDialog(this, "Draft #" + id + " saved. Submit it from the list below when ready.");
            }
            remarks.setText("");
            loadMyApplications();
            updateSeats();
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void loadMyApplications() {
        try {
            myApps = applicationService.getMyApplications();
            appModel.setRowCount(0);
            for (Application a : myApps) {
                appModel.addRow(new Object[] {
                    a.getApplicationId(), a.getRouteName(), a.getStopName(), a.getValidFrom(), a.getValidTo(),
                    a.getStatus(), a.getAppliedAt().toLocalDate(), a.getAdminRemarks()});
            }
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void submitSelectedDraft() {
        int row = appTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an application in the list first.");
            return;
        }
        Application a = myApps.get(row);
        try {
            applicationService.submitDraft(a.getApplicationId());
            JOptionPane.showMessageDialog(this, "Application #" + a.getApplicationId() + " submitted.");
            loadMyApplications();
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void deleteSelectedDraft() {
        int row = appTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an application in the list first.");
            return;
        }
        Application a = myApps.get(row);
        if (!a.getStatus().equals("DRAFT")) {
            JOptionPane.showMessageDialog(this,
                    "Only DRAFT applications can be deleted. Submitted and decided ones are kept as a record.");
            return;
        }
        int answer = JOptionPane.showConfirmDialog(this, "Delete draft #" + a.getApplicationId() + "?",
                "Confirm", JOptionPane.YES_NO_OPTION);
        if (answer != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            applicationService.deleteDraft(a.getApplicationId());
            loadMyApplications();
        } catch (ServiceException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
