package com.dao;

/** Command-line database initializer for local setup and verification. */
public final class DatabaseSetup {

    private DatabaseSetup() {
    }

    public static void main(String[] args) throws Exception {
        new AppDao().initialize();
        System.out.println("Campus Event database tables and organizer seed are ready.");
    }
}
