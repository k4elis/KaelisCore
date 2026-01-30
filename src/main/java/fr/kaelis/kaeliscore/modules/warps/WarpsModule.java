package fr.kaelis.kaeliscore.modules.warps;

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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Module for server warps (public teleportation points)
 */
public class WarpsModule extends AbstractModule {

    private final Map<String, Warp> warpsCache = new ConcurrentHashMap<>();
    private int teleportDelay;
    private int teleportCooldown;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> pendingTeleports = new HashMap<>();

    public WarpsModule(KaelisCore plugin) {
        super(plugin, "Warps", "homes"); // Share config with homes for now
    }

    @Override
    public void onEnable() {
        teleportDelay = getConfig().getInt("warp-teleport-delay", 3);
        teleportCooldown = getConfig().getInt("warp-teleport-cooldown", 5);
        loadAllWarps();
    }

    @Override
    public void onDisable() {
        // Cancel pending teleports
        for (int taskId : pendingTeleports.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        pendingTeleports.clear();
        warpsCache.clear();
    }

    private void loadAllWarps() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM kc_warps")) {
                
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
                        
                        Warp warp = new Warp(
                            rs.getInt("id"),
                            name,
                            loc,
                            rs.getString("permission"),
                            rs.getString("category")
                        );
                        warpsCache.put(name.toLowerCase(), warp);
                    }
                }
                
                plugin.getLogger().info("Loaded " + warpsCache.size() + " warps");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load warps", e);
            }
        });
    }

    /**
     * Get all warps
     */
    public Collection<Warp> getAllWarps() {
        return warpsCache.values();
    }

    /**
     * Get warps available to a player
     */
    public List<Warp> getAvailableWarps(Player player) {
        List<Warp> available = new ArrayList<>();
        for (Warp warp : warpsCache.values()) {
            if (warp.permission() == null || warp.permission().isEmpty() || player.hasPermission(warp.permission())) {
                available.add(warp);
            }
        }
        return available;
    }

    /**
     * Get a warp by name
     */
    public Warp getWarp(String name) {
        return warpsCache.get(name.toLowerCase());
    }

    /**
     * Create a new warp
     */
    public CompletableFuture<Boolean> createWarp(String name, Location location, String permission, String category) {
        return CompletableFuture.supplyAsync(() -> {
            if (location.getWorld() == null) {
                plugin.getLogger().warning("Cannot create warp " + name + ": location has null world");
                return false;
            }
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO kc_warps (name, world, x, y, z, yaw, pitch, permission, category) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
                
                stmt.setString(1, name.toLowerCase());
                stmt.setString(2, location.getWorld().getName());
                stmt.setDouble(3, location.getX());
                stmt.setDouble(4, location.getY());
                stmt.setDouble(5, location.getZ());
                stmt.setFloat(6, location.getYaw());
                stmt.setFloat(7, location.getPitch());
                stmt.setString(8, permission);
                stmt.setString(9, category);
                stmt.executeUpdate();
                
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    int id = keys.getInt(1);
                    Warp warp = new Warp(id, name, location, permission, category);
                    warpsCache.put(name.toLowerCase(), warp);
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create warp " + name, e);
            }
            return false;
        });
    }

    /**
     * Delete a warp
     */
    public CompletableFuture<Boolean> deleteWarp(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("DELETE FROM kc_warps WHERE name = ?")) {
                
                stmt.setString(1, name.toLowerCase());
                int affected = stmt.executeUpdate();
                
                if (affected > 0) {
                    warpsCache.remove(name.toLowerCase());
                    return true;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete warp " + name, e);
            }
            return false;
        });
    }

    /**
     * Teleport a player to a warp
     */
    public void teleportToWarp(Player player, String warpName) {
        Warp warp = getWarp(warpName);
        if (warp == null) {
            plugin.getMessageManager().send(player, "warps.not-found", "name", warpName);
            return;
        }

        // Check permission
        if (warp.permission() != null && !warp.permission().isEmpty() && !player.hasPermission(warp.permission())) {
            plugin.getMessageManager().send(player, "warps.no-permission", "name", warpName);
            return;
        }

        // Check cooldown
        Long lastTeleport = cooldowns.get(player.getUniqueId());
        if (lastTeleport != null && !player.hasPermission("kaeliscore.warps.bypass-cooldown")) {
            long remaining = (lastTeleport + (teleportCooldown * 1000L)) - System.currentTimeMillis();
            if (remaining > 0) {
                plugin.getMessageManager().send(player, "warps.cooldown",
                    "seconds", String.valueOf(Math.ceil(remaining / 1000.0)));
                return;
            }
        }

        // Cancel any pending teleport
        Integer existingTask = pendingTeleports.remove(player.getUniqueId());
        if (existingTask != null) {
            Bukkit.getScheduler().cancelTask(existingTask);
        }

        if (teleportDelay <= 0 || player.hasPermission("kaeliscore.warps.bypass-delay")) {
            // Instant teleport
            doTeleport(player, warp);
        } else {
            // Delayed teleport
            plugin.getMessageManager().send(player, "warps.teleporting",
                "seconds", String.valueOf(teleportDelay),
                "name", warpName);
            
            Location startLoc = player.getLocation();
            
            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                pendingTeleports.remove(player.getUniqueId());
                
                // Check if player moved
                if (player.getLocation().distanceSquared(startLoc) > 1) {
                    plugin.getMessageManager().send(player, "warps.teleport-cancelled");
                    return;
                }
                
                doTeleport(player, warp);
            }, teleportDelay * 20L);
            
            pendingTeleports.put(player.getUniqueId(), taskId);
        }
    }

    private void doTeleport(Player player, Warp warp) {
        player.teleportAsync(warp.location()).thenAccept(success -> {
            if (success) {
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                plugin.getMessageManager().send(player, "warps.teleported", "name", warp.name());
            } else {
                plugin.getMessageManager().send(player, "warps.teleport-failed");
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

    /**
     * Warp record
     */
    public record Warp(int id, String name, Location location, String permission, String category) {}
}
