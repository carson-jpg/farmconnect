package com.farmconnect.app.ui;

/** Icons, names and option lists shared by the community screens. */
final class Labels {
    private Labels() {}

    static final String[] SUB_COUNTIES = {"Kwanza", "Endebess", "Saboti", "Kiminini", "Cherangany"};

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
        return i < 0 ? (v == null ? "" : v) : names[i];
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

    static String roleIcon(String r) {
        switch (r == null ? "" : r) {
            case "FARMER": return "🧑‍🌾";
            case "BUYER": return "🛒";
            case "OFFICER": return "🏛";
            case "ADMIN": return "🛡";
            default: return "👤";
        }
    }

    static String roleName(String r) {
        if (r == null) return "";
        switch (r) {
            case "OFFICER": return "County officer";
            case "ADMIN": return "Administrator";
            default: return r.charAt(0) + r.substring(1).toLowerCase();
        }
    }

    static boolean isStaff() {
        String r = com.farmconnect.app.data.Session.role();
        return "ADMIN".equals(r) || "OFFICER".equals(r);
    }
}
