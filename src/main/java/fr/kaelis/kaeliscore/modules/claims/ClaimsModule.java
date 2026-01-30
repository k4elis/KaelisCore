package fr.kaelis.kaeliscore.modules.claims;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Claims module for land protection with golden shovel support
 */
public class ClaimsModule extends AbstractModule implements Listener {

    private final Map<String, Claim> claimCache = new ConcurrentHashMap<>();
    private final Map<UUID, Location> claimingCorner1 = new HashMap<>();
    private final Map<UUID, Long> visualizationCooldown = new HashMap<>();
    
    private int maxClaimsPerPlayer;
    private int maxClaimSize;
    private int minClaimSize;
    private int claimBlocksPerHour;
    private Material claimTool;

    public ClaimsModule(KaelisCore plugin) {
        super(plugin, "Claims", "claims");
    }

    @Override
    public void onEnable() {
        maxClaimsPerPlayer = getConfig().getInt("limits.max-claims-per-player", 5);
        maxClaimSize = getConfig().getInt("limits.max-claim-size", 10000);
        minClaimSize = getConfig().getInt("limits.min-claim-size", 100);
        claimBlocksPerHour = getConfig().getInt("claim-blocks-per-hour", 100);
        
        String toolName = getConfig().getString("claim-tool", "GOLDEN_SHOVEL");
        claimTool = Material.valueOf(toolName);
        
        // Register listener
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Load all claims into cache
        loadAllClaims();
    }

    @Override
    public void onDisable() {
        claimCache.clear();
        claimingCorner1.clear();
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();
        
        // Check if using claim tool
        if (item == null || item.getType() != claimTool) return;
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getClickedBlock() == null) return;
        
        event.setCancelled(true);
        
        Location clickedLoc = event.getClickedBlock().getLocation();
        
