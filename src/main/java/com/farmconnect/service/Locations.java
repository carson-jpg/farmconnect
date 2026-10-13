package com.farmconnect.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The counties FarmConnect serves and their sub-counties (the constituency names, which is how farmers know them).
 * To add a county or fix a name, change it here AND in android/.../ui/Labels.java (a build check compares the two).
 * Sub-county names must be unique across counties: the county is worked out from the sub-county.
 */
public final class Locations {
    public record County(String name, List<String> subCounties) {}

    private static final Map<String, List<String>> DATA = new LinkedHashMap<>();
    private static final Map<String, String> COUNTY_OF = new LinkedHashMap<>();

    private static void add(String county, String... subCounties) {
        DATA.put(county, List.of(subCounties));
        for (String s : subCounties) COUNTY_OF.put(s, county);
    }

    static {
        add("Bungoma", "Mt. Elgon", "Sirisia", "Kabuchai", "Bumula", "Kanduyi", "Webuye East", "Webuye West", "Kimilili", "Tongaren");
        add("Homa Bay", "Kasipul", "Kabondo Kasipul", "Karachuonyo", "Rangwe", "Homa Bay Town", "Ndhiwa", "Suba North", "Suba South");
        add("Kakamega", "Lugari", "Likuyani", "Malava", "Lurambi", "Navakholo", "Mumias West", "Mumias East", "Matungu", "Butere", "Khwisero", "Shinyalu", "Ikolomani");
        add("Kisumu", "Kisumu East", "Kisumu West", "Kisumu Central", "Seme", "Nyando", "Muhoroni", "Nyakach");
        add("Kitui", "Mwingi North", "Mwingi West", "Mwingi Central", "Kitui West", "Kitui Rural", "Kitui Central", "Kitui East", "Kitui South");
        add("Machakos", "Masinga", "Yatta", "Kangundo", "Matungulu", "Kathiani", "Mavoko", "Machakos Town", "Mwala");
        add("Meru", "Igembe South", "Igembe Central", "Igembe North", "Tigania West", "Tigania East", "North Imenti", "Buuri", "Central Imenti", "South Imenti");
        add("Migori", "Rongo", "Awendo", "Suna East", "Suna West", "Uriri", "Nyatike", "Kuria East", "Kuria West");
        add("Nakuru", "Nakuru Town East", "Nakuru Town West", "Naivasha", "Gilgil", "Kuresoi North", "Kuresoi South", "Subukia", "Rongai", "Bahati", "Njoro", "Molo");
        add("Nyeri", "Tetu", "Kieni", "Mathira", "Othaya", "Mukurweini", "Nyeri Town");
        add("Trans Nzoia", "Cherangany", "Endebess", "Kiminini", "Kwanza", "Saboti");
        add("Uasin Gishu", "Soy", "Turbo", "Moiben", "Ainabkoi", "Kapseret", "Kesses");
        add("West Pokot", "Kapenguria", "Sigor", "Kacheliba", "Pokot South");
    }

    /** A main town per county and its coordinates, used to look up the weather for a farmer's area. */
    private static final Map<String, String> TOWN = Map.ofEntries(
            Map.entry("Bungoma", "Bungoma"), Map.entry("Homa Bay", "Homa Bay"), Map.entry("Kakamega", "Kakamega"),
            Map.entry("Kisumu", "Kisumu"), Map.entry("Kitui", "Kitui"), Map.entry("Machakos", "Machakos"), Map.entry("Meru", "Meru"),
            Map.entry("Migori", "Migori"), Map.entry("Nakuru", "Nakuru"), Map.entry("Nyeri", "Nyeri"), Map.entry("Trans Nzoia", "Kitale"),
            Map.entry("Uasin Gishu", "Eldoret"), Map.entry("West Pokot", "Kapenguria"));
    private static final Map<String, double[]> TOWN_AT = Map.ofEntries(
            Map.entry("Bungoma", new double[]{0.5635, 34.5606}), Map.entry("Homa Bay", new double[]{-0.5273, 34.4571}),
            Map.entry("Kakamega", new double[]{0.2827, 34.7519}), Map.entry("Kisumu", new double[]{-0.0917, 34.7680}),
            Map.entry("Kitui", new double[]{-1.3667, 38.0106}), Map.entry("Machakos", new double[]{-1.5177, 37.2634}),
            Map.entry("Meru", new double[]{0.0470, 37.6490}), Map.entry("Migori", new double[]{-1.0634, 34.4731}),
            Map.entry("Nakuru", new double[]{-0.3031, 36.0800}), Map.entry("Nyeri", new double[]{-0.4201, 36.9476}),
            Map.entry("Trans Nzoia", new double[]{1.0191, 35.0020}), Map.entry("Uasin Gishu", new double[]{0.5143, 35.2698}),
            Map.entry("West Pokot", new double[]{1.2389, 35.1119}));

    public static String townOf(String county) { return TOWN.getOrDefault(county, county); }
    /** {latitude, longitude} of the county's main town. */
    public static double[] townCoords(String county) { return TOWN_AT.getOrDefault(county, TOWN_AT.get("Trans Nzoia")); }

    private Locations() {}

    public static List<County> all() {
        List<County> out = new ArrayList<>();
        DATA.forEach((k, v) -> out.add(new County(k, v)));
        return Collections.unmodifiableList(out);
    }

    public static boolean isCounty(String county) { return county != null && DATA.containsKey(county); }
    public static List<String> subCountiesOf(String county) { return DATA.getOrDefault(county, List.of()); }
    public static boolean isSubCounty(String subCounty) { return subCounty != null && COUNTY_OF.containsKey(subCounty); }
    /** The county a sub-county belongs to, or null when the name is not one we know. */
    public static String countyOf(String subCounty) { return subCounty == null ? null : COUNTY_OF.get(subCounty); }

    /** For forms: an empty value is fine, a value that is not on the list is not. */
    public static void requireKnownOrBlank(String subCounty) {
        if (subCounty != null && !subCounty.isBlank() && !isSubCounty(subCounty))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a sub-county from the list");
    }

    /** For officer assignments: the county must exist and every sub-county must belong to it. */
    public static void requireValidAssignment(String county, List<String> subCounties) {
        if (county == null || county.isBlank()) {
            if (subCounties != null && !subCounties.isEmpty())
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose a county first");
            return;
        }
        if (!isCounty(county)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown county");
        if (subCounties != null) for (String s : subCounties) {
            if (!county.equals(countyOf(s)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, s + " is not in " + county + " County");
        }
    }
}
