package com.campusride.service;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Vector;

import com.campusride.dao.ApplicationDAO;
import com.campusride.dao.PassDAO;
import com.campusride.dao.RouteDAO;
import com.campusride.dao.StopDAO;
import com.campusride.model.Application;
import com.campusride.model.BusPass;
import com.campusride.model.Route;
import com.campusride.model.Stop;
import com.campusride.util.DBConnection;

// Student side of applications: apply, submit a draft, delete a draft, see my applications and passes.
// Approve/reject is done by the admin, in AdminService.
public class ApplicationService {

    private ApplicationDAO appDAO = new ApplicationDAO();
    private PassDAO passDAO = new PassDAO();
    private StopDAO stopDAO = new StopDAO();
    private RouteDAO routeDAO = new RouteDAO();

    // submitNow = true  -> SUBMITTED (goes to the admin)
    // submitNow = false -> DRAFT (student can submit or delete it later)
    public int createApplication(Integer stopId, LocalDate from, LocalDate to, String remarks, boolean submitNow)
            throws ServiceException {
        int studentId = StudentService.currentStudentId();
        if (stopId == null) {
            throw new ServiceException("Please select a stop.");
        }
        checkDates(from, to);

        String note = null;
        if (remarks != null && !remarks.trim().isEmpty()) {
            note = remarks.trim();
        }
        if (note != null && note.length() > 255) {
            throw new ServiceException("Remarks must be at most 255 characters.");
        }

        Connection con = null;
        try {
            con = DBConnection.getConnection();
            if (submitNow) {
                checkCanSubmit(con, studentId, stopId, from, to, 0);
            } else if (stopDAO.findById(con, stopId) == null) {
                throw new ServiceException("The selected stop no longer exists.");
            }

            Application a = new Application();
            a.setStudentId(studentId);
            a.setStopId(stopId);
            a.setValidFrom(from);
            a.setValidTo(to);
            if (submitNow) {
                a.setStatus("SUBMITTED");
            } else {
                a.setStatus("DRAFT");
            }
            a.setRemarks(note);
            return appDAO.insert(con, a);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        } finally {
            DBConnection.close(con);
        }
    }

    // DRAFT -> SUBMITTED, with the same checks as a new application
    public void submitDraft(int applicationId) throws ServiceException {
        int studentId = StudentService.currentStudentId();
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            Application a = appDAO.findById(con, applicationId);
            if (a == null || a.getStudentId() != studentId) {
                throw new ServiceException("Application not found.");
            }
            if (!a.getStatus().equals("DRAFT")) {
                throw new ServiceException("Only DRAFT applications can be submitted.");
            }
            checkDates(a.getValidFrom(), a.getValidTo());
            checkCanSubmit(con, studentId, a.getStopId(), a.getValidFrom(), a.getValidTo(), applicationId);
            int rows = appDAO.submitDraft(con, applicationId, studentId);
            if (rows != 1) {
                throw new ServiceException("This draft could not be submitted. Please refresh.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        } finally {
            DBConnection.close(con);
        }
    }

    // only DRAFTs can be deleted, submitted/decided ones are kept as a record
    public void deleteDraft(int applicationId) throws ServiceException {
        int studentId = StudentService.currentStudentId();
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            int rows = appDAO.deleteDraft(con, applicationId, studentId);
            if (rows != 1) {
                throw new ServiceException("Only your own DRAFT applications can be deleted.");
            }
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        } finally {
            DBConnection.close(con);
        }
    }

    public Vector<Application> getMyApplications() throws ServiceException {
        int studentId = StudentService.currentStudentId();
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            return appDAO.findByStudent(con, studentId);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        } finally {
            DBConnection.close(con);
        }
    }

    public Vector<BusPass> getMyPasses() throws ServiceException {
        int studentId = StudentService.currentStudentId();
        Connection con = null;
        try {
            con = DBConnection.getConnection();
            passDAO.expireOldPasses(con);   // so old passes show as EXPIRED
            return passDAO.findByStudent(con, studentId);
        } catch (SQLException e) {
            throw ServiceException.fromSql(e);
        } finally {
            DBConnection.close(con);
        }
    }

    // valid_to after valid_from, start not in the past, at most one year
    private void checkDates(LocalDate from, LocalDate to) throws ServiceException {
        Validation.dateRange(from, to);
        if (from.isBefore(LocalDate.now())) {
            throw new ServiceException("'Valid from' cannot be in the past.");
        }
        if (to.isAfter(from.plusDays(366))) {
            throw new ServiceException("A pass can be requested for at most one year.");
        }
    }

    // Checks before an application goes to the admin, so the student gets a message early.
    // The admin's approve() checks capacity and overlap AGAIN, because things can change in between.
    private void checkCanSubmit(Connection con, int studentId, int stopId, LocalDate from, LocalDate to,
                                int excludeApplicationId) throws SQLException, ServiceException {
        Stop stop = stopDAO.findById(con, stopId);
        if (stop == null) {
            throw new ServiceException("The selected stop no longer exists.");
        }
        Route route = routeDAO.findById(con, stop.getRouteId());
        if (route == null || !route.isActive()) {
            throw new ServiceException("This route is not running at the moment. Please choose another route.");
        }
        if (passDAO.countActiveOverlapForStudent(con, studentId, from, to) > 0) {
            throw new ServiceException("You already have an ACTIVE pass for (part of) these dates.");
        }
        if (appDAO.countSubmittedOverlap(con, studentId, from, to, excludeApplicationId) > 0) {
            throw new ServiceException("You already have a submitted application for these dates. "
                    + "Please wait for the admin's decision.");
        }
        int seatsUsed = passDAO.countActiveOverlapOnRoute(con, route.getRouteId(), from, to);
        if (seatsUsed >= route.getCapacity()) {
            throw new ServiceException("Sorry, " + route.getRouteName() + " is full for these dates. "
                    + "Please choose another route or period.");
        }
    }
}
