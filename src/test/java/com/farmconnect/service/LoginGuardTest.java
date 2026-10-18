package com.farmconnect.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;

class LoginGuardTest {
    @Test void locksAfterFiveWrongPasswordsAndUnlocks() {
        LoginGuard g = new LoginGuard();
        for (int i = 0; i < 4; i++) assertFalse(g.fail("a@x.com", "1.1.1.1"));
        assertDoesNotThrow(() -> g.check("a@x.com", "1.1.1.1"));
        assertTrue(g.fail("a@x.com", "1.1.1.1"));
        assertThrows(ResponseStatusException.class, () -> g.check("A@X.com", "1.1.1.1"));
        g.unlockAccount("a@x.com");
        assertDoesNotThrow(() -> g.check("a@x.com", "1.1.1.1"));
    }

    @Test void guessingFromManyAddressesStillLocksTheAccount() {
        LoginGuard g = new LoginGuard();
        for (int i = 0; i < 20; i++) g.fail("v@x.com", "10.0.0." + i);
        assertThrows(ResponseStatusException.class, () -> g.check("v@x.com", "9.9.9.9"));
    }

    @Test void successClearsTheCounters() {
        LoginGuard g = new LoginGuard();
        for (int i = 0; i < 4; i++) g.fail("s@x.com", "3.3.3.3");
        g.success("s@x.com", "3.3.3.3");
        for (int i = 0; i < 4; i++) g.fail("s@x.com", "3.3.3.3");
        assertDoesNotThrow(() -> g.check("s@x.com", "3.3.3.3"));
    }
}
