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
import javax.swing.JPanel;
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

/**
 * Screen: "Apply for Pass".
 * Top: form to apply (route -> stop, dates, remarks) as DRAFT or SUBMITTED.
 * Bottom: the student's applications and their status (tracking), with
 * buttons to submit or delete a DRAFT.
 */
public class ApplicationPanel extends JPanel {

    private final ApplicationService applicationService = new ApplicationService();
    private final RouteService routeService = new RouteService();
    private final StudentDashboard dashboard;

    private final JComboBox<Route> routeBox = new JComboBox<>();
    private final JComboBox<Stop> stopBox = new JComboBox<>();
    private final JSpinner fromDate = UiUtil.dateSpinner(LocalDate.now().plusDays(1));
    private final JSpinner toDate = UiUtil.dateSpinner(LocalDate.now().plusMonths(6));
    private final JTextArea remarks = new JTextArea(3, 24);
    private final JLabel seatsLabel = new JLabel(" ");

    private final DefaultTableModel appModel = UiUtil.readOnlyModel(
            "ID", "Route", "Stop", "From", "To", "Status", "Applied on", "Admin remarks");
    private final JTable appTable = UiUtil.table(appModel);
    private List<Application> myApps = new ArrayList<>();

    /** True while we fill the combo boxes in code, so their listeners do nothing. */
    private boolean loading = false;

