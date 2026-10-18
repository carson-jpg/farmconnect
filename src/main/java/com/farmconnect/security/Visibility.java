package com.farmconnect.security;

import com.farmconnect.model.Farm;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.service.AreaScope;

/** Farmers' farms and products are public ONLY after an admin/officer verified the farmer. */
public final class Visibility {
    private Visibility() {}

    /** May this viewer see an unverified farmer's farm? Super admin and officers: any; a county admin: only in their county. */
    private static boolean reviewer(User v, Farm farm) {
        if (v == null) return false;
        if (v.getRole() == Role.ADMIN || v.getRole() == Role.OFFICER) return true;
        return v.getRole() == Role.COUNTY_ADMIN && AreaScope.of(v).allows(farm.getSubCounty());
    }

    /** A product is public only if its farmer is verified AND an admin has not taken it down. */
    public static boolean canSee(User viewer, com.farmconnect.model.Product p) {
        if (reviewer(viewer, p.getFarm())) return true;
        if (viewer != null && p.getFarm().getOwner().getId().equals(viewer.getId())) return true;
        return p.isActive() && p.getFarm().getOwner().isVerified();
    }

    public static boolean canSee(User viewer, Farm farm) {
        if (farm.getOwner().isVerified()) return true;
        if (viewer == null) return false;
        return reviewer(viewer, farm) || farm.getOwner().getId().equals(viewer.getId());
    }
}
