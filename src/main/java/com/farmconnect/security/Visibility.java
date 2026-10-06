package com.farmconnect.security;

import com.farmconnect.model.Farm;
import com.farmconnect.model.Role;
import com.farmconnect.model.User;

/** Farmers' farms and products are public ONLY after an admin/officer verified the farmer. */
public final class Visibility {
    private Visibility() {}

    public static boolean canSee(User viewer, Farm farm) {
        if (farm.getOwner().isVerified()) return true;
        if (viewer == null) return false;
        return viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.OFFICER
                || farm.getOwner().getId().equals(viewer.getId());
    }
}
