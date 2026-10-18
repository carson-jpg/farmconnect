package com.farmconnect.security;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

/** One rule for every password the server accepts: at least 8 characters, a letter and a digit, nothing obvious. */
public final class PasswordPolicy {
    private PasswordPolicy() {}

    private static final Set<String> COMMON = Set.of("password", "password1", "password123", "12345678", "123456789",
            "qwerty123", "11111111", "iloveyou1", "admin123", "welcome1", "changeme123", "changeme123!", "farmconnect1", "farmconnect123");

    /** @return a message saying what is wrong, or null when the password is fine. */
    public static String problem(String password, String email) {
        if (password == null || password.length() < 8) return "The password needs at least 8 characters";
        if (password.length() > 100) return "The password is too long";
        boolean letter = false, digit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) letter = true;
            if (Character.isDigit(c)) digit = true;
        }
        if (!letter || !digit) return "The password must contain at least one letter and one number";
        if (COMMON.contains(password.toLowerCase())) return "That password is too common. Choose another one";
        if (email != null && !email.isBlank() && password.equalsIgnoreCase(email.trim())) return "The password cannot be your email address";
        return null;
    }

    /** Throws 400 with the reason when the password is not acceptable. */
    public static void require(String password, String email) {
        String p = problem(password, email);
        if (p != null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, p);
    }
}
