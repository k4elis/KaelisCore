package fr.kaelis.kaeliscore.modules.teleport;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Module for teleportation features (TPA, spawn, back)
 */
public class TeleportModule extends AbstractModule implements Listener {

    // TPA requests: requester -> target
    private final Map<UUID, TpaRequest> tpaRequests = new HashMap<>();
    
    // Back locations
    private final Map<UUID, Location> backLocations = new HashMap<>();
    
    // Spawn location
    private Location spawnLocation;
    
    // Settings
    private int tpaTimeout;
    private int teleportDelay;
    private int teleportCooldown;
    private final Map<UUID, Long> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> pendingTeleports = new HashMap<>();

    public TeleportModule(KaelisCore plugin) {
        super(plugin, "Teleport", "homes");
    }

    @Override
    public void onEnable() {
        tpaTimeout = getConfig().getInt("tpa-timeout", 60);
        teleportDelay = getConfig().getInt("teleport-delay", 3);
        teleportCooldown = getConfig().getInt("teleport-cooldown", 5);
        
        // Load spawn
        loadSpawn();
        
        // Register listeners
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start TPA cleanup task
        Bukkit.getScheduler().runTaskTimer(plugin, this::cleanupExpiredRequests, 20L * 30, 20L * 30);
    }

    @Override
    public void onDisable() {
        for (int taskId : pendingTeleports.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        pendingTeleports.clear();
        tpaRequests.clear();
        backLocations.clear();
    }

    private void loadSpawn() {
        String world = getConfig().getString("spawn.world");
        if (world != null && Bukkit.getWorld(world) != null) {
            spawnLocation = new Location(
                Bukkit.getWorld(world),
                getConfig().getDouble("spawn.x", 0),
                getConfig().getDouble("spawn.y", 64),
                getConfig().getDouble("spawn.z", 0),
                (float) getConfig().getDouble("spawn.yaw", 0),
                (float) getConfig().getDouble("spawn.pitch", 0)
            );
        }
    }

    /**
     * Set the spawn location
     */
    public void setSpawn(Location location) {
        this.spawnLocation = location;
        
        // Save to config
        var config = getConfig();
        config.set("spawn.world", location.getWorld().getName());
        config.set("spawn.x", location.getX());
        config.set("spawn.y", location.getY());
        config.set("spawn.z", location.getZ());
        config.set("spawn.yaw", location.getYaw());
        config.set("spawn.pitch", location.getPitch());
        saveConfig();
    }

    /**
     * Get spawn location
     */
    public Location getSpawn() {
        return spawnLocation != null ? spawnLocation : Bukkit.getWorlds().get(0).getSpawnLocation();
    }

    /**
     * Teleport to spawn
     */
    public void teleportToSpawn(Player player) {
        Location spawn = getSpawn();
        saveBackLocation(player);
        teleportWithDelay(player, spawn, "spawn.teleporting", "spawn.teleported");
    }

    /**
     * Send a TPA request
     */
    public void sendTpaRequest(Player requester, Player target) {
        if (requester.equals(target)) {
            plugin.getMessageManager().send(requester, "tpa.self");
            return;
        }

        // Check if already has a pending request
        TpaRequest existing = tpaRequests.get(requester.getUniqueId());
        if (existing != null && !existing.isExpired()) {
            plugin.getMessageManager().send(requester, "tpa.already-pending");
            return;
        }

        // Create request
        TpaRequest request = new TpaRequest(requester.getUniqueId(), target.getUniqueId(), System.currentTimeMillis());
        tpaRequests.put(requester.getUniqueId(), request);

        plugin.getMessageManager().send(requester, "tpa.sent", "player", target.getName());
        plugin.getMessageManager().send(target, "tpa.received", "player", requester.getName());
    }

    /**
     * Accept a TPA request
     */
    public void acceptTpa(Player target) {
        TpaRequest request = findRequestForTarget(target.getUniqueId());
        if (request == null || request.isExpired()) {
            plugin.getMessageManager().send(target, "tpa.no-pending");
            return;
        }

        Player requester = Bukkit.getPlayer(request.requester());
        if (requester == null) {
            plugin.getMessageManager().send(target, "tpa.requester-offline");
            tpaRequests.remove(request.requester());
            return;
        }

        tpaRequests.remove(request.requester());
        
        saveBackLocation(requester);
        teleportWithDelay(requester, target.getLocation(), "tpa.teleporting", "tpa.teleported",
            "player", target.getName());
        plugin.getMessageManager().send(target, "tpa.accepted", "player", requester.getName());
    }

    /**
     * Deny a TPA request
     */
    public void denyTpa(Player target) {
        TpaRequest request = findRequestForTarget(target.getUniqueId());
        if (request == null || request.isExpired()) {
            plugin.getMessageManager().send(target, "tpa.no-pending");
            return;
        }

        Player requester = Bukkit.getPlayer(request.requester());
        tpaRequests.remove(request.requester());
        
        plugin.getMessageManager().send(target, "tpa.denied-target");
        if (requester != null) {
            plugin.getMessageManager().send(requester, "tpa.denied", "player", target.getName());
        }
    }

    private TpaRequest findRequestForTarget(UUID target) {
        for (TpaRequest request : tpaRequests.values()) {
            if (request.target().equals(target)) {
                return request;
            }
        }
        return null;
    }

    /**
     * Teleport back to last location
     */
    public void teleportBack(Player player) {
        Location back = backLocations.get(player.getUniqueId());
        if (back == null) {
            plugin.getMessageManager().send(player, "back.no-location");
            return;
        }

        Location current = player.getLocation();
        teleportWithDelay(player, back, "back.teleporting", "back.teleported");
        backLocations.put(player.getUniqueId(), current);
    }

    /**
     * Save back location
     */
    public void saveBackLocation(Player player) {
        backLocations.put(player.getUniqueId(), player.getLocation());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        saveBackLocation(event.getEntity());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.PLUGIN) {
            saveBackLocation(event.getPlayer());
        }
    }