        // Check if player already has first corner set
        if (claimingCorner1.containsKey(player.getUniqueId())) {
            // This is corner 2 - create claim
            Location corner1 = claimingCorner1.remove(player.getUniqueId());
            
            if (!corner1.getWorld().equals(clickedLoc.getWorld())) {
                plugin.getMessageManager().send(player, "claims.different-worlds");
                return;
            }
            
            createClaimFromCorners(player, corner1, clickedLoc);
        } else {
            // Check if clicked inside existing claim (show info)
            Claim existingClaim = getClaimAt(clickedLoc);
            if (existingClaim != null) {
                showClaimInfo(player, existingClaim);
                visualizeClaim(player, existingClaim);
                return;
            }
            
            // This is corner 1
            claimingCorner1.put(player.getUniqueId(), clickedLoc);
            plugin.getMessageManager().send(player, "claims.corner1-set",
                "x", String.valueOf(clickedLoc.getBlockX()),
                "z", String.valueOf(clickedLoc.getBlockZ()));
            
            // Visual feedback
            player.spawnParticle(Particle.VILLAGER_HAPPY, clickedLoc.clone().add(0.5, 1, 0.5), 10);
        }
    }

    private void createClaimFromCorners(Player player, Location corner1, Location corner2) {
        // Check permission
        if (!player.hasPermission("kaeliscore.claims.create")) {
            plugin.getMessageManager().send(player, "general.no-permission");
            return;
        }
        
        // Calculate area
        int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
        int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
        int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
        int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
        int area = (maxX - minX + 1) * (maxZ - minZ + 1);
        
        // Check size
        if (area < minClaimSize) {
            plugin.getMessageManager().send(player, "claims.too-small", "min", String.valueOf(minClaimSize));
            return;
        }
        
        if (area > maxClaimSize && !player.hasPermission("kaeliscore.claims.admin")) {
            plugin.getMessageManager().send(player, "claims.too-big", "max", String.valueOf(maxClaimSize));
            return;
        }
        
        // Create claim async
        createClaim(player, corner1, corner2).thenAccept(claim -> {
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (claim != null) {
                    plugin.getMessageManager().send(player, "claims.created", "area", String.valueOf(area));
                    visualizeClaim(player, claim);
                    player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
                } else {
                    plugin.getMessageManager().send(player, "claims.overlap");
                }
            });
        });
    }

    private void showClaimInfo(Player player, Claim claim) {
        String ownerName = Bukkit.getOfflinePlayer(claim.getOwner()).getName();
        if (ownerName == null) ownerName = "Inconnu";
        
        int area = claim.getArea();
        
        plugin.getMessageManager().send(player, "claims.info",
            "owner", ownerName,
            "area", String.valueOf(area),
            "members", String.valueOf(claim.getMembers().size()));
    }

    /**
     * Visualize claim boundaries with particles
     */
    public void visualizeClaim(Player player, Claim claim) {
        // Rate limit visualization
        Long lastVis = visualizationCooldown.get(player.getUniqueId());
        if (lastVis != null && System.currentTimeMillis() - lastVis < 2000) return;
        visualizationCooldown.put(player.getUniqueId(), System.currentTimeMillis());
        
        World world = Bukkit.getWorld(claim.getWorld());
        if (world == null) return;
        
        int minX = claim.getMinX();
        int maxX = claim.getMaxX();
        int minZ = claim.getMinZ();
        int maxZ = claim.getMaxZ();
        int y = player.getLocation().getBlockY();
        
        // Spawn particles along the border
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Location> particles = new ArrayList<>();
            
            // Top and bottom edges
            for (int x = minX; x <= maxX; x += 2) {
                particles.add(new Location(world, x + 0.5, y, minZ + 0.5));
                particles.add(new Location(world, x + 0.5, y, maxZ + 0.5));
            }
            
            // Left and right edges
            for (int z = minZ; z <= maxZ; z += 2) {
                particles.add(new Location(world, minX + 0.5, y, z + 0.5));
                particles.add(new Location(world, maxX + 0.5, y, z + 0.5));
            }
            
            // Corners with more particles
            for (int dy = 0; dy < 5; dy++) {
                particles.add(new Location(world, minX + 0.5, y + dy, minZ + 0.5));
                particles.add(new Location(world, minX + 0.5, y + dy, maxZ + 0.5));
                particles.add(new Location(world, maxX + 0.5, y + dy, minZ + 0.5));
                particles.add(new Location(world, maxX + 0.5, y + dy, maxZ + 0.5));
            }
            
            // Spawn particles on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                Particle.DustOptions dust = new Particle.DustOptions(
                    claim.getOwner().equals(player.getUniqueId()) ? Color.LIME : Color.YELLOW, 1);
                
                for (Location loc : particles) {
                    player.spawnParticle(Particle.DUST, loc, 1, dust);
                }
            });
        });
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        claimingCorner1.remove(event.getPlayer().getUniqueId());
        visualizationCooldown.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Cancel claim creation for a player
     */
    public void cancelClaimCreation(Player player) {
        if (claimingCorner1.remove(player.getUniqueId()) != null) {
            plugin.getMessageManager().send(player, "claims.creation-cancelled");
        }
    }

    private void loadAllClaims() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT * FROM kc_claims")) {
                
                ResultSet rs = stmt.executeQuery();
                while (rs.next()) {
                    Claim claim = new Claim(
                        rs.getInt("id"),
                        UUID.fromString(rs.getString("uuid")),
                        rs.getString("world"),
                        rs.getInt("min_x"),
                        rs.getInt("min_z"),
                        rs.getInt("max_x"),
                        rs.getInt("max_z"),
                        rs.getLong("created"),
                        rs.getString("flags")
                    );
                    
                    // Load members
                    loadClaimMembers(claim);
                    
                    claimCache.put(claim.getId() + "", claim);
                }
                
                plugin.getLogger().info("Loaded " + claimCache.size() + " claims");
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load claims", e);
            }
        });
    }

    private void loadClaimMembers(Claim claim) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                 "SELECT uuid, role FROM kc_claim_members WHERE claim_id = ?")) {
            
            stmt.setInt(1, claim.getId());
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                UUID memberUuid = UUID.fromString(rs.getString("uuid"));
                ClaimRole role = ClaimRole.valueOf(rs.getString("role"));
                claim.addMember(memberUuid, role);
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to load members for claim " + claim.getId(), e);
        }
    }

    /**
     * Get claim at a location
     */
    public Claim getClaimAt(Location location) {
        for (Claim claim : claimCache.values()) {
            if (claim.contains(location)) {
                return claim;
            }
        }
        return null;
    }

    /**
     * Get claims owned by a player
     */
    public List<Claim> getClaimsByOwner(UUID owner) {
        List<Claim> claims = new ArrayList<>();
        for (Claim claim : claimCache.values()) {
            if (claim.getOwner().equals(owner)) {
                claims.add(claim);
            }
        }
        return claims;
    }

    /**
     * Create a new claim
     */
    public CompletableFuture<Claim> createClaim(Player player, Location corner1, Location corner2) {
        return CompletableFuture.supplyAsync(() -> {
            UUID owner = player.getUniqueId();
            
            // Check claim limit
            List<Claim> existingClaims = getClaimsByOwner(owner);
            int maxClaims = player.hasPermission("kaeliscore.claims.admin") ? Integer.MAX_VALUE : maxClaimsPerPlayer;
            if (existingClaims.size() >= maxClaims) {
                return null;
            }
            
            // Normalize coordinates
            int minX = Math.min(corner1.getBlockX(), corner2.getBlockX());
            int maxX = Math.max(corner1.getBlockX(), corner2.getBlockX());
            int minZ = Math.min(corner1.getBlockZ(), corner2.getBlockZ());
            int maxZ = Math.max(corner1.getBlockZ(), corner2.getBlockZ());
            String world = corner1.getWorld().getName();
            
            // Check size
            int area = (maxX - minX + 1) * (maxZ - minZ + 1);
            if (area < minClaimSize || area > maxClaimSize) {
                return null;
            }
            
            // Check for overlapping claims
            for (Claim claim : claimCache.values()) {
                if (claim.getWorld().equals(world) && claim.overlaps(minX, minZ, maxX, maxZ)) {
                    return null;
                }
            }
            
            // Create claim in database
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO kc_claims (uuid, world, min_x, min_z, max_x, max_z, created, flags) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                     PreparedStatement.RETURN_GENERATED_KEYS)) {
                
                stmt.setString(1, owner.toString());
                stmt.setString(2, world);
                stmt.setInt(3, minX);
                stmt.setInt(4, minZ);
                stmt.setInt(5, maxX);
                stmt.setInt(6, maxZ);
                stmt.setLong(7, System.currentTimeMillis());
                stmt.setString(8, "{}");
                stmt.executeUpdate();
                
                ResultSet keys = stmt.getGeneratedKeys();
                if (keys.next()) {
                    int id = keys.getInt(1);
                    Claim claim = new Claim(id, owner, world, minX, minZ, maxX, maxZ, System.currentTimeMillis(), "{}");
                    claimCache.put(id + "", claim);
                    return claim;
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create claim", e);
            }
            
            return null;
        });
    }

    /**
     * Delete a claim
     */
    public CompletableFuture<Boolean> deleteClaim(Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection()) {
                // Delete members
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM kc_claim_members WHERE claim_id = ?")) {
                    stmt.setInt(1, claim.getId());
                    stmt.executeUpdate();
                }
                
                // Delete claim
                try (PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM kc_claims WHERE id = ?")) {
                    stmt.setInt(1, claim.getId());
                    stmt.executeUpdate();
                }
                
                claimCache.remove(claim.getId() + "");
                return true;
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete claim " + claim.getId(), e);
            }
            return false;
        });
    }

    /**
     * Check if a player can build at a location
     */
    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission("kaeliscore.claims.admin")) {
            return true;
        }
        
        Claim claim = getClaimAt(location);
        if (claim == null) {
            return true; // No claim = can build
        }
        
        return claim.canBuild(player.getUniqueId());
    }

    /**
     * Check if a player can interact at a location
     */
    public boolean canInteract(Player player, Location location) {
        if (player.hasPermission("kaeliscore.claims.admin")) {
            return true;
        }
        
        Claim claim = getClaimAt(location);
        if (claim == null) {
            return true;
        }
        
        return claim.canInteract(player.getUniqueId());
    }

    public Map<String, Claim> getClaimCache() {
        return claimCache;
    }

    public int getMaxClaimsPerPlayer() {
        return maxClaimsPerPlayer;
    }

    public int getMaxClaimSize() {
        return maxClaimSize;
    }

    public int getMinClaimSize() {
        return minClaimSize;
    }
    
    public Material getClaimTool() {
        return claimTool;
    }
}
