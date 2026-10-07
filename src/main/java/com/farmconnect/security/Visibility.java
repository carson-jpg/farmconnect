package com.farmconnect.security;

import com.farmconnect.model.Farm;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;

/** Farmers' farms and products are public ONLY after an admin/officer verified the farmer. */
public final class Visibility {
    private Visibility() {}

    private static boolean reviewer(User v) {
        return v != null && (v.getRole() == Role.ADMIN || v.getRole() == Role.OFFICER);
    }

    /** A product is public only if its farmer is verified AND an admin has not taken it down. */
    public static boolean canSee(User viewer, com.farmconnect.model.Product p) {
        if (reviewer(viewer)) return true;
        if (viewer != null && p.getFarm().getOwner().getId().equals(viewer.getId())) return true;
        return p.isActive() && p.getFarm().getOwner().isVerified();
    }

    public static boolean canSee(User viewer, Farm farm) {
        if (farm.getOwner().isVerified()) return true;
        if (viewer == null) return false;
        return viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.OFFICER
                || farm.getOwner().getId().equals(viewer.getId());
    }
}
