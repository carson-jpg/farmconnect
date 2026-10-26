package com.farmconnect.service;

import java.util.*;

/**
 * Plain, general farming knowledge for the crops FarmConnect farmers grow: what to do after planting and roughly when,
 * and which months are the usual planting windows in each part of the country. It is a guide only; the county
 * officer's advice for the season always wins, and the app says so.
 */
public final class CropCalendar {
    private CropCalendar() {}

    public enum Kind { SCOUT, WEED, FERTILIZE, SPRAY, HARVEST }

    /** A job that should happen between {@code fromDay} and {@code toDay} days after planting. */
    public record Task(String key, Kind kind, String title, int fromDay, int toDay, String advice) {}
    public record Crop(String name, String icon, List<Task> tasks) {}

    private static final Map<String, Crop> CROPS = new LinkedHashMap<>();
    static {
        add("Maize", "🌽", List.of(
                new Task("weed1", Kind.WEED, "First weeding", 14, 21, "Weed carefully so you do not cut the young roots."),
                new Task("scout", Kind.SCOUT, "Check for fall armyworm", 14, 35, "Look in the funnel of each plant for chewed leaves and droppings. Spray early if you find them."),
                new Task("topdress", Kind.FERTILIZE, "Top-dress with CAN or urea", 30, 42, "Put the fertiliser a hand-width from the plant and cover it, when the soil is moist."),
                new Task("spray", Kind.SPRAY, "Spray for stalk borer or armyworm if present", 28, 42, "Spray in the early morning or late afternoon, never in strong sun or wind."),
                new Task("weed2", Kind.WEED, "Second weeding", 42, 56, "Keep the field clean until the maize shades the ground."),
                new Task("harvest", Kind.HARVEST, "Harvest", 120, 180, "Harvest when the husks are dry. Dry the grain well (about 13% moisture) before storing.")));
        add("Beans", "🫘", List.of(
                new Task("weed1", Kind.WEED, "First weeding", 14, 21, "Weed by hand; beans have shallow roots."),
                new Task("spray", Kind.SPRAY, "Spray for aphids, bean fly or rust if present", 21, 35, "Check the underside of leaves first."),
                new Task("weed2", Kind.WEED, "Second weeding", 28, 35, "Do it before flowering."),
                new Task("harvest", Kind.HARVEST, "Harvest", 80, 100, "Harvest when pods are dry and rattle. Dry under cover.")));
        add("Irish Potato", "🥔", List.of(
                new Task("weedup", Kind.WEED, "Weed and earth up", 21, 30, "Pull soil up around the stems so tubers stay covered."),
                new Task("topdress", Kind.FERTILIZE, "Top-dress", 30, 35, "Use a potato top-dressing when the soil is moist."),
                new Task("blight", Kind.SPRAY, "Spray against late blight", 28, 42, "Blight spreads fast in cool wet weather. Spray before rain, and again if the weather stays wet."),
                new Task("harvest", Kind.HARVEST, "Harvest", 90, 120, "Harvest when the vines yellow and die back. Do not leave tubers in wet soil.")));
        add("Tomato", "🍅", List.of(
                new Task("weed", Kind.WEED, "Weed and stake", 20, 30, "Stake plants so fruit stays off the soil."),
                new Task("spray", Kind.SPRAY, "Spray against blight and whitefly", 21, 35, "Spray early morning; avoid wetting the fruit in the heat of the day."),
                new Task("topdress", Kind.FERTILIZE, "Top-dress", 30, 45, "Water after applying."),
                new Task("harvest", Kind.HARVEST, "Start harvesting", 75, 100, "Pick when fruit turns pink to red; keep it in the shade.")));
        add("Kale", "🥬", List.of(
                new Task("topdress", Kind.FERTILIZE, "Top-dress", 21, 28, "A small handful of CAN per plant, away from the leaves."),
                new Task("spray", Kind.SPRAY, "Spray against aphids", 21, 35, "Check the underside of leaves; wash off light attacks."),
                new Task("harvest", Kind.HARVEST, "Start harvesting leaves", 45, 75, "Pick lower leaves first and leave the top to keep growing.")));
        add("Cabbage", "🥬", List.of(
                new Task("topdress", Kind.FERTILIZE, "Top-dress", 21, 28, "Apply when the soil is moist."),
                new Task("spray", Kind.SPRAY, "Spray against diamondback moth", 21, 42, "Scout weekly and spray only when you see damage."),
                new Task("harvest", Kind.HARVEST, "Harvest heads", 70, 100, "Cut when heads are firm.")));
        add("Wheat", "🌾", List.of(
                new Task("weed", Kind.WEED, "Weed control", 21, 35, "Use the herbicide your agro-dealer recommends for the stage."),
                new Task("topdress", Kind.FERTILIZE, "Top-dress", 28, 42, "Apply when the soil is moist."),
                new Task("rust", Kind.SPRAY, "Spray against rust", 50, 80, "Check leaves for orange-brown spots, especially in cool wet weather."),
                new Task("harvest", Kind.HARVEST, "Harvest", 120, 150, "Harvest when the grain is hard and the straw is golden and dry.")));
        add("Sorghum", "🌾", List.of(
                new Task("weed1", Kind.WEED, "Weeding", 14, 28, "Weed early; sorghum is slow at first."),
                new Task("topdress", Kind.FERTILIZE, "Top-dress", 28, 35, "Apply when the soil is moist."),
                new Task("harvest", Kind.HARVEST, "Harvest", 100, 130, "Harvest when the grain is hard. Protect heads from birds.")));
    }

