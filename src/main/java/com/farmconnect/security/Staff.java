package com.farmconnect.security;

import com.farmconnect.model.Role;
import com.farmconnect.model.User;

/** Who counts as FarmConnect staff. One place, so every controller agrees. */
public final class Staff {
    private Staff() {}

    /** Super admin, county admin or officer. */
    public static boolean is(User u) {
        return u != null && (u.getRole() == Role.ADMIN || u.getRole() == Role.COUNTY_ADMIN || u.getRole() == Role.OFFICER);
    }

    /** The super administrator: the whole platform, every county. */
    public static boolean isSuper(User u) { return u != null && u.getRole() == Role.ADMIN; }

    public static boolean isCountyAdmin(User u) { return u != null && u.getRole() == Role.COUNTY_ADMIN; }

    /** Super admin or county admin: the "administrator" tools (users, orders, products). */
    public static boolean isAdmin(User u) { return isSuper(u) || isCountyAdmin(u); }
}
