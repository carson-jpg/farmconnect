package com.farmconnect.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

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
            try { jdbc.execute(sql); return; } catch (Exception ignored) { }
        }
    }
}
