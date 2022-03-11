package com.sequenceiq.cloudbreak.util;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.function.Supplier;

import org.hibernate.Hibernate;
import org.postgresql.Driver;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

public class DatabaseUtil {
    public static final String DEFAULT_SCHEMA_NAME = "public";

    public static final String UNINITIALIZED_TO_STRING = "<uninitialized>";

    private DatabaseUtil() {
    }

    public static void createSchemaIfNeeded(String dbType, String dbAddress, String dbName, String dbUser, String dbPassword, String dbSchema)
            throws SQLException {
        if (!DEFAULT_SCHEMA_NAME.equals(dbSchema)) {
            SimpleDriverDataSource ds = new SimpleDriverDataSource();
            ds.setDriverClass(Driver.class);
            ds.setUrl(String.format("jdbc:%s://%s/%s", dbType, dbAddress, dbName));
            try (Connection conn = ds.getConnection(dbUser, dbPassword); Statement statement = conn.createStatement()) {
                statement.execute("CREATE SCHEMA IF NOT EXISTS " + dbSchema);
            }
        }
    }

    public static boolean isLazyLoadInitialized(Object o) {
        return Hibernate.isInitialized(o);
    }

    public static String lazyLoadSafeToString(Object o) {
        return lazyLoadSafeToString(o, () -> o);
    }

    public static String lazyLoadSafeToString(Object o, Supplier<Object> toStringSupplier) {
        if (o == null) {
            return null;
        } else if (isLazyLoadInitialized(o)) {
            return String.valueOf(toStringSupplier.get());
        } else {
            return UNINITIALIZED_TO_STRING;
        }
    }
}