    private void teleportWithDelay(Player player, Location destination, String startMessage, String endMessage, String... replacements) {
        // Check cooldown
        if (!player.hasPermission("kaeliscore.teleport.bypass-cooldown")) {
            Long lastTeleport = cooldowns.get(player.getUniqueId());
            if (lastTeleport != null) {
                long remaining = (lastTeleport + (teleportCooldown * 1000L)) - System.currentTimeMillis();
                if (remaining > 0) {
                    plugin.getMessageManager().send(player, "teleport.cooldown",
                        "seconds", String.valueOf(Math.ceil(remaining / 1000.0)));
                    return;
                }
            }
        }

        // Cancel existing teleport
        Integer existingTask = pendingTeleports.remove(player.getUniqueId());
        if (existingTask != null) {
            Bukkit.getScheduler().cancelTask(existingTask);
        }

        if (teleportDelay <= 0 || player.hasPermission("kaeliscore.teleport.bypass-delay")) {
            doTeleport(player, destination, endMessage, replacements);
        } else {
            List<String> msgReplacements = new ArrayList<>(Arrays.asList(replacements));
            msgReplacements.add("seconds");
            msgReplacements.add(String.valueOf(teleportDelay));
            plugin.getMessageManager().send(player, startMessage, msgReplacements.toArray(new String[0]));
            
            Location startLoc = player.getLocation();
            
            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
                pendingTeleports.remove(player.getUniqueId());
                
                if (player.getLocation().distanceSquared(startLoc) > 1) {
                    plugin.getMessageManager().send(player, "teleport.cancelled");
                    return;
                }
                
                doTeleport(player, destination, endMessage, replacements);
            }, teleportDelay * 20L);
            
            pendingTeleports.put(player.getUniqueId(), taskId);
        }
    }

    private void doTeleport(Player player, Location destination, String successMessage, String... replacements) {
        player.teleportAsync(destination).thenAccept(success -> {
            if (success) {
                cooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                plugin.getMessageManager().send(player, successMessage, replacements);
            } else {
                plugin.getMessageManager().send(player, "teleport.failed");
            }
        });
    }

    /**
     * Cancel pending teleport
     */
    public void cancelTeleport(Player player) {
        Integer taskId = pendingTeleports.remove(player.getUniqueId());
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    private void cleanupExpiredRequests() {
        tpaRequests.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * TPA Request record
     */
    private record TpaRequest(UUID requester, UUID target, long timestamp) {
        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > 60000; // 60 seconds
        }
    }
}
