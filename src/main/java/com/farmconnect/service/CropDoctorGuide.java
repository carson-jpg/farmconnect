package com.farmconnect.service;

import java.util.*;

/**
 * The built-in crop problem guide: common Kenyan pests, diseases and nutrient or water problems, matched from the
 * symptoms a farmer ticks. This is general guidance, not a laboratory diagnosis; a county officer reviews every case
 * the farmer sends in. No chemical product names or doses are given (the label and the officer decide those).
 */
public final class CropDoctorGuide {
    private CropDoctorGuide() {}

    public record Symptom(String key, String icon, String label) {}

    public record Problem(String key, String name, String kind, Set<String> crops, Map<String, Integer> symptoms,
                          String cause, List<String> advice, String urgency, boolean seeOfficer) {}

    /** One possible cause for what the farmer described. score is 0-100; confidence is HIGH, MEDIUM or LOW. */
    public record Match(String key, String name, String kind, String cause, List<String> advice, String urgency,
                        int score, String confidence, boolean seeOfficer) {}

    public static final String DISCLAIMER = "This guide gives likely causes from the signs you chose. It is not a laboratory test. "
            + "If the problem is spreading fast, or you are unsure, send it to your county officer.";

    private static final List<Symptom> SYMPTOMS = new ArrayList<>();
    private static final List<Problem> PROBLEMS = new ArrayList<>();

    private static void sym(String key, String icon, String label) { SYMPTOMS.add(new Symptom(key, icon, label)); }

    static {
        sym("YELLOW_LEAVES", "🟡", "Leaves turning yellow");
        sym("YELLOW_STREAKS", "〰️", "Pale yellow stripes along the leaves");
        sym("MOTTLED_LEAVES", "🍂", "Patchy light and dark green leaves (mosaic)");
        sym("LEAF_SPOTS", "🟤", "Brown or grey spots on leaves");
        sym("BLACK_LESIONS", "⚫", "Dark, wet-looking patches that spread fast");
        sym("ORANGE_PUSTULES", "🟠", "Orange or rust-coloured powder on leaves");
        sym("WHITE_POWDER", "⚪", "White powder or mould on leaves");
        sym("HOLES_IN_LEAVES", "🕳️", "Holes or chewed leaves");
        sym("FRASS", "🐛", "Sawdust-like droppings in the leaf funnel or on leaves");
        sym("BORER_HOLES", "🪱", "Small holes in the stem, or the centre shoot dies");
        sym("INSECT_CLUSTERS", "🐜", "Clusters of tiny insects, sticky leaves");
        sym("WEBBING", "🕸️", "Fine webbing, leaves look dusty or bronzed");
        sym("CURLED_LEAVES", "🌀", "Curled or twisted leaves");
        sym("LEAF_MINES", "〰", "White winding tracks inside the leaf");
        sym("WILTING", "🥀", "Plant wilts even when the soil is moist");
        sym("DROUGHT_WILT", "☀️", "Wilting in dry soil or hot weather");
        sym("BROWN_EDGES", "🍁", "Leaf edges drying and turning brown");
        sym("PURPLE_LEAVES", "🟣", "Purple or reddish leaves");
        sym("STUNTED", "📉", "Plants small and slow growing");
        sym("ROT_STEM_ROOT", "🦠", "Rotting stem base or roots");
        sym("SWOLLEN_ROOTS", "🥔", "Swollen, knobbly roots");
        sym("FRUIT_ROT", "🍅", "Dark sunken patch at the bottom of the fruit");
        sym("FRUIT_HOLES", "🔴", "Holes or tunnels in fruit or pods");
        sym("V_YELLOW_EDGE", "🔻", "Yellow V-shaped patch from the leaf edge");
        sym("PLANT_PARASITE", "🌺", "Small pink or purple weed flowers growing beside the crop");
        sym("GRAIN_MOULD", "🌾", "Mouldy or discoloured grain or cobs");
    }

    private static Set<String> only(String... c) { return new LinkedHashSet<>(Arrays.asList(c)); }

