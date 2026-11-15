package com.campusride.service;

import java.time.LocalDate;

// Input checks we use in many places.
// If something is wrong they throw a ServiceException with a message for the user.
public class Validation {

    // not empty and not too long; returns the value without extra spaces
    public static String required(String value, String fieldName, int maxLength) throws ServiceException {
        if (value == null || value.trim().isEmpty()) {
            throw new ServiceException(fieldName + " is required.");
        }
        String trimmed = value.trim();
        if (trimmed.length() > maxLength) {
            throw new ServiceException(fieldName + " must be at most " + maxLength + " characters.");
        }
        return trimmed;
    }

    // 3 to 50 characters: letters, digits, dot or underscore
    public static String username(String value) throws ServiceException {
        String name = required(value, "Username", 50);
        if (!name.matches("[A-Za-z0-9._]{3,50}")) {
            throw new ServiceException("Username must be 3-50 characters: letters, digits, dot or underscore.");
        }
        return name;
    }

    // at least 8 characters with at least one letter and one digit
    public static void password(char[] password) throws ServiceException {
        if (password == null || password.length < 8) {
            throw new ServiceException("Password must be at least 8 characters long.");
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (int i = 0; i < password.length; i++) {
            if (Character.isLetter(password[i])) {
                hasLetter = true;
            }
            if (Character.isDigit(password[i])) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            throw new ServiceException("Password must contain at least one letter and one digit.");
        }
    }

    // exactly 10 digits (same rule as the CHECK in MySQL)
    public static String phone(String value) throws ServiceException {
        String phone = required(value, "Phone", 10);
        if (!phone.matches("[0-9]{10}")) {
            throw new ServiceException("Phone number must be exactly 10 digits.");
        }
        return phone;
    }

    // simple pattern: something@something.something
    public static String email(String value) throws ServiceException {
        String email = required(value, "Email", 100);
        if (!email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}")) {
            throw new ServiceException("Please enter a valid email address, e.g. name@example.com.");
        }
        return email;
    }

    public static void yearOfStudy(int year) throws ServiceException {
        if (year < 1 || year > 5) {
            throw new ServiceException("Year of study must be between 1 and 5.");
        }
    }

    // both dates chosen and valid_to after valid_from
    public static void dateRange(LocalDate from, LocalDate to) throws ServiceException {
        if (from == null || to == null) {
            throw new ServiceException("Please choose both the 'valid from' and 'valid to' dates.");
        }
        if (!to.isAfter(from)) {
            throw new ServiceException("'Valid to' must be after 'valid from'.");
        }
    }
}
