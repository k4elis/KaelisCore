package fr.kaelis.kaeliscore.player;

import fr.kaelis.kaeliscore.KaelisCore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player data caching and persistence
 */
public class PlayerDataManager {

    private final KaelisCore plugin;
    private final Map<UUID, KaelisPlayer> playerCache = new ConcurrentHashMap<>();

    public PlayerDataManager(KaelisCore plugin) {
        this.plugin = plugin;
    }

    /**
     * Get or load player data
     */
    public KaelisPlayer getPlayer(UUID uuid) {
        return playerCache.computeIfAbsent(uuid, this::loadPlayer);
    }

    /**
     * Get or load player data by player
     */
    public KaelisPlayer getPlayer(Player player) {
        return getPlayer(player.getUniqueId());
    }

    /**
     * Check if player data is cached
     */
    public boolean isCached(UUID uuid) {
        return playerCache.containsKey(uuid);
    }

    /**
     * Load player data from database
     */
    private KaelisPlayer loadPlayer(UUID uuid) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM kc_players WHERE uuid = ?")) {
            
            stmt.setString(1, uuid.toString());
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                KaelisPlayer kaelisPlayer = new KaelisPlayer(uuid);
                kaelisPlayer.setUsername(rs.getString("username"));
                kaelisPlayer.setBalance(rs.getDouble("balance"));
                kaelisPlayer.setFirstJoin(rs.getLong("first_join"));
                kaelisPlayer.setLastJoin(rs.getLong("last_join"));
                kaelisPlayer.setPlayTime(rs.getLong("play_time"));
                kaelisPlayer.loadFromJson(rs.getString("data"));
                return kaelisPlayer;
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player data for " + uuid, e);
        }
        
        // Create new player data
        return new KaelisPlayer(uuid);
    }

    /**
     * Save player data to database
     */
    public void savePlayer(KaelisPlayer player) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("""
                     INSERT INTO kc_players (uuid, username, balance, first_join, last_join, play_time, data)
                     VALUES (?, ?, ?, ?, ?, ?, ?)
                     ON CONFLICT(uuid) DO UPDATE SET
                         username = excluded.username,
                         balance = excluded.balance,
                         last_join = excluded.last_join,
                         play_time = excluded.play_time,
                         data = excluded.data
                     """)) {
                
                stmt.setString(1, player.getUuid().toString());
                stmt.setString(2, player.getUsername());
                stmt.setDouble(3, player.getBalance());
                stmt.setLong(4, player.getFirstJoin());
                stmt.setLong(5, player.getLastJoin());
                stmt.setLong(6, player.getPlayTime());
                stmt.setString(7, player.toJson());
                stmt.executeUpdate();
            } catch (SQLException e) {
                // Try MySQL syntax
                savePlayerMySQL(player);
            }
        });
    }

    private void savePlayerMySQL(KaelisPlayer player) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("""
                 INSERT INTO kc_players (uuid, username, balance, first_join, last_join, play_time, data)
                 VALUES (?, ?, ?, ?, ?, ?, ?)
                 ON DUPLICATE KEY UPDATE
                     username = VALUES(username),
                     balance = VALUES(balance),
                     last_join = VALUES(last_join),
                     play_time = VALUES(play_time),
                     data = VALUES(data)
                 """)) {
            
            stmt.setString(1, player.getUuid().toString());
            stmt.setString(2, player.getUsername());
            stmt.setDouble(3, player.getBalance());
            stmt.setLong(4, player.getFirstJoin());
            stmt.setLong(5, player.getLastJoin());
            stmt.setLong(6, player.getPlayTime());
            stmt.setString(7, player.toJson());
            stmt.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player data for " + player.getUuid(), e);
        }
    }

    /**
     * Save all cached players
     */
    public void saveAll() {
        for (KaelisPlayer player : playerCache.values()) {
            savePlayer(player);
        }
    }

    /**
     * Unload player data
     */
    public void unloadPlayer(UUID uuid) {
        KaelisPlayer player = playerCache.remove(uuid);
        if (player != null) {
            savePlayer(player);
        }
    }

    /**
     * Handle player join
     */
    public void handleJoin(Player player) {
        KaelisPlayer kaelisPlayer = getPlayer(player.getUniqueId());
        kaelisPlayer.setUsername(player.getName());
        
        if (kaelisPlayer.getFirstJoin() == 0) {
            kaelisPlayer.setFirstJoin(System.currentTimeMillis());
            // Give starting balance
            double startingBalance = plugin.getConfigManager().getConfig("economy")
                    .getDouble("starting-balance", 100.0);
            kaelisPlayer.setBalance(startingBalance);
        }
        
        kaelisPlayer.setLastJoin(System.currentTimeMillis());
        kaelisPlayer.setSessionStart(System.currentTimeMillis());
    }

    /**
     * Handle player quit
     */
    public void handleQuit(Player player) {
        KaelisPlayer kaelisPlayer = playerCache.get(player.getUniqueId());
        if (kaelisPlayer != null) {
            // Update play time
            long sessionTime = System.currentTimeMillis() - kaelisPlayer.getSessionStart();
            kaelisPlayer.addPlayTime(sessionTime);
            
            // Save and unload
            savePlayer(kaelisPlayer);
            playerCache.remove(player.getUniqueId());
        }
    }

    public Map<UUID, KaelisPlayer> getPlayerCache() {
        return playerCache;
    }
}
