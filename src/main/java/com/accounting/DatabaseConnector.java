package com.accounting;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnector {
    private final Connection connection;

    public DatabaseConnector(String databasePath) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + databasePath);
        connection.createStatement().execute("PRAGMA foreign_keys = ON");
    }

    public Connection getConnection() { return connection; }
    public void close() throws SQLException { connection.close(); }
}
