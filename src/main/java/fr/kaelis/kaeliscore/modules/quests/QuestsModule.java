package fr.kaelis.kaeliscore.modules.quests;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
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
 * Quests module for missions and objectives
 */
public class QuestsModule extends AbstractModule implements Listener {

    private final Map<String, Quest> quests = new LinkedHashMap<>();
    private final Map<UUID, Map<String, QuestProgress>> playerProgress = new ConcurrentHashMap<>();

    public QuestsModule(KaelisCore plugin) {
        super(plugin, "Quests", "quests");
    }

    @Override
    public void onEnable() {
        loadQuests();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
        // Save all progress
        for (Map.Entry<UUID, Map<String, QuestProgress>> entry : playerProgress.entrySet()) {
            savePlayerProgress(entry.getKey(), entry.getValue());
        }
        quests.clear();
        playerProgress.clear();
    }

    private void loadQuests() {
        ConfigurationSection questsSection = getConfig().getConfigurationSection("quests");
        if (questsSection == null) return;

        for (String questId : questsSection.getKeys(false)) {
            ConfigurationSection questConfig = questsSection.getConfigurationSection(questId);
            if (questConfig == null) continue;

            String name = questConfig.getString("name", questId);
            String description = questConfig.getString("description", "");
            QuestType type = QuestType.valueOf(questConfig.getString("type", "BREAK_BLOCK").toUpperCase());
            String target = questConfig.getString("target", "");
            int amount = questConfig.getInt("amount", 1);
            boolean repeatable = questConfig.getBoolean("repeatable", false);
            
            // Rewards
            double moneyReward = questConfig.getDouble("rewards.money", 0);
            int xpReward = questConfig.getInt("rewards.xp", 0);
            List<String> itemRewards = questConfig.getStringList("rewards.items");
            
            Material icon = Material.matchMaterial(questConfig.getString("icon", "BOOK"));
            if (icon == null) icon = Material.BOOK;

            Quest quest = new Quest(questId, name, description, type, target, amount, repeatable, 
                                   moneyReward, xpReward, itemRewards, icon);
            quests.put(questId.toLowerCase(), quest);
        }

        plugin.getLogger().info("Loaded " + quests.size() + " quests");
    }

    /**
     * Get all quests
     */
    public Collection<Quest> getAllQuests() {
        return quests.values();
    }

    /**
     * Get a quest by ID
     */
    public Quest getQuest(String id) {
        return quests.get(id.toLowerCase());
    }

    /**
     * Get player's progress for all quests
     */
    public CompletableFuture<Map<String, QuestProgress>> getPlayerProgress(UUID uuid) {
        Map<String, QuestProgress> cached = playerProgress.get(uuid);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }

