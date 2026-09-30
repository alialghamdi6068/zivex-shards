package me.voidflame.zivexshards;

import java.io.File;
import java.sql.*;
import java.util.UUID;

final class ShardDatabase {
    private final ZivexShardsPlugin plugin;
    private final File file;
    private Connection connection;

    ShardDatabase(ZivexShardsPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), plugin.getConfig().getString("settings.database-file", "../Zivex/database.db"));
        open();
    }

    private synchronized void open() {
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new SQLException("Could not create database directory");
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + file.getAbsolutePath());

            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA journal_mode=DELETE");
                statement.execute("PRAGMA foreign_keys=ON");
                statement.execute("PRAGMA busy_timeout=5000");
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS player_shards (
                        uuid TEXT PRIMARY KEY,
                        balance INTEGER NOT NULL DEFAULT 0 CHECK(balance >= 0)
                    )
                    """);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not open Shards database", ex);
        }
    }

    synchronized void initialize() {
        if (connection == null) open();
    }

    synchronized long getBalance(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT balance FROM player_shards WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Math.max(0L, rs.getLong("balance")) : 0L;
            }
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to read Shards balance: " + ex.getMessage());
            return -1L;
        }
    }

    synchronized boolean setBalance(UUID uuid, long amount) {
        if (amount < 0) return false;
        try (PreparedStatement ps = connection.prepareStatement("""
                INSERT INTO player_shards(uuid, balance) VALUES(?, ?)
                ON CONFLICT(uuid) DO UPDATE SET balance = excluded.balance
                """)) {
            ps.setString(1, uuid.toString());
            ps.setLong(2, amount);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to save Shards balance: " + ex.getMessage());
            return false;
        }
    }

    synchronized boolean withdraw(UUID uuid, long amount) {
        if (amount < 0) return false;
        try (PreparedStatement ps = connection.prepareStatement("""
                UPDATE player_shards
                SET balance = balance - ?
                WHERE uuid = ? AND balance >= ?
                """)) {
            ps.setLong(1, amount);
            ps.setString(2, uuid.toString());
            ps.setLong(3, amount);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            plugin.getLogger().severe("Failed to withdraw Shards: " + ex.getMessage());
            return false;
        }
    }

    synchronized void close() {
        try {
            if (connection != null && !connection.isClosed()) connection.close();
        } catch (SQLException ex) {
            plugin.getLogger().warning("Failed to close Shards database: " + ex.getMessage());
        }
    }
}
