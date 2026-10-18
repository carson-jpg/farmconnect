package com.farmconnect.controller;

import com.farmconnect.model.PriceAlert;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PriceAlertTargetTest {
    private static PriceAlert alert(String target, String direction) {
        PriceAlert a = new PriceAlert();
        a.setTargetPrice(target == null ? null : new BigDecimal(target));
        a.setDirection(direction);
        return a;
    }

    private static BigDecimal kes(String v) { return new BigDecimal(v); }

    @Test
    void plainAlertIsAlwaysReached() {
        assertTrue(PriceController.reached(alert(null, null), kes("1")));
    }

    @Test
    void aboveTargetFiresAtOrOverTheLine() {
        PriceAlert a = alert("4500", "ABOVE");
        assertFalse(PriceController.reached(a, kes("4499.99")));
        assertTrue(PriceController.reached(a, kes("4500")));
        assertTrue(PriceController.reached(a, kes("4800")));
    }

    @Test
    void belowTargetFiresAtOrUnderTheLine() {
        PriceAlert a = alert("3000", "BELOW");
        assertFalse(PriceController.reached(a, kes("3000.01")));
        assertTrue(PriceController.reached(a, kes("3000")));
        assertTrue(PriceController.reached(a, kes("2500")));
    }

    @Test
    void missingDirectionDefaultsToAbove() {
        PriceAlert a = alert("100", null);
        assertTrue(PriceController.reached(a, kes("100")));
        assertFalse(PriceController.reached(a, kes("99")));
    }
}
