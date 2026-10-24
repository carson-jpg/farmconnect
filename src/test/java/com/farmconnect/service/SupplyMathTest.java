package com.farmconnect.service;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class SupplyMathTest {
    @Test void fivePercentOfOneHundredThousand() {
        SupplyMath.Split s = SupplyMath.split(new BigDecimal("100000.00"), 500, 0);
        assertEquals(new BigDecimal("5000.00"), s.platform());
        assertEquals(new BigDecimal("0.00"), s.county());
        assertEquals(new BigDecimal("95000.00"), s.company());
    }

    @Test void partsAlwaysAddUpToTheTotal() {
        for (String t : new String[]{"0.01", "1.00", "333.33", "99999.99", "12345.67"}) {
            BigDecimal total = new BigDecimal(t);
            SupplyMath.Split s = SupplyMath.split(total, 500, 500);
            assertEquals(0, total.compareTo(s.platform().add(s.county()).add(s.company())), t);
            assertTrue(s.company().signum() >= 0);
        }
    }

    @Test void sharesOverOneHundredPercentAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> SupplyMath.split(BigDecimal.TEN, 6000, 5000));
    }

    @Test void wholeShillingSplitAddsUp() {
        for (long sale : new long[]{1, 17, 333, 1999, 100000, 987654}) {
            long platform = SupplyPaymentService.shillings(sale, 500);
            long county = SupplyPaymentService.shillings(sale, 500);
            assertTrue(sale - platform - county >= 0, "company share never negative for " + sale);
        }
        assertEquals(5000, SupplyPaymentService.shillings(100000, 500));
        assertEquals(17, SupplyPaymentService.shillings(333, 500));      // 16.65 rounds half up
    }
}