    public ApplicationPanel(StudentDashboard dashboard) {
        this.dashboard = dashboard;
        setLayout(new BorderLayout(10, 10));
        setBackground(UiUtil.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ---------------- form ----------------
        JPanel form = UiUtil.section("New bus pass application");
        form.setLayout(new GridBagLayout());
        remarks.setLineWrap(true);
        remarks.setWrapStyleWord(true);
        int row = 0;
        UiUtil.addRow(form, row++, "Route *", routeBox);
        UiUtil.addRow(form, row++, "Stop *", stopBox);
        UiUtil.addRow(form, row++, "Valid from *", fromDate);
        UiUtil.addRow(form, row++, "Valid to *", toDate);
        UiUtil.addRow(form, row++, "Remarks (optional)", UiUtil.scroll(remarks));
        seatsLabel.setFont(seatsLabel.getFont().deriveFont(Font.BOLD));
        UiUtil.addRow(form, row++, "Seats left", seatsLabel);

        JButton draftBtn = UiUtil.secondaryButton("Save as draft");
        draftBtn.addActionListener(e -> create(false));
        JButton submitBtn = UiUtil.primaryButton("Submit application");
        submitBtn.addActionListener(e -> create(true));
        JPanel formButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        formButtons.setOpaque(false);
        formButtons.add(draftBtn);
        formButtons.add(submitBtn);
        UiUtil.addWide(form, row, formButtons);

        JPanel formHolder = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        formHolder.setOpaque(false);
        formHolder.add(form);
        add(formHolder, BorderLayout.NORTH);

        // ---------------- my applications ----------------
        JPanel list = UiUtil.section("My applications (status tracking)");
        list.setLayout(new BorderLayout());
        list.add(UiUtil.scroll(appTable), BorderLayout.CENTER);
        JButton submitDraft = UiUtil.primaryButton("Submit selected draft");
        submitDraft.addActionListener(e -> submitSelectedDraft());
        JButton deleteDraft = UiUtil.secondaryButton("Delete selected draft");
        deleteDraft.addActionListener(e -> deleteSelectedDraft());
        JButton refresh = UiUtil.secondaryButton("Refresh");
        refresh.addActionListener(e -> loadMyApplications());
        JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        listButtons.setOpaque(false);
        listButtons.add(refresh);
        listButtons.add(deleteDraft);
        listButtons.add(submitDraft);
        list.add(listButtons, BorderLayout.SOUTH);
        add(list, BorderLayout.CENTER);

        // ---------------- listeners ----------------
        routeBox.addActionListener(e -> {
            if (!loading) {
                loadStopsFor((Route) routeBox.getSelectedItem(), null);
                updateSeats();
            }
        });
        fromDate.addChangeListener(e -> updateSeats());
        toDate.addChangeListener(e -> updateSeats());

        loadRoutes();
        loadMyApplications();
    }

    /** Called by RoutesPanel's "Apply for this stop": pre-fills the form. */
    public void prefill(Route route, Stop stop, LocalDate from, LocalDate to) {
        loadRoutes();
        for (int i = 0; i < routeBox.getItemCount(); i++) {
            if (routeBox.getItemAt(i).getRouteId() == route.getRouteId()) {
                loading = true;
                routeBox.setSelectedIndex(i);
                loading = false;
                break;
            }
        }
        loadStopsFor(route, stop.getStopId());
        UiUtil.setDate(fromDate, from);
        UiUtil.setDate(toDate, to);
        remarks.setText("");
        updateSeats();
    }

    private void loadRoutes() {
        loading = true;
        try {
            routeBox.removeAllItems();
            for (Route r : routeService.getActiveRoutes()) {
                routeBox.addItem(r);
            }
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        } finally {
            loading = false;
        }
        loadStopsFor((Route) routeBox.getSelectedItem(), null);
    }

    /** Fills the stop box for a route; selects selectStopId if given. */
    private void loadStopsFor(Route route, Integer selectStopId) {
        stopBox.removeAllItems();
        if (route == null) {
            return;
        }
        try {
            for (Stop s : routeService.getStops(route.getRouteId())) {
                stopBox.addItem(s);
                if (selectStopId != null && s.getStopId() == selectStopId) {
                    stopBox.setSelectedItem(s);
                }
            }
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void updateSeats() {
        Route r = (Route) routeBox.getSelectedItem();
        if (r == null) {
            seatsLabel.setText(" ");
            return;
        }
        try {
            int left = routeService.getSeatsLeft(r.getRouteId(), UiUtil.getDate(fromDate), UiUtil.getDate(toDate));
            seatsLabel.setText(left + " of " + r.getCapacity() + (left == 0 ? "  (bus full for these dates)" : ""));
            seatsLabel.setForeground(left > 0 ? UiUtil.GREEN : UiUtil.RED);
        } catch (ServiceException ex) {
            seatsLabel.setText(ex.getMessage());
            seatsLabel.setForeground(UiUtil.RED);
        }
    }

    /** INSERT a new application as DRAFT or SUBMITTED. */
    private void create(boolean submitNow) {
        Stop stop = (Stop) stopBox.getSelectedItem();
        Integer stopId = (stop == null) ? null : stop.getStopId();
        try {
            int id = applicationService.createApplication(stopId, UiUtil.getDate(fromDate),
                    UiUtil.getDate(toDate), remarks.getText(), submitNow);
            UiUtil.info(this, submitNow
                    ? "Application #" + id + " submitted. The transport office will review it."
                    : "Draft #" + id + " saved. Submit it from the list below when ready.");
            remarks.setText("");
            loadMyApplications();
            updateSeats();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    public void loadMyApplications() {
        try {
            myApps = applicationService.getMyApplications();
            appModel.setRowCount(0);
            for (Application a : myApps) {
                appModel.addRow(new Object[] {
                    a.getApplicationId(), a.getRouteName(), a.getStopName(), a.getValidFrom(), a.getValidTo(),
                    a.getStatus(), a.getAppliedAt().toLocalDate(), UiUtil.text(a.getAdminRemarks())});
            }
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private Application selectedApp() {
        int row = appTable.getSelectedRow();
        if (row < 0) {
            UiUtil.error(this, "Please select an application in the list first.");
            return null;
        }
        return myApps.get(row);
    }

    private void submitSelectedDraft() {
        Application a = selectedApp();
        if (a == null) return;
        try {
            applicationService.submitDraft(a.getApplicationId());
            UiUtil.info(this, "Application #" + a.getApplicationId() + " submitted.");
            loadMyApplications();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }

    private void deleteSelectedDraft() {
        Application a = selectedApp();
        if (a == null) return;
        if (!"DRAFT".equals(a.getStatus())) {
            UiUtil.error(this, "Only DRAFT applications can be deleted. Submitted and decided ones are kept as a record.");
            return;
        }
        if (!UiUtil.confirm(this, "Delete draft #" + a.getApplicationId() + "?")) return;
        try {
            applicationService.deleteDraft(a.getApplicationId());
            loadMyApplications();
        } catch (ServiceException ex) {
            UiUtil.error(this, ex.getMessage());
        }
    }
}
