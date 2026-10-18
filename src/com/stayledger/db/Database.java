package com.stayledger.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Database {
    // Replace these defaults with the Oracle schema account used for HotelBilling.
    // Environment variables or Java system properties with the same names override them.
    private static final String DEFAULT_URL = "jdbc:oracle:thin:@localhost:1521:xe";
    private static final String DEFAULT_USER = "hotelbilling";
    private static final String DEFAULT_PASSWORD = "hotelbilling";

    private Database() { }

    public static Connection open() throws SQLException {
        String url = setting("HOTEL_DB_URL", DEFAULT_URL);
        String user = setting("HOTEL_DB_USER", DEFAULT_USER);
        String password = setting("HOTEL_DB_PASSWORD", DEFAULT_PASSWORD);
        try {
            Class.forName("oracle.jdbc.driver.OracleDriver");
        } catch (ClassNotFoundException ex) {
            throw new SQLException("Oracle JDBC driver is missing from WEB-INF/lib.", ex);
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static String setting(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.trim().isEmpty()) value = System.getProperty(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }
}
