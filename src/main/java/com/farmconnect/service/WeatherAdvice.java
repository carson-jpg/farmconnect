package com.farmconnect.service;

import com.farmconnect.service.CropCalendar.Kind;

/** Turns a short weather outlook into plain advice for one kind of farm job. Pure logic, easy to test. */
public final class WeatherAdvice {
    private WeatherAdvice() {}

    /** @param rain2 mm expected in the next 2 days, rain3 / rain7 likewise; probSoon = highest rain chance (%) in the next 2 days */
    public record Outlook(boolean known, double rain2, double rain3, double rain7, int probSoon, double windKmh, int humidity) {
        public static Outlook unknown() { return new Outlook(false, 0, 0, 0, 0, 0, 0); }
    }
    public record Note(String text, boolean goodToGo) {}

    public static Note forJob(Kind kind, boolean blightCrop, Outlook w) {
        if (!w.known()) return new Note(null, true);
        boolean rainSoon = w.probSoon() >= 60 || w.rain2() >= 5;
        switch (kind) {
            case SPRAY:
                if (blightCrop && (w.humidity() >= 80 || w.rain7() >= 30))
                    return new Note("Wet weather means a high blight risk. Spray a protective fungicide BEFORE the rain, and keep checking the leaves.", true);
                if (rainSoon) return new Note("Rain is expected soon and would wash the spray off. Wait for a dry spell if the pest is not urgent.", false);
                if (w.windKmh() >= 25) return new Note("It is windy. Spray in the calm early morning so the spray does not drift.", false);
                return new Note("Dry weather ahead: a good time to spray, early in the morning.", true);
            case FERTILIZE:
                if (w.rain3() >= 40) return new Note("Very heavy rain is expected. It would wash the fertiliser away, so wait until it passes.", false);
                if (w.rain7() < 5) return new Note("The soil may be too dry. Wait for rain, or water after applying.", false);
                if (w.rain3() >= 5 || rainSoon) return new Note("Light rain is coming: a good time to apply fertiliser, as the rain helps it soak in.", true);
                return new Note("Apply while the soil is still moist.", true);
            case HARVEST:
                if (rainSoon || w.rain3() >= 10) return new Note("Rain is expected. Harvest as soon as you can and keep it dry under cover.", true);
                return new Note("Dry weather ahead: good for harvesting and drying.", true);
            case WEED:
                if (w.rain3() >= 25) return new Note("Heavy rain is expected, so weeds will grow back fast. Weed after it passes.", false);
                return new Note("Weeding is easiest when the soil is slightly moist.", true);
            default:
                return new Note(null, true);
        }
    }

    /** One line about planting this week. */
    public static String planting(Outlook w) {
        if (!w.known()) return null;
        if (w.rain3() >= 10 || w.probSoon() >= 60) return "Rain is expected, so soil moisture should be good for planting.";
        if (w.rain7() < 5) return "A dry week is ahead. Wait for the rains, or plant only where you can irrigate.";
        return "Plant if the soil is moist enough to keep the seed damp.";
    }
}
