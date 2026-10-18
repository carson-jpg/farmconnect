package com.farmconnect.app.ui;

/** Icons, names and option lists shared by the community screens. */
final class Labels {
    private Labels() {}

    /** Counties the app covers, and their sub-counties. Same list as the server's Locations class (a build check compares them). */
    static final String[] COUNTIES = {"Bungoma", "Homa Bay", "Kakamega", "Kisumu", "Kitui", "Machakos", "Meru", "Migori", "Nakuru", "Nyeri", "Trans Nzoia", "Uasin Gishu", "West Pokot"};
    static final String[][] SUBS = {
        {"Mt. Elgon", "Sirisia", "Kabuchai", "Bumula", "Kanduyi", "Webuye East", "Webuye West", "Kimilili", "Tongaren"},
        {"Kasipul", "Kabondo Kasipul", "Karachuonyo", "Rangwe", "Homa Bay Town", "Ndhiwa", "Suba North", "Suba South"},
        {"Lugari", "Likuyani", "Malava", "Lurambi", "Navakholo", "Mumias West", "Mumias East", "Matungu", "Butere", "Khwisero", "Shinyalu", "Ikolomani"},
        {"Kisumu East", "Kisumu West", "Kisumu Central", "Seme", "Nyando", "Muhoroni", "Nyakach"},
        {"Mwingi North", "Mwingi West", "Mwingi Central", "Kitui West", "Kitui Rural", "Kitui Central", "Kitui East", "Kitui South"},
        {"Masinga", "Yatta", "Kangundo", "Matungulu", "Kathiani", "Mavoko", "Machakos Town", "Mwala"},
        {"Igembe South", "Igembe Central", "Igembe North", "Tigania West", "Tigania East", "North Imenti", "Buuri", "Central Imenti", "South Imenti"},
        {"Rongo", "Awendo", "Suna East", "Suna West", "Uriri", "Nyatike", "Kuria East", "Kuria West"},
        {"Nakuru Town East", "Nakuru Town West", "Naivasha", "Gilgil", "Kuresoi North", "Kuresoi South", "Subukia", "Rongai", "Bahati", "Njoro", "Molo"},
        {"Tetu", "Kieni", "Mathira", "Othaya", "Mukurweini", "Nyeri Town"},
        {"Cherangany", "Endebess", "Kiminini", "Kwanza", "Saboti"},
        {"Soy", "Turbo", "Moiben", "Ainabkoi", "Kapseret", "Kesses"},
        {"Kapenguria", "Sigor", "Kacheliba", "Pokot South"}
    };
    static final String[] SUB_COUNTIES, SUB_LABELS;
    static {
        int n = 0;
        for (String[] s : SUBS) n += s.length;
        SUB_COUNTIES = new String[n];
        SUB_LABELS = new String[n];
        int k = 0;
        for (int c = 0; c < COUNTIES.length; c++) {
            for (String s : SUBS[c]) { SUB_COUNTIES[k] = s; SUB_LABELS[k] = s + " · " + COUNTIES[c]; k++; }
        }
    }

    static String[] subCountiesOf(String county) {
        int i = idx(COUNTIES, county);
        return i < 0 ? new String[0] : SUBS[i];
    }

    /** The county a sub-county belongs to, or null. */
    static String countyOf(String subCounty) {
        for (int c = 0; c < COUNTIES.length; c++) if (idx(SUBS[c], subCounty) >= 0) return COUNTIES[c];
        return null;
    }

    /** Two steps: choose the county, then one of its sub-counties. */
    static void pickSubCounty(android.app.Activity a, java.util.function.Consumer<String> done) {
        new androidx.appcompat.app.AlertDialog.Builder(a).setTitle(com.farmconnect.app.util.I18n.t("Select county"))
                .setItems(COUNTIES, (d, ci) -> new androidx.appcompat.app.AlertDialog.Builder(a).setTitle(COUNTIES[ci])
                        .setItems(SUBS[ci], (d2, si) -> done.accept(SUBS[ci][si])).show())
                .show();
    }

    static final String[] POST_TYPES = {"TIP", "ARTICLE", "ADVISORY", "ANNOUNCEMENT", "PROGRAMME", "TRAINING", "OPPORTUNITY"};
    static final String[] POST_NAMES = {"Farming tip", "Article", "Advisory", "Announcement", "County programme", "Training", "Opportunity"};
    static final String[] TOPICS = {"GENERAL", "CROPS", "LIVESTOCK", "PESTS_DISEASES", "SOIL_WATER", "POST_HARVEST", "MARKETS", "FINANCE"};
    static final String[] TOPIC_NAMES = {"General", "Crops", "Livestock", "Pests & diseases", "Soil & water", "Post-harvest", "Markets", "Finance"};

