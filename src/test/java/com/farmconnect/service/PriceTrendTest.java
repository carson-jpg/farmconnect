package com.farmconnect.service;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PriceTrendTest {
    private static final LocalDate D0 = LocalDate.of(2026, 1, 1);

    private static List<PriceTrend.Point> line(int n, int stepDays, double start, double perStep) {
        List<PriceTrend.Point> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(new PriceTrend.Point(D0.plusDays((long) i * stepDays), start + i * perStep));
        return l;
    }

    @Test void risingPricesAreRisingWithAForecastAboveTheLastPrice() {
        PriceTrend.Result r = PriceTrend.compute("Maize", "90kg bag", line(12, 3, 3000, 50));
        assertEquals("RISING", r.direction());
        assertEquals("HIGH", r.confidence());
        assertEquals(2, r.forecast().size());
        assertTrue(r.forecast().get(0).price() > 3550);
    }

    @Test void fallingAndSteadyAreDetected() {
        assertEquals("FALLING", PriceTrend.compute("Maize", "bag", line(8, 4, 4000, -100)).direction());
        assertEquals("STABLE", PriceTrend.compute("Milk", "litre", line(8, 1, 100, 0)).direction());
    }

    @Test void fewerThanThreeDaysIsNotEnoughData() {
        PriceTrend.Result r = PriceTrend.compute("Beans", "kg", line(2, 1, 100, 5));
        assertEquals("NOT_ENOUGH_DATA", r.direction());
        assertTrue(r.forecast().isEmpty());
    }

    @Test void severalPricesOnOneDayAreAveraged() {
        List<PriceTrend.Point> l = new ArrayList<>();
        l.add(new PriceTrend.Point(D0, 100)); l.add(new PriceTrend.Point(D0, 200));
        PriceTrend.Result r = PriceTrend.compute("X", "kg", l);
        assertEquals(1, r.history().size());
        assertEquals(150.0, r.history().get(0).price(), 0.001);
    }
}
