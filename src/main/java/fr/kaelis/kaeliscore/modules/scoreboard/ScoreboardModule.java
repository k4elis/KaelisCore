package fr.kaelis.kaeliscore.modules.scoreboard;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import fr.kaelis.kaeliscore.modules.ranks.RanksModule;
import fr.kaelis.kaeliscore.modules.stats.StatsModule;
import fr.kaelis.kaeliscore.player.KaelisPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Module for sidebar scoreboard
 */
public class ScoreboardModule extends AbstractModule implements Listener {

    private final Map<UUID, Scoreboard> playerScoreboards = new HashMap<>();
    private int updateInterval;
    private String title;
    private List<String> lines;

    public ScoreboardModule(KaelisCore plugin) {
        super(plugin, "Scoreboard", "scoreboard");
    }

    @Override
    public void onEnable() {
        loadConfig();
        
        Bukkit.getPluginManager().registerEvents(this, plugin);
        
        // Start update task
        Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllScoreboards, 20L, updateInterval * 20L);
        
        // Create scoreboard for online players
        for (Player player : Bukkit.getOnlinePlayers()) {
            createScoreboard(player);
        }
    }

    @Override
    public void onDisable() {
        // Remove scoreboards
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeScoreboard(player);
        }
        playerScoreboards.clear();
    }

    private void loadConfig() {
        updateInterval = getConfig().getInt("update-interval", 5);
        title = getConfig().getString("title", "<gradient:#6366f1:#8b5cf6><bold>KaelisCore</bold></gradient>");
        lines = getConfig().getStringList("lines");
        
        if (lines.isEmpty()) {
            // Default lines
            lines = List.of(
                "",
                "<gray>👤 <white>%player_name%",
                "<gray>🏆 <white>%player_rank%",
                "",
                "<gray>💰 <yellow>%player_balance%",
                "<gray>⚔ <red>%player_kills% <gray>kills",
                "<gray>☠ <gray>%player_deaths% <gray>deaths",
                "",
                "<gray>⏱ <aqua>%player_playtime%",
                "",
                "<yellow>play.monserveur.fr"
            );
        }
    }

    /**
     * Create scoreboard for a player
     */
    public void createScoreboard(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective("kaeliscore", Criteria.DUMMY, 
            plugin.getMessageManager().parse(title));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        
        playerScoreboards.put(player.getUniqueId(), scoreboard);
        player.setScoreboard(scoreboard);
        
        updateScoreboard(player);
    }

    /**
     * Remove scoreboard from a player
     */
    public void removeScoreboard(Player player) {
        playerScoreboards.remove(player.getUniqueId());
        player.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
    }

    /**
     * Update a player's scoreboard
     */
    public void updateScoreboard(Player player) {
        Scoreboard scoreboard = playerScoreboards.get(player.getUniqueId());
        if (scoreboard == null) return;

        Objective objective = scoreboard.getObjective("kaeliscore");
        if (objective == null) return;

        // Clear existing entries
        for (String entry : scoreboard.getEntries()) {
            scoreboard.resetScores(entry);
        }

        // Get player data
        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(player);
        EconomyModule economy = plugin.getModuleManager().getModule(EconomyModule.class);
        RanksModule ranks = plugin.getModuleManager().getModule(RanksModule.class);
        StatsModule stats = plugin.getModuleManager().getModule(StatsModule.class);

        // Build lines with placeholders replaced
        int score = lines.size();
        for (String line : lines) {
            String parsed = replacePlaceholders(line, player, kPlayer, economy, ranks, stats);
            
            // Use unique whitespace for duplicate lines
            String uniqueLine = makeUnique(parsed, scoreboard);
            
            var team = scoreboard.getTeam("line_" + score);
            if (team == null) {
                team = scoreboard.registerNewTeam("line_" + score);
            }
            
            // Use entry with invisible characters for uniqueness
            String entry = getEntryForScore(score);
            team.addEntry(entry);
            team.prefix(plugin.getMessageManager().parse(parsed));
            
            objective.getScore(entry).setScore(score);
            score--;
        }
    }

    private String getEntryForScore(int score) {
        // Use color codes as unique entries
        StringBuilder entry = new StringBuilder();
        String hex = Integer.toHexString(score);
        for (char c : hex.toCharArray()) {
            entry.append("§").append(c);
        }
        return entry.toString();
    }

    private String makeUnique(String line, Scoreboard scoreboard) {
        String unique = line;
        int counter = 0;
        while (scoreboard.getEntries().contains(unique) && counter < 100) {
            unique = line + " ".repeat(counter + 1);
            counter++;
        }
        return unique;
    }

    private String replacePlaceholders(String line, Player player, KaelisPlayer kPlayer, 
            EconomyModule economy, RanksModule ranks, StatsModule stats) {
        
        String result = line;
        
        // Player placeholders
        result = result.replace("%player_name%", player.getName());
        result = result.replace("%player_displayname%", player.getName());
        
        // Economy
        if (economy != null) {
            result = result.replace("%player_balance%", economy.format(kPlayer.getBalance()));
            result = result.replace("%player_balance_raw%", String.valueOf(kPlayer.getBalance()));
        }
        
        // Stats
        result = result.replace("%player_kills%", String.valueOf((int) kPlayer.getStat("kills")));
        result = result.replace("%player_deaths%", String.valueOf((int) kPlayer.getStat("deaths")));
        result = result.replace("%player_mob_kills%", String.valueOf((int) kPlayer.getStat("mob_kills")));
        
        // KDR
        double kills = kPlayer.getStat("kills");
        double deaths = kPlayer.getStat("deaths");
        double kdr = deaths == 0 ? kills : kills / deaths;
        result = result.replace("%player_kdr%", String.format("%.2f", kdr));
        
        // Playtime
        result = result.replace("%player_playtime%", kPlayer.getPlayTimeFormatted());
        
        // Rank
        if (ranks != null) {
            var rank = ranks.getCurrentRank(player);
            result = result.replace("%player_rank%", rank != null ? rank.name() : "Aucun");
            result = result.replace("%player_rank_prefix%", rank != null ? rank.prefix() : "");
        }
        
        // Server info
        result = result.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        result = result.replace("%max_players%", String.valueOf(Bukkit.getMaxPlayers()));
        
        return result;
    }

    /**
     * Update all online players' scoreboards
     */
    private void updateAllScoreboards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updateScoreboard(player);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            createScoreboard(event.getPlayer());
        }, 10L);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerScoreboards.remove(event.getPlayer().getUniqueId());
    }

    /**
     * Toggle scoreboard for a player
     */
    public void toggleScoreboard(Player player) {
        if (playerScoreboards.containsKey(player.getUniqueId())) {
            removeScoreboard(player);
            plugin.getMessageManager().send(player, "scoreboard.disabled");
        } else {
            createScoreboard(player);
            plugin.getMessageManager().send(player, "scoreboard.enabled");
        }
    }
}
