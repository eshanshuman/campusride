package com.campusride.service;

import java.time.LocalDate;

/**
 * Small, reusable input checks. Each method throws a ServiceException with a
 * friendly message when the input is wrong, so the services stay short:
 *
 *     Validation.required(name, "Full name");
 */
public final class Validation {

    private Validation() { }

    /** Field must not be empty (after trimming spaces) and not longer than maxLen. */
    public static String required(String value, String fieldName, int maxLen) throws ServiceException {
        if (value == null || value.trim().isEmpty()) {
            throw new ServiceException(fieldName + " is required.");
        }
        String v = value.trim();
        if (v.length() > maxLen) {
            throw new ServiceException(fieldName + " must be at most " + maxLen + " characters.");
        }
        return v;
    }

    /** Username: 3-50 characters, letters, digits, dot or underscore. */
    public static String username(String value) throws ServiceException {
        String v = required(value, "Username", 50);
        if (!v.matches("[A-Za-z0-9._]{3,50}")) {
            throw new ServiceException("Username must be 3-50 characters: letters, digits, dot or underscore.");
        }
        return v;
    }

    /** Password: at least 8 characters with at least one letter and one digit. */
    public static void password(char[] pwd) throws ServiceException {
        if (pwd == null || pwd.length < 8) {
            throw new ServiceException("Password must be at least 8 characters long.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : pwd) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c))  hasDigit = true;
        }
        if (!hasLetter || !hasDigit) {
            throw new ServiceException("Password must contain at least one letter and one digit.");
        }
    }

    /** Phone: exactly 10 digits (same rule as the CHECK constraint in MySQL). */
    public static String phone(String value) throws ServiceException {
        String v = required(value, "Phone", 10);
        if (!v.matches("[0-9]{10}")) {
            throw new ServiceException("Phone number must be exactly 10 digits.");
        }
        return v;
    }

    /** Email: simple pattern  something@something.something */
    public static String email(String value) throws ServiceException {
        String v = required(value, "Email", 100);
        if (!v.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
            throw new ServiceException("Please enter a valid email address, e.g. name@example.com.");
        }
        return v;
    }

    /** Year of study must be 1 to 5. */
    public static void yearOfStudy(int year) throws ServiceException {
        if (year < 1 || year > 5) {
            throw new ServiceException("Year of study must be between 1 and 5.");
        }
    }

    /** Both dates given, and valid_to strictly after valid_from. */
    public static void dateRange(LocalDate from, LocalDate to) throws ServiceException {
        if (from == null || to == null) {
            throw new ServiceException("Please choose both the 'valid from' and 'valid to' dates.");
        }
        if (!to.isAfter(from)) {
            throw new ServiceException("'Valid to' must be after 'valid from'.");
        }
    }
}
