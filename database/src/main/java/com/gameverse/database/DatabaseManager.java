package com.gameverse.database;

/**
 * Manager for database operations.
 * Stub implementation — persistence will be added in Phase 5.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private boolean connected;

    private DatabaseManager() {
        this.connected = false;
    }

    /**
     * Get the singleton instance of DatabaseManager
     * @return the DatabaseManager instance
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Connect to the database
     * @return true if connection was successful
     */
    public boolean connect() {
        // Stub — will use SQLite in Phase 5
        connected = true;
        return true;
    }

    /**
     * Disconnect from the database
     */
    public void disconnect() {
        connected = false;
    }

    /**
     * Check if connected to the database
     * @return true if connected
     */
    public boolean isConnected() {
        return connected;
    }
}
