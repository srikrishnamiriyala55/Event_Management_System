package com.dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Dao {

    private Dao() {
    }

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new ExceptionInInitializerError("MySQL JDBC driver is missing from WEB-INF/lib: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                env(
                        "DB_URL",
                        "jdbc:mysql://localhost:3306/event"
                                + "?createDatabaseIfNotExist=true"
                                + "&serverTimezone=Asia/Kolkata"),
                env("DB_USER", "root"),
                env("DB_PASSWORD", "root123"));
    }

    public static String env(String name, String fallback) {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? fallback : value;
    }
}
