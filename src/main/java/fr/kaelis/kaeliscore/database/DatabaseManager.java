package fr.kaelis.kaeliscore.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import fr.kaelis.kaeliscore.KaelisCore;
import org.bukkit.configuration.file.FileConfiguration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Database manager supporting MySQL and SQLite
 */
public class DatabaseManager {

    private final KaelisCore plugin;
    private HikariDataSource dataSource;
    private DatabaseType type;

    public enum DatabaseType {
        MYSQL,
        SQLITE
    }

    public DatabaseManager(KaelisCore plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        FileConfiguration config = plugin.getConfigManager().getConfig("database");
        String typeStr = config.getString("type", "sqlite").toLowerCase();

        HikariConfig hikariConfig = new HikariConfig();

        if (typeStr.equals("mysql") || typeStr.equals("mariadb")) {
            type = DatabaseType.MYSQL;
            String host = config.getString("mysql.host", "localhost");
            int port = config.getInt("mysql.port", 3306);
            String database = config.getString("mysql.database", "kaeliscore");
            String username = config.getString("mysql.username", "root");
            String password = config.getString("mysql.password", "");
            boolean ssl = config.getBoolean("mysql.ssl", false);

            hikariConfig.setJdbcUrl("jdbc:mysql://" + host + ":" + port + "/" + database + "?useSSL=" + ssl + "&autoReconnect=true");
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
            
            plugin.getLogger().info("Using MySQL/MariaDB database");
        } else {
            type = DatabaseType.SQLITE;
            hikariConfig.setJdbcUrl("jdbc:sqlite:" + plugin.getDataFolder().getAbsolutePath() + "/data.db");
            hikariConfig.setDriverClassName("org.sqlite.JDBC");
            
            plugin.getLogger().info("Using SQLite database");
        }

        // HikariCP settings
        hikariConfig.setPoolName("KaelisCore-Pool");
        hikariConfig.setMaximumPoolSize(config.getInt("pool.maximum-size", 10));
        hikariConfig.setMinimumIdle(config.getInt("pool.minimum-idle", 2));
        hikariConfig.setConnectionTimeout(config.getLong("pool.connection-timeout", 30000));
        hikariConfig.setIdleTimeout(config.getLong("pool.idle-timeout", 600000));
        hikariConfig.setMaxLifetime(config.getLong("pool.max-lifetime", 1800000));

        try {
            dataSource = new HikariDataSource(hikariConfig);
            createTables();
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to connect to database", e);
            return false;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public DatabaseType getType() {
        return type;
    }

    public boolean isMySQL() {
        return type == DatabaseType.MYSQL;
    }

    private void createTables() {
        String autoIncrement = isMySQL() ? "AUTO_INCREMENT" : "AUTOINCREMENT";
        String textType = isMySQL() ? "TEXT" : "TEXT";
        
        // Players table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_players (
                id INTEGER PRIMARY KEY %s,
                uuid VARCHAR(36) NOT NULL UNIQUE,
                username VARCHAR(16) NOT NULL,
                balance DOUBLE DEFAULT 0,
                first_join BIGINT NOT NULL,
                last_join BIGINT NOT NULL,
                play_time BIGINT DEFAULT 0,
                data %s
            )
            """.formatted(autoIncrement, textType));

        // Homes table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_homes (
                id INTEGER PRIMARY KEY %s,
                uuid VARCHAR(36) NOT NULL,
                name VARCHAR(32) NOT NULL,
                world VARCHAR(64) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                yaw FLOAT NOT NULL,
                pitch FLOAT NOT NULL,
                UNIQUE(uuid, name)
            )
            """.formatted(autoIncrement));

        // Warps table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_warps (
                id INTEGER PRIMARY KEY %s,
                name VARCHAR(32) NOT NULL UNIQUE,
                world VARCHAR(64) NOT NULL,
                x DOUBLE NOT NULL,
                y DOUBLE NOT NULL,
                z DOUBLE NOT NULL,
                yaw FLOAT NOT NULL,
                pitch FLOAT NOT NULL,
                permission VARCHAR(64),
                category VARCHAR(32)
            )
            """.formatted(autoIncrement));

        // Claims table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_claims (
                id INTEGER PRIMARY KEY %s,
                uuid VARCHAR(36) NOT NULL,
                world VARCHAR(64) NOT NULL,
                min_x INT NOT NULL,
                min_z INT NOT NULL,
                max_x INT NOT NULL,
                max_z INT NOT NULL,
                created BIGINT NOT NULL,
                flags %s
            )
            """.formatted(autoIncrement, textType));

        // Claim members table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_claim_members (
                claim_id INTEGER NOT NULL,
                uuid VARCHAR(36) NOT NULL,
                role VARCHAR(16) NOT NULL,
                PRIMARY KEY (claim_id, uuid)
            )
            """);

        // Kits table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_kits (
                id INTEGER PRIMARY KEY %s,
                name VARCHAR(32) NOT NULL UNIQUE,
                permission VARCHAR(64),
                cooldown INT DEFAULT 0,
                items %s NOT NULL
            )
            """.formatted(autoIncrement, textType));

        // Kit cooldowns table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_kit_cooldowns (
                uuid VARCHAR(36) NOT NULL,
                kit VARCHAR(32) NOT NULL,
                last_use BIGINT NOT NULL,
                PRIMARY KEY (uuid, kit)
            )
            """);

        // Quests table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_quests (
                id INTEGER PRIMARY KEY %s,
                quest_id VARCHAR(32) NOT NULL UNIQUE,
                name VARCHAR(64) NOT NULL,
                description %s,
                type VARCHAR(32) NOT NULL,
                target %s NOT NULL,
                amount INT NOT NULL,
                rewards %s,
                repeatable BOOLEAN DEFAULT FALSE
            )
            """.formatted(autoIncrement, textType, textType, textType));

        // Player quests progress
        execute("""
            CREATE TABLE IF NOT EXISTS kc_player_quests (
                uuid VARCHAR(36) NOT NULL,
                quest_id VARCHAR(32) NOT NULL,
                progress INT DEFAULT 0,
                completed BOOLEAN DEFAULT FALSE,
                completed_at BIGINT,
                PRIMARY KEY (uuid, quest_id)
            )
            """);

        // Statistics table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_statistics (
                uuid VARCHAR(36) NOT NULL,
                stat_key VARCHAR(64) NOT NULL,
                stat_value DOUBLE DEFAULT 0,
                PRIMARY KEY (uuid, stat_key)
            )
            """);

        // Events table
        execute("""
            CREATE TABLE IF NOT EXISTS kc_events (
                id INTEGER PRIMARY KEY %s,
                event_id VARCHAR(32) NOT NULL UNIQUE,
                type VARCHAR(32) NOT NULL,
                data %s,
                start_time BIGINT,
                end_time BIGINT,
                active BOOLEAN DEFAULT FALSE
            )
            """.formatted(autoIncrement, textType));

        // Transactions table (economy history)
        execute("""
            CREATE TABLE IF NOT EXISTS kc_transactions (
                id INTEGER PRIMARY KEY %s,
                uuid VARCHAR(36) NOT NULL,
                type VARCHAR(32) NOT NULL,
                amount DOUBLE NOT NULL,
                balance_after DOUBLE NOT NULL,
                description %s,
                timestamp BIGINT NOT NULL
            )
            """.formatted(autoIncrement, textType));

        plugin.getLogger().info("Database tables created successfully");
    }

    // Sync execute
    public void execute(String sql) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to execute SQL: " + sql, e);
        }
    }

    // Async execute
    public CompletableFuture<Void> executeAsync(String sql) {
        return CompletableFuture.runAsync(() -> execute(sql));
    }

    // Sync query
    public <T> T query(String sql, ResultHandler<T> handler) {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return handler.handle(rs);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to query SQL: " + sql, e);
            return null;
        }
    }

    // Async query
    public <T> CompletableFuture<T> queryAsync(String sql, ResultHandler<T> handler) {
        return CompletableFuture.supplyAsync(() -> query(sql, handler));
    }

    @FunctionalInterface
    public interface ResultHandler<T> {
        T handle(ResultSet rs) throws SQLException;
    }
}
