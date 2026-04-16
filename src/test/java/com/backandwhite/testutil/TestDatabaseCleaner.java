package com.backandwhite.testutil;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public class TestDatabaseCleaner {

    private final JdbcTemplate jdbcTemplate;

    public TestDatabaseCleaner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void truncateAllTables() {
        List<String> tables = jdbcTemplate.queryForList("SELECT tablename FROM pg_tables WHERE schemaname = 'public'",
                String.class);
        jdbcTemplate.execute("SET session_replication_role = 'replica';");
        for (String table : tables) {
            if (table == null) {
                continue;
            }
            String lower = table.toLowerCase();
            if (lower.startsWith("databasechangelog")) {
                continue;
            }
            jdbcTemplate.execute("TRUNCATE TABLE public.\"" + table + "\" CASCADE;");
        }
        jdbcTemplate.execute("SET session_replication_role = 'origin';");
    }
}