    static final String[] SERVICE_CATS = {"AGRO_DEALER", "VETERINARY", "EXTENSION", "TRANSPORT", "STORAGE", "MACHINERY", "FINANCE", "BUYER_COMPANY", "OTHER"};
    static final String[] SERVICE_NAMES = {"Agro-dealer", "Veterinary", "Extension", "Transport", "Storage", "Machinery hire", "Finance", "Produce buyer", "Other"};

    static final String[] GROUP_TYPES = {"COOPERATIVE", "FARMER_GROUP", "SACCO", "YOUTH_GROUP", "WOMEN_GROUP"};
    static final String[] GROUP_NAMES = {"Cooperative", "Farmer group", "SACCO", "Youth group", "Women group"};

    static final String[] RECORD_TYPES = {"PLANTING", "HARVEST", "LIVESTOCK", "EXPENSE", "INCOME", "NOTE"};
    static final String[] RECORD_NAMES = {"Planting", "Harvest", "Livestock", "Expense", "Income", "Note"};

    static int idx(String[] arr, String v) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(v)) return i;
        return -1;
    }

    static String name(String[] values, String[] names, String v) {
        int i = idx(values, v);
        return com.farmconnect.app.util.I18n.t(i < 0 ? (v == null ? "" : v) : names[i]);
    }

    static String postIcon(String t) {
        switch (t == null ? "" : t) {
            case "TIP": return "💡";
            case "ARTICLE": return "📰";
            case "ADVISORY": return "🌾";
            case "ANNOUNCEMENT": return "📢";
            case "PROGRAMME": return "🏛";
            case "TRAINING": return "🎓";
            case "OPPORTUNITY": return "💼";
            default: return "📄";
        }
    }

    static String serviceIcon(String c) {
        switch (c == null ? "" : c) {
            case "AGRO_DEALER": return "🌱";
            case "VETERINARY": return "🐄";
            case "EXTENSION": return "🧑‍🌾";
            case "TRANSPORT": return "🚚";
            case "STORAGE": return "🏚";
            case "MACHINERY": return "🚜";
            case "FINANCE": return "🏦";
            case "BUYER_COMPANY": return "🛒";
            default: return "🧰";
        }
    }

    static String groupIcon(String t) {
        switch (t == null ? "" : t) {
            case "COOPERATIVE": return "🤝";
            case "SACCO": return "🏦";
            case "YOUTH_GROUP": return "🧑";
            case "WOMEN_GROUP": return "👩‍🌾";
            default: return "👥";
        }
    }

    static String recordIcon(String t) {
        switch (t == null ? "" : t) {
            case "PLANTING": return "🌱";
            case "HARVEST": return "🌾";
            case "LIVESTOCK": return "🐄";
            case "EXPENSE": return "💸";
            case "INCOME": return "💰";
            default: return "📝";
        }
    }

    static String cropIcon(String crop) {
        String c = crop == null ? "" : crop.toLowerCase();
        if (c.contains("maize") || c.contains("corn")) return "🌽";
        if (c.contains("bean")) return "🫘";
        if (c.contains("wheat") || c.contains("rice") || c.contains("sorghum") || c.contains("millet")) return "🌾";
        if (c.contains("potato")) return "🥔";
        if (c.contains("tomato")) return "🍅";
        if (c.contains("cabbage") || c.contains("kale") || c.contains("sukuma") || c.contains("spinach")) return "🥬";
        if (c.contains("onion")) return "🧅";
        if (c.contains("milk")) return "🥛";
        if (c.contains("egg")) return "🥚";
        if (c.contains("banana")) return "🍌";
        if (c.contains("avocado")) return "🥑";
        if (c.contains("carrot")) return "🥕";
        if (c.contains("chicken") || c.contains("poultry")) return "🐔";
        if (c.contains("beef") || c.contains("cattle") || c.contains("cow")) return "🐄";
        return "🌱";
    }

    static String stars(int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) sb.append(i < n ? "★" : "☆");
        return sb.toString();
    }

    static String roleIcon(String r) {
        switch (r == null ? "" : r) {
            case "FARMER": return "🧑‍🌾";
            case "BUYER": return "🛒";
            case "OFFICER": return "🏛";
            case "ADMIN": return "🛡";
            case "COUNTY_ADMIN": return "🗺";
            default: return "👤";
        }
    }

    static String roleName(String r) {
        if (r == null) return "";
        switch (r) {
            case "OFFICER": return com.farmconnect.app.util.I18n.t("County officer");
            case "ADMIN": return com.farmconnect.app.util.I18n.t("Super admin");
            case "COUNTY_ADMIN": return com.farmconnect.app.util.I18n.t("County admin");
            default: return com.farmconnect.app.util.I18n.t(r.charAt(0) + r.substring(1).toLowerCase());
        }
    }

    static boolean isStaff() {
        String r = com.farmconnect.app.data.Session.role();
        return "ADMIN".equals(r) || "COUNTY_ADMIN".equals(r) || "OFFICER".equals(r);
    }
}
