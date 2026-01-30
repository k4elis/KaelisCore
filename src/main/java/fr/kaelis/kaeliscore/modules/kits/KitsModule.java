package fr.kaelis.kaeliscore.modules.kits;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Kits module for predefined item sets
 */
public class KitsModule extends AbstractModule {

    private final Map<String, Kit> kits = new LinkedHashMap<>();

    public KitsModule(KaelisCore plugin) {
        super(plugin, "Kits", "kits");
    }

    @Override
    public void onEnable() {
        loadKits();
    }

    @Override
    public void onDisable() {
        kits.clear();
    }

    private void loadKits() {
        ConfigurationSection kitsSection = getConfig().getConfigurationSection("kits");
        if (kitsSection == null) return;

        for (String kitId : kitsSection.getKeys(false)) {
            ConfigurationSection kitConfig = kitsSection.getConfigurationSection(kitId);
            if (kitConfig == null) continue;

            String displayName = kitConfig.getString("name", kitId);
            String permission = kitConfig.getString("permission", "kaeliscore.kits." + kitId);
            int cooldown = kitConfig.getInt("cooldown", 86400); // Default 24 hours
            String description = kitConfig.getString("description", "");
            Material icon = Material.matchMaterial(kitConfig.getString("icon", "CHEST"));
            if (icon == null) icon = Material.CHEST;

            List<ItemStack> items = new ArrayList<>();
            List<String> itemStrings = kitConfig.getStringList("items");
            for (String itemStr : itemStrings) {
                ItemStack item = parseItemString(itemStr);
                if (item != null) {
                    items.add(item);
                }
            }

            Kit kit = new Kit(kitId, displayName, permission, cooldown, description, icon, items);
            kits.put(kitId.toLowerCase(), kit);
        }

        plugin.getLogger().info("Loaded " + kits.size() + " kits");
    }

    private ItemStack parseItemString(String itemStr) {
        // Format: MATERIAL:amount or MATERIAL
        String[] parts = itemStr.split(":");
        Material material = Material.matchMaterial(parts[0]);
        if (material == null) return null;

        int amount = 1;
        if (parts.length > 1) {
            try {
                amount = Integer.parseInt(parts[1]);
            } catch (NumberFormatException ignored) {}
        }

        return new ItemStack(material, amount);
    }

    /**
     * Get all kits
     */
    public Collection<Kit> getAllKits() {
        return kits.values();
    }

    /**
     * Get a kit by ID
     */
    public Kit getKit(String id) {
        return kits.get(id.toLowerCase());
    }

    /**
     * Get kits available to a player
     */
    public List<Kit> getAvailableKits(Player player) {
        List<Kit> available = new ArrayList<>();
        for (Kit kit : kits.values()) {
            if (player.hasPermission(kit.permission())) {
                available.add(kit);
            }
        }
        return available;
    }

    /**
     * Check if a player can claim a kit (cooldown check)
     */
    public CompletableFuture<Long> getCooldownRemaining(UUID uuid, String kitId) {
        return CompletableFuture.supplyAsync(() -> {
            Kit kit = getKit(kitId);
            if (kit == null) return 0L;

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT last_use FROM kc_kit_cooldowns WHERE uuid = ? AND kit = ?")) {
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, kitId.toLowerCase());
                ResultSet rs = stmt.executeQuery();

                if (rs.next()) {
                    long lastUse = rs.getLong("last_use");
                    long cooldownEnd = lastUse + (kit.cooldown() * 1000L);
                    long remaining = cooldownEnd - System.currentTimeMillis();
                    return Math.max(0, remaining);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to check kit cooldown", e);
            }

            return 0L;
        });
    }

    /**
     * Give a kit to a player
     */
    public CompletableFuture<KitResult> giveKit(Player player, String kitId) {
        Kit kit = getKit(kitId);
        if (kit == null) {
            return CompletableFuture.completedFuture(KitResult.NOT_FOUND);
        }

        if (!player.hasPermission(kit.permission())) {
            return CompletableFuture.completedFuture(KitResult.NO_PERMISSION);
        }

        return getCooldownRemaining(player.getUniqueId(), kitId).thenApply(remaining -> {
            if (remaining > 0) {
                return KitResult.ON_COOLDOWN;
            }

            // Check inventory space
            int emptySlots = 0;
            for (ItemStack item : player.getInventory().getStorageContents()) {
                if (item == null || item.getType() == Material.AIR) {
                    emptySlots++;
                }
            }

            if (emptySlots < kit.items().size()) {
                return KitResult.INVENTORY_FULL;
            }

            // Give items on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                for (ItemStack item : kit.items()) {
                    player.getInventory().addItem(item.clone());
                }
            });

            // Update cooldown
            updateCooldown(player.getUniqueId(), kitId);

            return KitResult.SUCCESS;
        });
    }

    private void updateCooldown(UUID uuid, String kitId) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            String sql = plugin.getDatabaseManager().isMySQL()
                ? "INSERT INTO kc_kit_cooldowns (uuid, kit, last_use) VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE last_use = VALUES(last_use)"
                : "INSERT INTO kc_kit_cooldowns (uuid, kit, last_use) VALUES (?, ?, ?) ON CONFLICT(uuid, kit) DO UPDATE SET last_use = excluded.last_use";

            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setString(1, uuid.toString());
                stmt.setString(2, kitId.toLowerCase());
                stmt.setLong(3, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to update kit cooldown", e);
            }
        });
    }

    /**
     * Format cooldown time
     */
    public String formatCooldown(long milliseconds) {
        long seconds = milliseconds / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dj %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm", hours, minutes % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    public enum KitResult {
        SUCCESS,
        NOT_FOUND,
        NO_PERMISSION,
        ON_COOLDOWN,
        INVENTORY_FULL
    }

    public record Kit(String id, String displayName, String permission, int cooldown, String description, Material icon, List<ItemStack> items) {}
}
