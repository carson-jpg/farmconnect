package com.farmconnect.service;

import com.farmconnect.model.Role;
import com.farmconnect.model.User;
import com.farmconnect.security.Staff;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The county limits behind the super admin / county admin split. */
class AreaScopeTest {
    private static User user(Role role, String county) {
        User u = new User();
        u.setRole(role);
        u.setAssignedCounty(county);
        return u;
    }

    @Test
    void superAdminSeesEverything() {
        AreaScope s = AreaScope.of(user(Role.ADMIN, null));
        assertTrue(s.all());
        assertTrue(s.allows("Endebess"));
        assertTrue(s.allowsCounty("Bungoma"));
    }

    @Test
    void countyAdminSeesOnlyTheirOwnWholeCounty() {
        AreaScope s = AreaScope.of(user(Role.COUNTY_ADMIN, "Trans Nzoia"));
        assertFalse(s.all());
        assertTrue(s.allows("Endebess"));          // a Trans Nzoia sub-county
        assertFalse(s.allows("Kimilili"));         // a Bungoma sub-county
        assertFalse(s.allows(null));
        assertTrue(s.allowsCounty("Trans Nzoia"));
        assertFalse(s.allowsCounty("Bungoma"));
    }

    @Test
    void countyAdminWithoutACountySeesNothing() {
        for (String county : new String[]{null, "", "  ", "Atlantis"}) {
            AreaScope s = AreaScope.of(user(Role.COUNTY_ADMIN, county));
            assertTrue(s.none(), "county=" + county);
            assertFalse(s.all());
            assertFalse(s.allows("Endebess"));
            assertFalse(s.allowsCounty("Trans Nzoia"));
        }
    }

    @Test
    void unassignedOfficerKeepsSeeingEverythingAsBefore() {
        assertTrue(AreaScope.of(user(Role.OFFICER, null)).all());
    }

    @Test
    void onlyTheSuperAdminIsSuper() {
        assertTrue(Staff.isSuper(user(Role.ADMIN, null)));
        assertFalse(Staff.isSuper(user(Role.COUNTY_ADMIN, "Bungoma")));
        assertTrue(Staff.isAdmin(user(Role.COUNTY_ADMIN, "Bungoma")));
        assertTrue(Staff.is(user(Role.OFFICER, null)));
        assertFalse(Staff.is(user(Role.FARMER, null)));
    }
}