    private static void add(String name, String icon, List<Task> tasks) { CROPS.put(name, new Crop(name, icon, tasks)); }

    /** Finds the crop a farmer meant by "maize", "Maize (hybrid)", "sukuma wiki", "irish potatoes"... or null when we have no guide for it. */
    public static Crop find(String title) {
        if (title == null) return null;
        String t = title.toLowerCase(Locale.ROOT);
        if (t.contains("maize") || t.contains("corn")) return CROPS.get("Maize");
        if (t.contains("bean")) return CROPS.get("Beans");
        if (t.contains("potato") && !t.contains("sweet")) return CROPS.get("Irish Potato");
        if (t.contains("tomato")) return CROPS.get("Tomato");
        if (t.contains("kale") || t.contains("sukuma")) return CROPS.get("Kale");
        if (t.contains("cabbage")) return CROPS.get("Cabbage");
        if (t.contains("wheat")) return CROPS.get("Wheat");
        if (t.contains("sorghum") || t.contains("millet")) return CROPS.get("Sorghum");
        return null;
    }

    public static Collection<Crop> all() { return CROPS.values(); }

    // ------------------------------------------------------------------ planting windows

    public enum Region { RIFT_HIGHLANDS, WESTERN, EASTERN_CENTRAL }

    public static Region regionOf(String county) {
        if (county == null) return Region.RIFT_HIGHLANDS;
        switch (county) {
            case "Bungoma": case "Kakamega": case "Kisumu": case "Homa Bay": case "Migori": return Region.WESTERN;
            case "Meru": case "Machakos": case "Kitui": case "Nyeri": return Region.EASTERN_CENTRAL;
            default: return Region.RIFT_HIGHLANDS;     // Trans Nzoia, Uasin Gishu, West Pokot, Nakuru
        }
    }

    /** Months (1-12) when each crop is usually planted in a region. */
    private static final Map<Region, Map<String, int[]>> WINDOWS = new EnumMap<>(Region.class);
    static {
        WINDOWS.put(Region.RIFT_HIGHLANDS, Map.of(
                "Maize", new int[]{3, 4, 5}, "Beans", new int[]{3, 4, 8, 9}, "Irish Potato", new int[]{3, 4, 8, 9},
                "Wheat", new int[]{3, 4}, "Kale", new int[]{2, 3, 4, 9, 10, 11}, "Cabbage", new int[]{2, 3, 9, 10},
                "Tomato", new int[]{9, 10, 11}, "Sorghum", new int[]{3, 4}));
        WINDOWS.put(Region.WESTERN, Map.of(
                "Maize", new int[]{3, 4, 8, 9}, "Beans", new int[]{3, 4, 8, 9}, "Sorghum", new int[]{3, 4, 8, 9},
                "Irish Potato", new int[]{3, 4}, "Tomato", new int[]{9, 10, 11}, "Kale", new int[]{3, 4, 5, 9, 10, 11},
                "Cabbage", new int[]{3, 4, 9, 10}));
        WINDOWS.put(Region.EASTERN_CENTRAL, Map.of(
                "Maize", new int[]{3, 4, 10, 11}, "Beans", new int[]{3, 4, 10, 11}, "Sorghum", new int[]{10, 11},
                "Irish Potato", new int[]{3, 4, 10, 11}, "Tomato", new int[]{10, 11, 12}, "Kale", new int[]{3, 4, 10, 11},
                "Cabbage", new int[]{3, 4, 10, 11}));
    }

    /** Crops that are usually planted around this month (month = 1-12) in the region. */
    public static List<String> plantNow(Region region, int month) {
        List<String> out = new ArrayList<>();
        Map<String, int[]> w = WINDOWS.get(region);
        for (String crop : CROPS.keySet()) {
            int[] months = w.get(crop);
            if (months == null) continue;
            for (int m : months) if (m == month) { out.add(crop); break; }
        }
        return out;
    }
}
