package fr.kaelis.kaeliscore.modules.claims;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
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
 * Claims module for land protection
 */
public class ClaimsModule extends AbstractModule {

    private final Map<String, Claim> claimCache = new ConcurrentHashMap<>();
    private int maxClaimsPerPlayer;
    private int maxClaimSize;
    private int minClaimSize;
    private int claimBlocksPerHour;

    public ClaimsModule(KaelisCore plugin) {
        super(plugin, "Claims", "claims");
    }

    @Override
    public void onEnable() {
        maxClaimsPerPlayer = getConfig().getInt("limits.max-claims-per-player", 5);
        maxClaimSize = getConfig().getInt("limits.max-claim-size", 10000);
        minClaimSize = getConfig().getInt("limits.min-claim-size", 100);
        claimBlocksPerHour = getConfig().getInt("claim-blocks-per-hour", 100);
        
        // Load all claims into cache
        loadAllClaims();
    }

    @Override
    public void onDisable() {
        claimCache.clear();
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
}
