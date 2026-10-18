package com.farmconnect.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Hibernate "ddl-auto=update" never widens an existing column, so databases created before the longer
 * product description (5000 chars) keep the old 1000 limit. This widens it once; failures are ignored.
 */
@Component
@Order(0)
@RequiredArgsConstructor
public class SchemaPatcher implements CommandLineRunner {
    private final JdbcTemplate jdbc;

    @Override
    public void run(String... args) {
        String[] attempts = {
                "ALTER TABLE products ALTER COLUMN description VARCHAR(5000)",          // H2
                "ALTER TABLE products ALTER COLUMN description TYPE VARCHAR(5000)"      // PostgreSQL
        };
        for (String sql : attempts) {
            try { jdbc.execute(sql); break; } catch (Exception ignored) { }
        }
        allowCountyAdminRole();
    }

    /**
     * Databases created before the COUNTY_ADMIN role carry an ENUM type or CHECK constraint that only lists the old roles
     * (ADMIN, BUYER, FARMER, OFFICER), so inserting a county admin fails. Drop that one constraint; the
     * application validates roles itself. Safe to run on every start.
     */
    private void allowCountyAdminRole() {
        // H2 + Hibernate 6 create the role column as a native ENUM('ADMIN','BUYER','FARMER','OFFICER'); turn it into plain text
        String[] widen = {
                "ALTER TABLE users ALTER COLUMN role SET DATA TYPE VARCHAR(30)",     // H2
                "ALTER TABLE users ALTER COLUMN role TYPE VARCHAR(30)"               // PostgreSQL
        };
        for (String sql : widen) {
            try { jdbc.execute(sql); break; } catch (Exception ignored) { }
        }
        try {
            List<String> names = jdbc.queryForList(
                    "SELECT tc.CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc "
                            + "JOIN INFORMATION_SCHEMA.CHECK_CONSTRAINTS cc ON cc.CONSTRAINT_NAME = tc.CONSTRAINT_NAME "
                            + "WHERE UPPER(tc.TABLE_NAME) = 'USERS' AND tc.CONSTRAINT_TYPE = 'CHECK' "
                            + "AND cc.CHECK_CLAUSE LIKE '%OFFICER%' AND cc.CHECK_CLAUSE NOT LIKE '%COUNTY_ADMIN%'",
                    String.class);
            for (String n : names) {
                try { jdbc.execute("ALTER TABLE users DROP CONSTRAINT \"" + n.replace("\"", "") + "\""); } catch (Exception ignored) { }
            }
        } catch (Exception ignored) { }
        try { jdbc.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check"); } catch (Exception ignored) { }   // PostgreSQL name
    }
}
