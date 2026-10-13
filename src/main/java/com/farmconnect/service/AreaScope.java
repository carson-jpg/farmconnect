package com.farmconnect.service;

import com.farmconnect.model.Role;
import com.farmconnect.model.User;

import java.util.Set;
import java.util.TreeSet;

/**
 * Which part of the country a staff member may see.
 * Administrators see everything. An officer with an assigned county sees that county (or only the sub-counties
 * assigned inside it). An officer who has not been assigned yet keeps seeing everything until an administrator
 * gives them an area.
 */
public final class AreaScope {
    private static final AreaScope ALL = new AreaScope(true, null, Set.of());

    private final boolean all;
    private final String county;
    private final Set<String> subCounties;

    private AreaScope(boolean all, String county, Set<String> subCounties) {
        this.all = all;
        this.county = county;
        this.subCounties = subCounties;
    }

    public static AreaScope of(User u) {
        if (u == null || u.getRole() != Role.OFFICER) return ALL;
        String c = u.getAssignedCounty();
        if (c == null || c.isBlank()) return ALL;
        Set<String> subs = u.getAssignedSubCounties() == null ? Set.of() : new TreeSet<>(u.getAssignedSubCounties());
        return new AreaScope(false, c, subs);
    }

    public boolean all() { return all; }
    public String county() { return county; }
    public Set<String> subCounties() { return subCounties; }

    /** Is a farmer (or group, or service) in this sub-county inside the area? */
    public boolean allows(String subCounty) {
        if (all) return true;
        if (subCounty == null || subCounty.isBlank()) return false;
        if (!subCounties.isEmpty()) return subCounties.contains(subCounty);
        return county.equals(Locations.countyOf(subCounty));
    }

    public String label() {
        if (all) return "All counties";
        if (subCounties.isEmpty()) return county + " County";
        return county + " County: " + String.join(", ", subCounties);
    }
}
