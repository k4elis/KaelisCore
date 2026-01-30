package fr.kaelis.kaeliscore.modules.claims;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Location;

import java.util.*;

/**
 * Represents a claimed area
 */
public class Claim {

    private static final Gson GSON = new Gson();

    private final int id;
    private final UUID owner;
    private final String world;
    private final int minX, minZ, maxX, maxZ;
    private final long created;
    private final Map<UUID, ClaimRole> members = new HashMap<>();
    private final Map<String, Boolean> flags = new HashMap<>();

    public Claim(int id, UUID owner, String world, int minX, int minZ, int maxX, int maxZ, long created, String flagsJson) {
        this.id = id;
        this.owner = owner;
        this.world = world;
        this.minX = minX;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxZ = maxZ;
        this.created = created;
        
        // Parse flags
        if (flagsJson != null && !flagsJson.isEmpty()) {
            try {
                JsonObject json = JsonParser.parseString(flagsJson).getAsJsonObject();
                for (String key : json.keySet()) {
                    flags.put(key, json.get(key).getAsBoolean());
                }
            } catch (Exception ignored) {}
        }
        
        // Default flags
        flags.putIfAbsent("pvp", false);
        flags.putIfAbsent("mob-spawning", true);
        flags.putIfAbsent("explosions", false);
        flags.putIfAbsent("fire-spread", false);
    }

    /**
     * Check if location is inside this claim
     */
    public boolean contains(Location location) {
        if (!location.getWorld().getName().equals(world)) {
            return false;
        }
        int x = location.getBlockX();
        int z = location.getBlockZ();
        return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
    }

    /**
     * Check if this claim overlaps with another area
     */
    public boolean overlaps(int otherMinX, int otherMinZ, int otherMaxX, int otherMaxZ) {
        return !(maxX < otherMinX || minX > otherMaxX || maxZ < otherMinZ || minZ > otherMaxZ);
    }

    /**
     * Get the area of the claim
     */
    public int getArea() {
        return (maxX - minX + 1) * (maxZ - minZ + 1);
    }

    /**
     * Add a member to the claim
     */
    public void addMember(UUID uuid, ClaimRole role) {
        members.put(uuid, role);
    }

    /**
     * Remove a member from the claim
     */
    public void removeMember(UUID uuid) {
        members.remove(uuid);
    }

    /**
     * Get member role
     */
    public ClaimRole getMemberRole(UUID uuid) {
        if (uuid.equals(owner)) {
            return ClaimRole.OWNER;
        }
        return members.getOrDefault(uuid, ClaimRole.NONE);
    }

    /**
     * Check if player can build
     */
    public boolean canBuild(UUID uuid) {
        ClaimRole role = getMemberRole(uuid);
        return role == ClaimRole.OWNER || role == ClaimRole.TRUSTED;
    }

    /**
     * Check if player can interact (chests, doors, etc)
     */
    public boolean canInteract(UUID uuid) {
        ClaimRole role = getMemberRole(uuid);
        return role != ClaimRole.NONE;
    }

    /**
     * Set a flag
     */
    public void setFlag(String flag, boolean value) {
        flags.put(flag, value);
    }

    /**
     * Get a flag
     */
    public boolean getFlag(String flag) {
        return flags.getOrDefault(flag, false);
    }

    /**
     * Serialize flags to JSON
     */
    public String getFlagsJson() {
        return GSON.toJson(flags);
    }

    // Getters
    public int getId() {
        return id;
    }

    public UUID getOwner() {
        return owner;
    }

    public String getWorld() {
        return world;
    }

    public int getMinX() {
        return minX;
    }

    public int getMinZ() {
        return minZ;
    }

    public int getMaxX() {
        return maxX;
    }

    public int getMaxZ() {
        return maxZ;
    }

    public long getCreated() {
        return created;
    }

    public Map<UUID, ClaimRole> getMembers() {
        return members;
    }

    public Map<String, Boolean> getFlags() {
        return flags;
    }
}
