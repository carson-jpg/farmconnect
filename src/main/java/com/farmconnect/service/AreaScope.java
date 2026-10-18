package com.farmconnect.service;

import com.farmconnect.model.Role;
import com.farmconnect.model.User;

import java.util.Set;
import java.util.TreeSet;

/**
 * Which part of the country a staff member may see.
 * The super administrator sees everything. A county admin sees exactly their assigned county, and sees NOTHING until
 * the super admin assigns one. An officer with an assigned county sees that county (or only the sub-counties
 * assigned inside it); an officer who has not been assigned yet keeps seeing everything until an administrator
 * gives them an area.
 */
public final class AreaScope {
    private static final AreaScope ALL = new AreaScope(true, false, null, Set.of());
    /** A county admin who has not been given a county yet: allowed to see nothing at all. */
    private static final AreaScope NONE = new AreaScope(false, true, null, Set.of());

    private final boolean all;
    private final boolean none;
    private final String county;
    private final Set<String> subCounties;

    private AreaScope(boolean all, boolean none, String county, Set<String> subCounties) {
        this.all = all;
        this.none = none;
        this.county = county;
        this.subCounties = subCounties;
    }

    public static AreaScope of(User u) {
        if (u == null) return ALL;
        if (u.getRole() == Role.COUNTY_ADMIN) {
            String c = u.getAssignedCounty();
            if (c == null || c.isBlank() || !Locations.isCounty(c)) return NONE;
            return new AreaScope(false, false, c, Set.of());        // the whole county, never a part of it
        }
        if (u.getRole() != Role.OFFICER) return ALL;
        String c = u.getAssignedCounty();
        if (c == null || c.isBlank()) return ALL;
        Set<String> subs = u.getAssignedSubCounties() == null ? Set.of() : new TreeSet<>(u.getAssignedSubCounties());
        return new AreaScope(false, false, c, subs);
    }

    public boolean all() { return all; }
    /** True for a county admin with no county: nothing is inside this area. */
    public boolean none() { return none; }
    public String county() { return county; }
    public Set<String> subCounties() { return subCounties; }

    /** Is a farmer (or group, or service) in this sub-county inside the area? */
    public boolean allows(String subCounty) {
        if (all) return true;
        if (none) return false;
        if (subCounty == null || subCounty.isBlank()) return false;
        if (!subCounties.isEmpty()) return subCounties.contains(subCounty);
        return county.equals(Locations.countyOf(subCounty));
    }

    /** Is this county inside the area? (A county admin: only their own; an officer with a sub-county list: the county they sit in.) */
    public boolean allowsCounty(String c) {
        if (all) return true;
        if (none || c == null || c.isBlank()) return false;
        return c.equals(county);
    }

    public String label() {
        if (all) return "All counties";
        if (none) return "No county assigned yet";
        if (subCounties.isEmpty()) return county + " County";
        return county + " County: " + String.join(", ", subCounties);
    }
}
