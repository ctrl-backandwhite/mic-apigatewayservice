package com.backandwhite.testutil;

import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class DatabaseCleanerExtension implements BeforeEachCallback {

    @Override
    public void beforeEach(ExtensionContext context) {
        ApplicationContext applicationContext = SpringExtension.getApplicationContext(context);
        String[] beanNames = applicationContext.getBeanNamesForType(JdbcTemplate.class);
        if (beanNames.length > 0) {
            JdbcTemplate jdbcTemplate = applicationContext.getBean(JdbcTemplate.class);
            new TestDatabaseCleaner(jdbcTemplate).truncateAllTables();
        }
    }
}
