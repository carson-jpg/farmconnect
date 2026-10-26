package com.farmconnect.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PoolMathTest {
    private static BigDecimal d(String s) { return new BigDecimal(s); }

    @Test void totalAddsQuantitiesAndIgnoresNulls() {
        assertEquals(0, d("35.5").compareTo(PoolMath.total(java.util.Arrays.asList(d("10"), null, d("25.5")))));
        assertEquals(0, BigDecimal.ZERO.compareTo(PoolMath.total(null)));
    }

    @Test void progressIsWholePercentCappedAtHundred() {
        assertEquals(0, PoolMath.progressPct(d("0"), d("100")));
        assertEquals(33, PoolMath.progressPct(d("1"), d("3")));
        assertEquals(50, PoolMath.progressPct(d("50"), d("100")));
        assertEquals(100, PoolMath.progressPct(d("250"), d("100")));
        assertEquals(0, PoolMath.progressPct(d("5"), d("0")));
        assertEquals(0, PoolMath.progressPct(null, d("5")));
    }

    @Test void amountIsQuantityTimesPriceToTwoDecimals() {
        assertEquals(d("1250.00"), PoolMath.amount(d("25"), d("50")));
        assertEquals(d("33.33"), PoolMath.amount(d("1"), d("33.333")));
        assertNull(PoolMath.amount(d("5"), null));
        assertNull(PoolMath.amount(null, d("5")));
    }

    @Test void discountIsTheSavingAgainstTheNormalPrice() {
        assertEquals(10, PoolMath.discountPct(d("3000"), d("2700")));
        assertEquals(8, PoolMath.discountPct(d("100"), d("92.4")));
        assertNull(PoolMath.discountPct(d("100"), d("100")));
        assertNull(PoolMath.discountPct(d("100"), d("120")));
        assertNull(PoolMath.discountPct(null, d("90")));
        assertNull(PoolMath.discountPct(d("0"), d("0")));
    }
}
