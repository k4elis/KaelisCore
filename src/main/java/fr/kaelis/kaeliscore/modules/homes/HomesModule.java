package fr.kaelis.kaeliscore.modules.homes;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Homes module for player teleportation
 */
public class HomesModule extends AbstractModule {

    private int teleportDelay;
    private int teleportCooldown;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> pendingTeleports = new HashMap<>();

    public HomesModule(KaelisCore plugin) {
        super(plugin, "Homes", "homes");
    }

    @Override
    public void onEnable() {
        teleportDelay = getConfig().getInt("teleport-delay", 3);
        teleportCooldown = getConfig().getInt("teleport-cooldown", 5);
    }

    @Override
    public void onDisable() {
        // Cancel pending teleports
        for (int taskId : pendingTeleports.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        pendingTeleports.clear();
    }

    /**
     * Get the maximum number of homes a player can have
     */
    public int getMaxHomes(Player player) {
        if (player.hasPermission("kaeliscore.homes.limit.unlimited")) {
            return Integer.MAX_VALUE;
        }
        
        int max = getConfig().getInt("default-limit", 1);
        
        // Check for limit permissions (highest takes priority)
        for (int i = 100; i >= 1; i--) {
            if (player.hasPermission("kaeliscore.homes.limit." + i)) {
                return i;
            }
        }
        
        return max;
    }

    /**
     * Get all homes for a player
     */
    public CompletableFuture<Map<String, Location>> getHomes(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, Location> homes = new LinkedHashMap<>();
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT name, world, x, y, z, yaw, pitch FROM kc_homes WHERE uuid = ?")) {
                
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    String name = rs.getString("name");
                    String worldName = rs.getString("world");
                    var world = Bukkit.getWorld(worldName);
                    
                    if (world != null) {
                        Location loc = new Location(
                            world,
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                        );
                        homes.put(name, loc);
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get homes for " + uuid, e);
            }
            
            return homes;
        });
    }

    /**
     * Get a specific home
     */
    public CompletableFuture<Location> getHome(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT world, x, y, z, yaw, pitch FROM kc_homes WHERE uuid = ? AND name = ?")) {
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name.toLowerCase());
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    var world = Bukkit.getWorld(rs.getString("world"));
                    if (world != null) {
                        return new Location(
                            world,
                            rs.getDouble("x"),
                            rs.getDouble("y"),
                            rs.getDouble("z"),
                            rs.getFloat("yaw"),
                            rs.getFloat("pitch")
                        );
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to get home " + name + " for " + uuid, e);
            }
            return null;
        });
    }

    /**
     * Set a home for a player
     */
    public CompletableFuture<Boolean> setHome(Player player, String name) {
        return CompletableFuture.supplyAsync(() -> {
            UUID uuid = player.getUniqueId();
            Location loc = player.getLocation();
            
            // Check home limit
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Count existing homes
                try (PreparedStatement countStmt = conn.prepareStatement(
                        "SELECT COUNT(*) FROM kc_homes WHERE uuid = ?")) {
                    countStmt.setString(1, uuid.toString());
                    ResultSet rs = countStmt.executeQuery();
                    if (rs.next()) {
                        int currentHomes = rs.getInt(1);
                        int maxHomes = getMaxHomes(player);
                        
                        // Check if this is a new home or updating existing
                        try (PreparedStatement existsStmt = conn.prepareStatement(
                                "SELECT 1 FROM kc_homes WHERE uuid = ? AND name = ?")) {
                            existsStmt.setString(1, uuid.toString());
                            existsStmt.setString(2, name.toLowerCase());
                            ResultSet existsRs = existsStmt.executeQuery();
                            boolean exists = existsRs.next();
                            
                            if (!exists && currentHomes >= maxHomes) {
                                return false; // At limit
                            }
                        }
                    }
                }
                
                // Insert or update home
                String sql = plugin.getDatabaseManager().isMySQL()
                    ? "INSERT INTO kc_homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                      "ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), yaw = VALUES(yaw), pitch = VALUES(pitch)"
                    : "INSERT INTO kc_homes (uuid, name, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                      "ON CONFLICT(uuid, name) DO UPDATE SET world = excluded.world, x = excluded.x, y = excluded.y, z = excluded.z, yaw = excluded.yaw, pitch = excluded.pitch";
                
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, uuid.toString());
                    stmt.setString(2, name.toLowerCase());
                    stmt.setString(3, loc.getWorld().getName());
                    stmt.setDouble(4, loc.getX());
                    stmt.setDouble(5, loc.getY());
                    stmt.setDouble(6, loc.getZ());
                    stmt.setFloat(7, loc.getYaw());
                    stmt.setFloat(8, loc.getPitch());
                    stmt.executeUpdate();
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to set home for " + uuid, e);
            }
            return false;
        });
    }

    /**
     * Delete a home
     */
    public CompletableFuture<Boolean> deleteHome(UUID uuid, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "DELETE FROM kc_homes WHERE uuid = ? AND name = ?")) {
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, name.toLowerCase());
                return stmt.executeUpdate() > 0;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete home " + name + " for " + uuid, e);
            }
            return false;
        });
    }

    /**
     * Teleport a player to a home with delay
     */
    public void teleportToHome(Player player, String homeName) {
        // Check cooldown
        Long lastTeleport = cooldowns.get(player.getUniqueId());
        if (lastTeleport != null) {
            long remaining = (lastTeleport + (teleportCooldown * 1000L)) - System.currentTimeMillis();
            if (remaining > 0) {
                plugin.getMessageManager().send(player, "homes.cooldown",
                    "seconds", String.valueOf(Math.ceil(remaining / 1000.0)));
                return;
            }
        }
        
        // Cancel any pending teleport
        Integer existingTask = pendingTeleports.remove(player.getUniqueId());
        if (existingTask != null) {
            Bukkit.getScheduler().cancelTask(existingTask);
        }
        
        getHome(player.getUniqueId(), homeName).thenAccept(location -> {
            if (location == null) {
                Bukkit.getScheduler().runTask(plugin, () -> 
                    plugin.getMessageManager().send(player, "homes.not-found", "name", homeName));
                return;
            }
            
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (teleportDelay <= 0) {
                    // Instant teleport
                    doTeleport(player, location, homeName);
                } else {
                    // Delayed teleport
                    plugin.getMessageManager().send(player, "homes.teleporting",
                        "seconds", String.valueOf(teleportDelay),
                        "name", homeName);
                    
                    Location startLoc = player.getLocation();
                    
                    int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                        pendingTeleports.remove(player.getUniqueId());
                        
                        // Check if player moved
                        if (player.getLocation().distanceSquared(startLoc) > 1) {
                            plugin.getMessageManager().send(player, "homes.teleport-cancelled");
                            return;
                        }
                        
                        doTeleport(player, location, homeName);
                    }, teleportDelay * 20L);
                    
                    pendingTeleports.put(player.getUniqueId(), taskId);
                }
            });
        });
    }

    private void doTeleport(Player player, Location location, String homeName) {
        player.teleportAsync(location).thenAccept(success -> {
            if (success) {
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                plugin.getMessageManager().send(player, "homes.teleported", "name", homeName);
            } else {
                plugin.getMessageManager().send(player, "homes.teleport-failed");
            }
        });
    }

    /**
     * Cancel a pending teleport
     */
    public void cancelTeleport(Player player) {
        Integer taskId = pendingTeleports.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public int getTeleportDelay() {
        return teleportDelay;
    }

    public int getTeleportCooldown() {
        return teleportCooldown;
    }
}