        return CompletableFuture.supplyAsync(() -> {
            Map<String, QuestProgress> progress = new HashMap<>();
            
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "SELECT quest_id, progress, completed, completed_at FROM kc_player_quests WHERE uuid = ?")) {
                
                stmt.setString(1, uuid.toString());
                ResultSet rs = stmt.executeQuery();
                
                while (rs.next()) {
                    String questId = rs.getString("quest_id");
                    progress.put(questId, new QuestProgress(
                        questId,
                        rs.getInt("progress"),
                        rs.getBoolean("completed"),
                        rs.getLong("completed_at")
                    ));
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to load quest progress for " + uuid, e);
            }
            
            playerProgress.put(uuid, progress);
            return progress;
        });
    }

    /**
     * Get progress for a specific quest
     */
    public QuestProgress getProgress(UUID uuid, String questId) {
        Map<String, QuestProgress> progress = playerProgress.get(uuid);
        if (progress == null) return null;
        return progress.get(questId.toLowerCase());
    }

    /**
     * Update quest progress
     */
    public void updateProgress(Player player, QuestType type, String target, int amount) {
        UUID uuid = player.getUniqueId();
        
        getPlayerProgress(uuid).thenAccept(progress -> {
            for (Quest quest : quests.values()) {
                if (quest.type() != type) continue;
                if (!quest.target().isEmpty() && !quest.target().equalsIgnoreCase(target)) continue;
                
                QuestProgress questProgress = progress.computeIfAbsent(quest.id().toLowerCase(), 
                    k -> new QuestProgress(quest.id(), 0, false, 0));
                
                // Skip if completed and not repeatable
                if (questProgress.completed() && !quest.repeatable()) continue;
                
                // Update progress
                int newProgress = questProgress.progress() + amount;
                QuestProgress updated = new QuestProgress(
                    quest.id(),
                    newProgress,
                    newProgress >= quest.amount(),
                    questProgress.completedAt()
                );
                progress.put(quest.id().toLowerCase(), updated);
                
                // Check completion
                if (newProgress >= quest.amount() && !questProgress.completed()) {
                    // Quest completed!
                    updated = new QuestProgress(quest.id(), newProgress, true, System.currentTimeMillis());
                    progress.put(quest.id().toLowerCase(), updated);
                    
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        giveRewards(player, quest);
                        plugin.getMessageManager().send(player, "quests.completed", "quest", quest.name());
                    });
                }
            }
        });
    }

    private void giveRewards(Player player, Quest quest) {
        // Money
        if (quest.moneyReward() > 0) {
            EconomyModule economy = plugin.getModuleManager().getModule(EconomyModule.class);
            if (economy != null) {
                economy.deposit(player, quest.moneyReward());
            }
        }
        
        // XP
        if (quest.xpReward() > 0) {
            player.giveExp(quest.xpReward());
        }
        
        // Items
        for (String itemStr : quest.itemRewards()) {
            String[] parts = itemStr.split(":");
            Material material = Material.matchMaterial(parts[0]);
            if (material != null) {
                int amount = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                player.getInventory().addItem(new ItemStack(material, amount));
            }
        }
    }

    private void savePlayerProgress(UUID uuid, Map<String, QuestProgress> progress) {
        String sql = plugin.getDatabaseManager().isMySQL()
            ? "INSERT INTO kc_player_quests (uuid, quest_id, progress, completed, completed_at) VALUES (?, ?, ?, ?, ?) " +
              "ON DUPLICATE KEY UPDATE progress = VALUES(progress), completed = VALUES(completed), completed_at = VALUES(completed_at)"
            : "INSERT INTO kc_player_quests (uuid, quest_id, progress, completed, completed_at) VALUES (?, ?, ?, ?, ?) " +
              "ON CONFLICT(uuid, quest_id) DO UPDATE SET progress = excluded.progress, completed = excluded.completed, completed_at = excluded.completed_at";
        
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            for (QuestProgress qp : progress.values()) {
                stmt.setString(1, uuid.toString());
                stmt.setString(2, qp.questId());
                stmt.setInt(3, qp.progress());
                stmt.setBoolean(4, qp.completed());
                stmt.setLong(5, qp.completedAt());
                stmt.addBatch();
            }
            
            stmt.executeBatch();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.WARNING, "Failed to save quest progress for " + uuid, e);
        }
    }

    // Event listeners for quest tracking
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        updateProgress(event.getPlayer(), QuestType.BREAK_BLOCK, event.getBlock().getType().name(), 1);
    }

    @EventHandler
    public void onEntityKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        
        String entityType = event.getEntityType().name();
        if (event.getEntity() instanceof Player) {
            updateProgress(killer, QuestType.KILL_PLAYER, "", 1);
        } else {
            updateProgress(killer, QuestType.KILL_MOB, entityType, 1);
        }
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            updateProgress(event.getPlayer(), QuestType.FISH, "", 1);
        }
    }

    public enum QuestType {
        BREAK_BLOCK,
        PLACE_BLOCK,
        KILL_MOB,
        KILL_PLAYER,
        FISH,
        CRAFT,
        TRADE,
        TRAVEL
    }

    public record Quest(String id, String name, String description, QuestType type, String target, int amount,
                       boolean repeatable, double moneyReward, int xpReward, List<String> itemRewards, Material icon) {}

    public record QuestProgress(String questId, int progress, boolean completed, long completedAt) {}
}
