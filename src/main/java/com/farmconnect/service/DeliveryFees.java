package com.farmconnect.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Distance and fee for a delivery: base fee + rate per km x distance, rounded up to a whole shilling. */
public final class DeliveryFees {
    private DeliveryFees() {}

    private static final double EARTH_KM = 6371.0088;

    /** Roughly Kenya. Anything outside is treated as "no location" rather than quoting a silly distance. */
    public static boolean validKenyaPoint(Double lat, Double lng) {
        return lat != null && lng != null && lat >= -5.0 && lat <= 5.6 && lng >= 33.5 && lng <= 42.0;
    }

    /** Straight-line distance times a road factor (roads are longer than a straight line), to 0.1 km. */
    public static double roadKm(double lat1, double lng1, double lat2, double lng2, double roadFactor) {
        double p1 = Math.toRadians(lat1), p2 = Math.toRadians(lat2);
        double dp = p2 - p1, dl = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dp / 2) * Math.sin(dp / 2) + Math.cos(p1) * Math.cos(p2) * Math.sin(dl / 2) * Math.sin(dl / 2);
        double straight = 2 * EARTH_KM * Math.asin(Math.min(1, Math.sqrt(a)));
        return Math.round(straight * roadFactor * 10.0) / 10.0;
    }

    /** base + perKm x km, rounded up to a whole shilling; null when the distance is not known. */
    public static BigDecimal fee(BigDecimal base, BigDecimal perKm, Double km) {
        if (km == null || base == null || perKm == null || km < 0) return null;
        return base.add(perKm.multiply(BigDecimal.valueOf(km))).setScale(0, RoundingMode.CEILING);
    }
}
