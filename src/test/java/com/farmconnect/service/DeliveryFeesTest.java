package com.farmconnect.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class DeliveryFeesTest {
    @Test void kenyaPointsOnly() {
        assertTrue(DeliveryFees.validKenyaPoint(1.0167, 35.0));      // Kitale area
        assertTrue(DeliveryFees.validKenyaPoint(-1.2921, 36.8219));  // Nairobi
        assertFalse(DeliveryFees.validKenyaPoint(0.0, 0.0));
        assertFalse(DeliveryFees.validKenyaPoint(51.5, -0.12));
        assertFalse(DeliveryFees.validKenyaPoint(null, 36.0));
        assertFalse(DeliveryFees.validKenyaPoint(1.0, null));
    }

    @Test void sameSpotIsZeroKm() {
        assertEquals(0.0, DeliveryFees.roadKm(1.0, 35.0, 1.0, 35.0, 1.3));
    }

    @Test void kitaleToEldoretIsAboutOneHundredKmByRoad() {
        double km = DeliveryFees.roadKm(1.0191, 35.0020, 0.5143, 35.2698, 1.3);   // Kitale -> Eldoret
        assertTrue(km > 70 && km < 110, "got " + km);
    }

    @Test void roadFactorStretchesTheDistance() {
        double straight = DeliveryFees.roadKm(1.0191, 35.0020, 0.5143, 35.2698, 1.0);
        double road = DeliveryFees.roadKm(1.0191, 35.0020, 0.5143, 35.2698, 1.5);
        assertEquals(straight * 1.5, road, 0.2);
    }

    @Test void distanceIsSymmetric() {
        assertEquals(DeliveryFees.roadKm(1.0, 35.0, -1.3, 36.8, 1.3), DeliveryFees.roadKm(-1.3, 36.8, 1.0, 35.0, 1.3));
    }

    @Test void feeIsBasePlusRateTimesKmRoundedUp() {
        assertEquals(new BigDecimal("1350"), DeliveryFees.fee(new BigDecimal("150"), new BigDecimal("30"), 40.0));
        assertEquals(new BigDecimal("1356"), DeliveryFees.fee(new BigDecimal("150"), new BigDecimal("30"), 40.2));  // 1356.0
        assertEquals(new BigDecimal("1357"), DeliveryFees.fee(new BigDecimal("150"), new BigDecimal("30"), 40.21)); // 1356.3 -> 1357
        assertEquals(new BigDecimal("150"), DeliveryFees.fee(new BigDecimal("150"), new BigDecimal("30"), 0.0));
    }

    @Test void noDistanceNoFee() {
        assertNull(DeliveryFees.fee(new BigDecimal("150"), new BigDecimal("30"), null));
        assertNull(DeliveryFees.fee(null, new BigDecimal("30"), 5.0));
        assertNull(DeliveryFees.fee(new BigDecimal("150"), new BigDecimal("30"), -1.0));
    }
}
