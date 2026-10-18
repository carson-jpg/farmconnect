package com.farmconnect.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {
    @Test void rejectsShortNoDigitNoLetterAndCommon() {
        assertNotNull(PasswordPolicy.problem("abc1", null));
        assertNotNull(PasswordPolicy.problem("onlyletters", null));
        assertNotNull(PasswordPolicy.problem("1234567890", null));
        assertNotNull(PasswordPolicy.problem("ChangeMe123!", null));
    }

    @Test void acceptsAGoodPassword() {
        assertNull(PasswordPolicy.problem("Maize2026field", "a@b.com"));
    }
}