    private static Map<String, Integer> s(Object... kv) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put((String) kv[i], (Integer) kv[i + 1]);
        return m;
    }

    private static void add(String key, String name, String kind, Set<String> crops, Map<String, Integer> symptoms,
                            String cause, String urgency, boolean officer, String... advice) {
        PROBLEMS.add(new Problem(key, name, kind, crops, symptoms, cause, List.of(advice), urgency, officer));
    }

    private static final String SAFE = "If you use a pesticide, buy one registered for the crop, follow the label, wear protective clothing and respect the waiting period before harvest.";

    static {
        // ---------------- maize
        add("FALL_ARMYWORM", "Fall armyworm", "PEST", only("Maize", "Sorghum"),
                s("HOLES_IN_LEAVES", 3, "FRASS", 4, "STUNTED", 1),
                "A caterpillar that hides in the leaf funnel and eats the young leaves and cobs.", "HIGH", true,
                "Scout early in the morning: look in the funnel of 20 plants across the field.",
                "Pick and crush caterpillars by hand on small fields; a pinch of dry sand or fine soil in the funnel can help.",
                "Report it to your county officer so neighbours are warned.", SAFE);
        add("MLN", "Maize lethal necrosis (MLN)", "DISEASE", only("Maize"),
                s("YELLOW_LEAVES", 2, "MOTTLED_LEAVES", 3, "BROWN_EDGES", 2, "STUNTED", 2),
                "A virus disease: leaves yellow from the edge, then dry, and the young leaves can die.", "HIGH", true,
                "Do not move the plants or seed from this field.", "Uproot and destroy badly affected plants early.",
                "Tell your county officer at once; this disease is controlled by the Government.",
                "Plant clean certified seed and rotate with beans or other non-cereal crops.");
        add("MAIZE_STREAK", "Maize streak virus", "DISEASE", only("Maize"),
                s("YELLOW_STREAKS", 5, "STUNTED", 2, "YELLOW_LEAVES", 1),
                "A virus spread by leafhoppers: broken yellow stripes along the leaf veins.", "MEDIUM", false,
                "Remove and destroy badly infected plants.", "Plant early and use tolerant, certified varieties.", "Keep the field and the borders free of grass weeds.");
        add("LEAF_BLIGHT", "Grey leaf spot or leaf blight", "DISEASE", only("Maize", "Sorghum"),
                s("LEAF_SPOTS", 5, "BROWN_EDGES", 1),
                "A fungus that likes humid weather and leaves long grey or brown spots.", "MEDIUM", false,
                "Rotate maize with a different crop and bury or remove old crop residue.", "Use tolerant varieties next season.", "Space the plants properly so air can move.");
        add("COMMON_RUST", "Rust", "DISEASE", only("Maize", "Beans", "Wheat"),
                s("ORANGE_PUSTULES", 6),
                "A fungus that produces orange or brown powdery pustules and spreads in cool, damp weather.", "MEDIUM", false,
                "Remove badly affected plants and weeds around the field.", "Rotate crops and plant resistant varieties next season.",
                "For wheat, report heavy rust early because it can spread across a whole county.");
        add("STEM_BORER", "Stem borer", "PEST", only("Maize", "Sorghum"),
                s("BORER_HOLES", 5, "HOLES_IN_LEAVES", 1, "FRASS", 1, "STUNTED", 1),
                "Caterpillars that tunnel inside the stem and kill the growing centre ('dead heart').", "MEDIUM", false,
                "Pull out and destroy plants with a dead heart.", "After harvest, remove or burn stalks so borers do not survive.", "Rotate the crop and plant at the start of the rains.");
        add("NITROGEN_HUNGER", "Not enough nitrogen", "NUTRIENT", only("Maize", "Sorghum", "Wheat", "Kale", "Cabbage"),
                s("YELLOW_LEAVES", 4, "STUNTED", 3, "V_YELLOW_EDGE", 1),
                "The soil may be short of nitrogen: the older lower leaves yellow first.", "LOW", false,
                "Top-dress with the fertilizer your officer or agro-dealer recommends for your soil.", "Add well-rotted manure or compost.",
                "A soil test tells you exactly what the field needs.");
        add("STRIGA", "Striga (witchweed)", "WEED", only("Maize", "Sorghum"),
                s("PLANT_PARASITE", 6, "STUNTED", 2, "YELLOW_LEAVES", 1),
                "A parasitic weed that feeds on the crop roots and stunts the plants.", "MEDIUM", true,
                "Pull the witchweed before it flowers and seeds, and burn it.", "Rotate with beans or groundnuts; do not let it seed.",
                "Ask your county officer about resistant varieties and soil fertility.");
        // ---------------- beans
        add("BEAN_ANTHRACNOSE", "Anthracnose or angular leaf spot", "DISEASE", only("Beans"),
                s("LEAF_SPOTS", 4, "BLACK_LESIONS", 3),
                "Seed-borne fungi that make dark spots on leaves, stems and pods in wet weather.", "MEDIUM", false,
                "Do not work in the field while the plants are wet.", "Use clean seed and rotate beans every season.", "Remove and destroy badly affected plants.");
        add("BEAN_FLY", "Bean fly", "PEST", only("Beans"),
                s("WILTING", 3, "STUNTED", 3, "ROT_STEM_ROOT", 3, "YELLOW_LEAVES", 1),
                "A small fly whose maggots tunnel in the young stem and cause wilting and a swollen stem base.", "MEDIUM", false,
                "Plant early and earth up the stem base so extra roots grow.", "Remove very badly affected seedlings.", "Rotate with a cereal crop.");
        // ---------------- potato / tomato
        add("LATE_BLIGHT", "Late blight", "DISEASE", only("Irish Potato", "Tomato"),
                s("BLACK_LESIONS", 6, "LEAF_SPOTS", 2, "FRUIT_ROT", 1, "WILTING", 1),
                "A fast fungus-like disease in cool, wet weather. It can destroy a field in days.", "HIGH", true,
                "Act the same day: remove infected plants and do not leave them in the field.", "Do not use tubers or fruit from badly infected plants as seed.",
                "Ask your county officer which fungicide is registered and when to spray.", SAFE);
        add("EARLY_BLIGHT", "Early blight", "DISEASE", only("Irish Potato", "Tomato"),
                s("LEAF_SPOTS", 5, "YELLOW_LEAVES", 1, "BROWN_EDGES", 1),
                "A fungus that makes brown spots with rings, starting on the lowest leaves.", "MEDIUM", false,
                "Remove the lowest infected leaves and keep the leaves dry (water at the base).", "Rotate with a non-potato, non-tomato crop for at least two seasons.", "Keep the plants well fed and not stressed.");
        add("BACTERIAL_WILT", "Bacterial wilt", "DISEASE", only("Irish Potato", "Tomato"),
                s("WILTING", 6, "ROT_STEM_ROOT", 2, "YELLOW_LEAVES", 1),
                "A soil bacterium: the whole plant wilts suddenly even when the soil is wet, and does not recover.", "HIGH", true,
                "Uproot and destroy wilted plants; do not plant potato, tomato or other relatives there for 3 seasons.",
                "Clean tools and boots when leaving the field.", "Use clean seed and avoid water running from infected fields.");
        add("TUTA_ABSOLUTA", "Tomato leaf miner (Tuta absoluta)", "PEST", only("Tomato"),
                s("LEAF_MINES", 6, "FRUIT_HOLES", 3, "HOLES_IN_LEAVES", 1),
                "A small moth whose caterpillars tunnel inside leaves and fruit.", "HIGH", true,
                "Remove and destroy affected leaves and fruit.", "Use pheromone traps to watch and reduce the moths.", "Report heavy infestations to your county officer.", SAFE);
        add("BLOSSOM_END_ROT", "Blossom end rot", "NUTRIENT", only("Tomato"),
                s("FRUIT_ROT", 6),
                "Not a germ: the fruit lacks calcium, usually because watering is uneven.", "LOW", false,
                "Water regularly and evenly; mulch to keep the soil moisture steady.", "Do not over-fertilize with nitrogen.", "Pick off affected fruit.");
        add("WHITEFLY_VIRUS", "Whitefly and leaf curl virus", "PEST", only("Tomato", "Kale", "Cabbage"),
                s("CURLED_LEAVES", 4, "YELLOW_LEAVES", 2, "STUNTED", 2, "INSECT_CLUSTERS", 2),
                "Tiny white flies suck the plant and carry viruses that curl and yellow the leaves.", "MEDIUM", false,
                "Remove and burn badly curled plants early.", "Use sticky traps and keep weeds down around the field.", "Raise seedlings under a net.", SAFE);
        add("POTATO_VIRUS", "Potato virus (mosaic or leaf roll)", "DISEASE", only("Irish Potato"),
                s("MOTTLED_LEAVES", 4, "CURLED_LEAVES", 3, "STUNTED", 2),
                "Viruses carried in seed tubers and by aphids.", "MEDIUM", false,
                "Rogue (pull out) sick plants early.", "Buy certified clean seed potato.", "Do not save seed from a sick crop.");
        // ---------------- brassicas
        add("DIAMONDBACK_MOTH", "Diamondback moth caterpillars", "PEST", only("Kale", "Cabbage"),
                s("HOLES_IN_LEAVES", 5, "FRASS", 1),
                "Small green caterpillars that eat the underside of the leaves and leave holes.", "MEDIUM", false,
                "Check the underside of the leaves weekly; hand-pick if few.", "Plant a trap crop and rotate away from cabbage family crops.", "Vary any spray you use; the moth quickly resists.", SAFE);
        add("BLACK_ROT", "Black rot", "DISEASE", only("Kale", "Cabbage"),
                s("V_YELLOW_EDGE", 6, "BLACK_LESIONS", 1),
                "A bacterial disease: yellow V-shaped patches from the leaf edge with blackened veins.", "MEDIUM", false,
                "Remove infected leaves and destroy them; do not compost.", "Rotate with non-cabbage crops for 3 years.", "Use clean seed and do not work in the field when wet.");
        add("CLUBROOT", "Clubroot", "DISEASE", only("Kale", "Cabbage"),
                s("SWOLLEN_ROOTS", 6, "WILTING", 2, "STUNTED", 2),
                "A soil disease that makes the roots swollen and knobbly, so the plant cannot drink.", "MEDIUM", true,
                "Uproot a few plants to check the roots; destroy affected plants.", "Do not plant cabbage-family crops there for years; lime acid soils.",
                "Ask your county officer how to manage the soil.");
        add("APHIDS", "Aphids", "PEST", only("Kale", "Cabbage", "Beans", "Wheat", "Irish Potato", "Tomato"),
                s("INSECT_CLUSTERS", 5, "CURLED_LEAVES", 2, "STUNTED", 1),
                "Small soft insects that cluster on young shoots and underside of leaves and leave a sticky coating.", "LOW", false,
                "Wash them off with a strong stream of water or crush by hand on small plots.", "Protect ladybirds and other helpful insects.", "Keep weeds down and avoid too much nitrogen.", SAFE);
        add("SPIDER_MITES", "Spider mites", "PEST", only("Tomato", "Beans", "Kale", "Cabbage"),
                s("WEBBING", 6, "BROWN_EDGES", 1, "YELLOW_LEAVES", 1),
                "Very small mites in hot, dry weather that suck the leaves and spin fine webs.", "MEDIUM", false,
                "Spray the underside of leaves with plain water, and keep the plants watered.", "Remove badly infested leaves.", "Avoid dusty field edges.", SAFE);
        add("DOWNY_MILDEW", "Mildew", "DISEASE", only("Kale", "Cabbage", "Wheat"),
                s("WHITE_POWDER", 6, "YELLOW_LEAVES", 1),
                "A fungus that likes cool, humid weather and covers leaves in a white or grey coating.", "LOW", false,
                "Space the plants and avoid watering the leaves late in the day.", "Remove affected leaves and old crop residue.", "Rotate crops each season.");
        // ---------------- wheat
        add("WHEAT_SEPTORIA", "Septoria leaf blotch", "DISEASE", only("Wheat"),
                s("LEAF_SPOTS", 5, "BROWN_EDGES", 1),
                "A fungus that makes brown blotches on the leaves in wet weather.", "MEDIUM", false,
                "Rotate away from wheat and bury old stubble.", "Plant resistant varieties.", "Ask your county officer about the right time to spray.", SAFE);
        add("STEM_RUST", "Stem or yellow rust of wheat", "DISEASE", only("Wheat"),
                s("ORANGE_PUSTULES", 5, "YELLOW_STREAKS", 3),
                "A serious fungus whose spores are blown over long distances. Early report saves the neighbours' crop.", "HIGH", true,
                "Report it to your county officer immediately.", "Do not wait for it to spread; ask about registered fungicides and timing.", SAFE);
        // ---------------- sorghum
        add("SORGHUM_MOULD", "Grain mould", "DISEASE", only("Sorghum", "Maize"),
                s("GRAIN_MOULD", 6),
                "Fungi that grow on grain heads and cobs when rain falls at flowering and grain filling.", "LOW", false,
                "Harvest at maturity and dry the grain quickly to a safe level before storing.", "Do not eat or sell mouldy grain: some moulds make toxins (aflatoxin).", "Use varieties that dry quickly and have loose heads.");
        // ---------------- any crop
        add("DROUGHT_STRESS", "Dry soil or heat stress", "WATER", only(),
                s("DROUGHT_WILT", 6, "BROWN_EDGES", 3, "YELLOW_LEAVES", 1),
                "Not enough water: the plant wilts in the heat and recovers in the evening when watered.", "LOW", false,
                "Water in the early morning or evening, and mulch to keep moisture.", "Reduce weeds that compete for water.", "Plan planting dates with the calendar and weather.");
        add("WATERLOGGING", "Waterlogging or root rot", "WATER", only(),
                s("WILTING", 3, "YELLOW_LEAVES", 3, "ROT_STEM_ROOT", 4, "STUNTED", 1),
                "Soil stays wet too long, roots lose air and rot.", "MEDIUM", false,
                "Dig drainage channels and plant on ridges or raised beds.", "Do not over-water; avoid compacted soil.", "Do not replant the same spot until it dries.");
        add("PHOSPHORUS_HUNGER", "Not enough phosphorus", "NUTRIENT", only(),
                s("PURPLE_LEAVES", 6, "STUNTED", 2),
                "Young plants turn purple when the soil is low in phosphorus, especially in cold or acid soils.", "LOW", false,
                "Use phosphate or planting fertilizer in the planting hole, as your officer advises.", "Lime very acid soil.", "A soil test tells you what is needed.");
    }

    public static List<Symptom> symptoms() { return Collections.unmodifiableList(SYMPTOMS); }

    public static List<Problem> problems() { return Collections.unmodifiableList(PROBLEMS); }

    public static List<String> crops() {
        List<String> out = new ArrayList<>();
        for (CropCalendar.Crop c : CropCalendar.all()) out.add(c.name());
        return out;
    }

    public static String symptomLabel(String key) {
        for (Symptom s : SYMPTOMS) if (s.key().equals(key)) return s.label();
        return key;
    }

    public static boolean knownSymptom(String key) {
        for (Symptom s : SYMPTOMS) if (s.key().equals(key)) return true;
        return false;
    }

    public static Problem problem(String key) {
        for (Problem p : PROBLEMS) if (p.key().equals(key)) return p;
        return null;
    }

    /**
     * Likely causes, best first (at most 3). Score = how much of the problem's typical signs the farmer ticked (70%)
     * and how many of the ticked signs the problem explains (30%). An unknown crop is only matched with the general problems.
     */
    public static List<Match> diagnose(String cropText, Collection<String> ticked) {
        CropCalendar.Crop c = cropText == null ? null : CropCalendar.find(cropText);
        String crop = c == null ? null : c.name();
        Set<String> picked = new LinkedHashSet<>();
        if (ticked != null) for (String t : ticked) if (knownSymptom(t)) picked.add(t);
        if (picked.isEmpty()) return List.of();

        List<Match> out = new ArrayList<>();
        for (Problem p : PROBLEMS) {
            boolean general = p.crops().isEmpty();
            if (!general && (crop == null || !p.crops().contains(crop))) continue;
            int total = 0, hit = 0, explained = 0;
            for (Map.Entry<String, Integer> e : p.symptoms().entrySet()) {
                total += e.getValue();
                if (picked.contains(e.getKey())) { hit += e.getValue(); explained++; }
            }
            if (explained == 0) continue;
            double coverage = (double) hit / total;
            double precision = (double) explained / picked.size();
            double score = 0.7 * coverage + 0.3 * precision;
            if (general) score *= 0.85;                         // a specific problem for this crop beats a general one
            int pct = (int) Math.round(score * 100);
            String conf = pct >= 70 ? "HIGH" : pct >= 45 ? "MEDIUM" : "LOW";
            if (pct < 20) continue;
            out.add(new Match(p.key(), p.name(), p.kind(), p.cause(), p.advice(), p.urgency(), pct, conf,
                    p.seeOfficer() || "LOW".equals(conf)));
        }
        out.sort((a, b) -> b.score() != a.score() ? Integer.compare(b.score(), a.score()) : a.name().compareTo(b.name()));
        return out.size() > 3 ? new ArrayList<>(out.subList(0, 3)) : out;
    }
}
