package fr.kaelis.kaeliscore.modules.stats;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import fr.kaelis.kaeliscore.player.KaelisPlayer;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Statistics module for tracking player stats
 */
public class StatsModule extends AbstractModule implements Listener {

    // Common stat keys
    public static final String KILLS = "kills";
    public static final String DEATHS = "deaths";
    public static final String MOB_KILLS = "mob_kills";
    public static final String BLOCKS_BROKEN = "blocks_broken";
    public static final String BLOCKS_PLACED = "blocks_placed";
    public static final String DISTANCE_WALKED = "distance_walked";
    public static final String ITEMS_CRAFTED = "items_crafted";
    public static final String FISH_CAUGHT = "fish_caught";
    public static final String TRADES = "trades";
    public static final String LOGINS = "logins";

    public StatsModule(KaelisCore plugin) {
        super(plugin, "Stats", "stats");
    }

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start periodic stat sync
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::syncAllPlayerStats, 6000L, 6000L); // Every 5 minutes
    }

    @Override
    public void onDisable() {
        // Sync all stats before shutdown
        syncAllPlayerStats();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(event.getPlayer());
        kPlayer.addStat(LOGINS, 1);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        KaelisPlayer kVictim = plugin.getPlayerDataManager().getPlayer(victim);
        kVictim.addStat(DEATHS, 1);

        Player killer = victim.getKiller();
        if (killer != null) {
            KaelisPlayer kKiller = plugin.getPlayerDataManager().getPlayer(killer);
            kKiller.addStat(KILLS, 1);
        }
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity() instanceof Player) return;
        
        Player killer = event.getEntity().getKiller();
        if (killer != null) {
            KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(killer);
            kPlayer.addStat(MOB_KILLS, 1);
        }
    }

    /**
     * Sync all online players' vanilla stats
     */
    private void syncAllPlayerStats() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            syncVanillaStats(player);
        }
    }

    /**
     * Sync vanilla Minecraft stats to our system
     */
    public void syncVanillaStats(Player player) {
        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(player);
        
        // Sync some vanilla stats
        kPlayer.setStat(BLOCKS_BROKEN, player.getStatistic(Statistic.MINE_BLOCK));
        kPlayer.setStat(BLOCKS_PLACED, player.getStatistic(Statistic.USE_ITEM));
        kPlayer.setStat(DISTANCE_WALKED, player.getStatistic(Statistic.WALK_ONE_CM) / 100.0);
        kPlayer.setStat(FISH_CAUGHT, player.getStatistic(Statistic.FISH_CAUGHT));
        kPlayer.setStat(TRADES, player.getStatistic(Statistic.TRADED_WITH_VILLAGER));
    }

    /**
     * Get a player's stat
     */
    public double getStat(Player player, String key) {
        return plugin.getPlayerDataManager().getPlayer(player).getStat(key);
    }

    /**
     * Add to a player's stat
     */
    public void addStat(Player player, String key, double value) {
        plugin.getPlayerDataManager().getPlayer(player).addStat(key, value);
    }

    /**
     * Set a player's stat
     */
    public void setStat(Player player, String key, double value) {
        plugin.getPlayerDataManager().getPlayer(player).setStat(key, value);
    }

    /**
     * Get leaderboard for a stat
     */
    public CompletableFuture<List<LeaderboardEntry>> getLeaderboard(String statKey, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<LeaderboardEntry> entries = new ArrayList<>();
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT p.username, s.stat_value FROM kc_statistics s " +
                     "JOIN kc_players p ON s.uuid = p.uuid " +
                     "WHERE s.stat_key = ? ORDER BY s.stat_value DESC LIMIT ?")) {
                
                stmt.setString(1, statKey);
                stmt.setInt(2, limit);
                ResultSet rs = stmt.executeQuery();
                
                int rank = 1;
                while (rs.next()) {
                    entries.add(new LeaderboardEntry(
                        rank++,
                        rs.getString("username"),
                        rs.getDouble("stat_value")
                    ));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to get leaderboard for " + statKey, e);
            }
            
            return entries;
        });
    }

    /**
     * Save all stats to database
     */
    public void saveStats(Player player) {
        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(player);
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = plugin.getDatabaseManager().isMySQL()
                ? "INSERT INTO kc_statistics (uuid, stat_key, stat_value) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE stat_value = VALUES(stat_value)"
                : "INSERT INTO kc_statistics (uuid, stat_key, stat_value) VALUES (?, ?, ?) ON CONFLICT(uuid, stat_key) DO UPDATE SET stat_value = excluded.stat_value";
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                for (Map.Entry<String, Double> entry : kPlayer.getStatistics().entrySet()) {
                    stmt.setString(1, player.getUniqueId().toString());
                    stmt.setString(2, entry.getKey());
                    stmt.setDouble(3, entry.getValue());
                    stmt.addBatch();
                }
                
                stmt.executeBatch();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to save stats for " + player.getName(), e);
            }
        });
    }

    /**
     * Get K/D ratio
     */
    public double getKDRatio(Player player) {
        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(player);
        double kills = kPlayer.getStat(KILLS);
        double deaths = kPlayer.getStat(DEATHS);
        return deaths == 0 ? kills : kills / deaths;
    }

    public record LeaderboardEntry(int rank, String playerName, double value) {}
}
