package fr.kaelis.kaeliscore.modules.ranks;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import fr.kaelis.kaeliscore.player.KaelisPlayer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.*;

/**
 * Module for automatic rank progression
 */
public class RanksModule extends AbstractModule implements Listener {

    private final List<Rank> ranks = new ArrayList<>();
    private boolean autoProgress;
    private int checkInterval;

    public RanksModule(KaelisCore plugin) {
        super(plugin, "Ranks", "ranks");
    }

    @Override
    public void onEnable() {
        loadRanks();
        autoProgress = getConfig().getBoolean("auto-progress", true);
        checkInterval = getConfig().getInt("check-interval", 300);
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start periodic rank check
        if (autoProgress) {
            Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkAllPlayersRanks, 
                20L * checkInterval, 20L * checkInterval);
        }
    }

    @Override
    public void onDisable() {
        ranks.clear();
    }

    private void loadRanks() {
        ConfigurationSection ranksSection = getConfig().getConfigurationSection("ranks");
        if (ranksSection == null) return;

        for (String rankId : ranksSection.getKeys(false)) {
            ConfigurationSection rankConfig = ranksSection.getConfigurationSection(rankId);
            if (rankConfig == null) continue;

            String name = rankConfig.getString("name", rankId);
            String prefix = rankConfig.getString("prefix", "");
            String permission = rankConfig.getString("permission", "kaeliscore.rank." + rankId);
            int priority = rankConfig.getInt("priority", 0);
            
            // Requirements
            long playTimeRequired = rankConfig.getLong("requirements.playtime", 0); // in minutes
            int killsRequired = rankConfig.getInt("requirements.kills", 0);
            double balanceRequired = rankConfig.getDouble("requirements.balance", 0);
            int questsRequired = rankConfig.getInt("requirements.quests", 0);
            
            // Rewards
            List<String> commands = rankConfig.getStringList("rewards.commands");
            double moneyReward = rankConfig.getDouble("rewards.money", 0);

            ranks.add(new Rank(rankId, name, prefix, permission, priority, 
                playTimeRequired, killsRequired, balanceRequired, questsRequired,
                commands, moneyReward));
        }

        // Sort by priority
        ranks.sort(Comparator.comparingInt(Rank::priority));
        
        plugin.getLogger().info("Loaded " + ranks.size() + " ranks");
    }

    /**
     * Get all ranks
     */
    public List<Rank> getAllRanks() {
        return Collections.unmodifiableList(ranks);
    }

    /**
     * Get a player's current rank
     */
    public Rank getCurrentRank(Player player) {
        Rank currentRank = null;
        for (Rank rank : ranks) {
            if (player.hasPermission(rank.permission())) {
                if (currentRank == null || rank.priority() > currentRank.priority()) {
                    currentRank = rank;
                }
            }
        }
        return currentRank;
    }

    /**
     * Get the next rank for a player
     */
    public Rank getNextRank(Player player) {
        Rank current = getCurrentRank(player);
        int currentPriority = current != null ? current.priority() : -1;
        
        for (Rank rank : ranks) {
            if (rank.priority() > currentPriority) {
                return rank;
            }
        }
        return null;
    }

    /**
     * Check if a player qualifies for a rank
     */
    public boolean qualifiesForRank(Player player, Rank rank) {
        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(player);
        
        // Check playtime (convert to minutes)
        long playTimeMinutes = kPlayer.getPlayTime() / 60000;
        if (playTimeMinutes < rank.playTimeRequired()) return false;
        
        // Check kills
        if (kPlayer.getStat("kills") < rank.killsRequired()) return false;
        
        // Check balance
        if (kPlayer.getBalance() < rank.balanceRequired()) return false;
        
        // Check completed quests
        // TODO: Integrate with quests module
        
        return true;
    }

    /**
     * Try to promote a player to their next available rank
     */
    public boolean tryPromote(Player player) {
        Rank nextRank = getNextRank(player);
        if (nextRank == null) return false;
        
        if (!qualifiesForRank(player, nextRank)) return false;
        
        return promote(player, nextRank);
    }

    /**
     * Promote a player to a specific rank
     */
    public boolean promote(Player player, Rank rank) {
        // Grant permission via LuckPerms if available
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                var luckPerms = net.luckperms.api.LuckPermsProvider.get();
                var user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    user.data().add(net.luckperms.api.node.Node.builder(rank.permission()).build());
                    luckPerms.getUserManager().saveUser(user);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to add rank permission via LuckPerms: " + e.getMessage());
            }
        }
        
        // Execute reward commands
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (String command : rank.commands()) {
                String parsed = command.replace("%player%", player.getName());
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parsed);
            }
            
            // Give money reward
            if (rank.moneyReward() > 0) {
                var economy = plugin.getModuleManager().getModule(
                    fr.kaelis.kaeliscore.modules.economy.EconomyModule.class);
                if (economy != null) {
                    economy.deposit(player, rank.moneyReward());
                }
            }
        });
        
        // Broadcast
        plugin.getMessageManager().send(player, "ranks.promoted", 
            "rank", rank.name(),
            "prefix", rank.prefix());
        
        // Optional: broadcast to server
        if (getConfig().getBoolean("broadcast-promotions", true)) {
            Bukkit.broadcast(plugin.getMessageManager().getPrefixed("ranks.broadcast",
                "player", player.getName(),
                "rank", rank.name()));
        }
        
        return true;
    }

    /**
     * Check all online players for rank upgrades
     */
    private void checkAllPlayersRanks() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Bukkit.getScheduler().runTask(plugin, () -> tryPromote(player));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (autoProgress) {
            // Check rank on join (delayed to allow data loading)
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                tryPromote(event.getPlayer());
            }, 40L); // 2 seconds delay
        }
    }

    /**
     * Rank record
     */
    public record Rank(
        String id,
        String name,
        String prefix,
        String permission,
        int priority,
        long playTimeRequired,
        int killsRequired,
        double balanceRequired,
        int questsRequired,
        List<String> commands,
        double moneyReward
    ) {}
}
