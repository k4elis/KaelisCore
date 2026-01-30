package fr.kaelis.kaeliscore.integrations.discord;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import fr.kaelis.kaeliscore.modules.events.EventsModule;
import fr.kaelis.kaeliscore.modules.stats.StatsModule;
import fr.kaelis.kaeliscore.player.KaelisPlayer;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.logging.Level;

/**
 * Discord bot integration with extended commands
 */
public class DiscordBot extends ListenerAdapter {

    private final KaelisCore plugin;
    private JDA jda;
    private String serverChannelId;
    private String consoleChannelId;
    private String adminRoleId;
    private boolean enabled;
    
    // Player linking
    private final Map<String, UUID> linkedAccounts = new HashMap<>();
    private final Map<UUID, String> pendingLinks = new HashMap<>();

    public DiscordBot(KaelisCore plugin) {
        this.plugin = plugin;
    }

    public void start() {
        FileConfiguration config = plugin.getConfigManager().getConfig("discord");
        String token = config.getString("bot-token", "");
        
        if (token.isEmpty() || token.equals("YOUR_BOT_TOKEN_HERE")) {
            plugin.getLogger().warning("Discord bot token not configured!");
            enabled = false;
            return;
        }

        serverChannelId = config.getString("channels.server", "");
        consoleChannelId = config.getString("channels.console", "");
        adminRoleId = config.getString("admin-role-id", "");

        try {
            jda = JDABuilder.createDefault(token)
                    .enableIntents(GatewayIntent.MESSAGE_CONTENT, GatewayIntent.GUILD_MESSAGES)
                    .setActivity(Activity.playing("Minecraft"))
                    .addEventListeners(this)
                    .build();
            
            jda.awaitReady();
            enabled = true;
            plugin.getLogger().info("Discord bot connected successfully!");
            
            // Send startup message
            sendServerMessage("🟢 **Serveur Démarré!**", "Le serveur Minecraft est maintenant en ligne.", Color.GREEN);
            
            // Update activity periodically
            Bukkit.getScheduler().runTaskTimer(plugin, this::updateActivity, 20L * 60, 20L * 60);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to start Discord bot", e);
            enabled = false;
        }
    }

    public void shutdown() {
        if (jda != null) {
            // Send shutdown message
            sendServerMessage("🔴 **Serveur Arrêté!**", "Le serveur Minecraft s'arrête.", Color.RED);
            
            jda.shutdown();
        }
    }
    
    private void updateActivity() {
        if (jda != null) {
            int online = Bukkit.getOnlinePlayers().size();
            jda.getPresence().setActivity(Activity.playing(online + " joueurs en ligne"));
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        String message = event.getMessage().getContentRaw();
        
        // Chat bridge: Discord -> Minecraft
        if (event.getChannel().getId().equals(serverChannelId)) {
            String author = event.getMember() != null ? event.getMember().getEffectiveName() : event.getAuthor().getName();
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.broadcast(plugin.getMessageManager().parse(
                    "<dark_purple>[Discord]</dark_purple> <gray>" + author + "</gray> <white>" + message
                ));
            });
        }
        
        // Commands
        if (message.startsWith("!")) {
            handleCommand(event, message.substring(1).split(" "));
        }
    }

    private void handleCommand(MessageReceivedEvent event, String[] args) {
        if (args.length == 0) return;
        
        String command = args[0].toLowerCase();
        
        switch (command) {
            case "status" -> sendStatusEmbed(event);
            case "players", "list" -> sendPlayersEmbed(event);
            case "leaderboard", "top" -> sendLeaderboardEmbed(event, args);
            case "link" -> handleLinkCommand(event, args);
            case "stats" -> sendStatsEmbed(event, args);
            case "events" -> sendEventsEmbed(event);
            case "tps" -> sendTpsEmbed(event);
            case "help" -> sendHelpEmbed(event);
            default -> {}
        }
    }

    private void sendStatusEmbed(MessageReceivedEvent event) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        double[] tps = Bukkit.getTPS();
        
        Runtime runtime = Runtime.getRuntime();
        long usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
        long maxMemory = runtime.maxMemory() / 1024 / 1024;
        
        Color statusColor = tps[0] >= 18 ? Color.GREEN : tps[0] >= 15 ? Color.YELLOW : Color.RED;
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 Statut du Serveur")
                .setColor(statusColor)
                .addField("👥 Joueurs", online + "/" + max, true)
                .addField("⚡ TPS", String.format("%.1f", tps[0]), true)
                .addField("💾 RAM", usedMemory + "/" + maxMemory + " MB", true)
                .addField("🌍 Mondes", String.valueOf(Bukkit.getWorlds().size()), true)
                .addField("🔌 Plugins", String.valueOf(Bukkit.getPluginManager().getPlugins().length), true)
                .addField("📅 Uptime", getUptime(), true)
                .setFooter("KaelisCore")
                .setTimestamp(Instant.now());
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
    
    private String getUptime() {
        // Simple approximation - in real implementation you'd track start time
        long seconds = System.currentTimeMillis() / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        return hours + "h " + minutes + "m";
    }

    private void sendPlayersEmbed(MessageReceivedEvent event) {
        int online = Bukkit.getOnlinePlayers().size();
        int max = Bukkit.getMaxPlayers();
        
        StringBuilder players = new StringBuilder();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!players.isEmpty()) players.append(", ");
            players.append("`").append(player.getName()).append("`");
        }
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎮 Joueurs en Ligne (" + online + "/" + max + ")")
                .setDescription(players.length() > 0 ? players.toString() : "*Aucun joueur en ligne*")
                .setColor(Color.CYAN)
                .setTimestamp(Instant.now());
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void sendLeaderboardEmbed(MessageReceivedEvent event, String[] args) {
        String type = args.length > 1 ? args[1].toLowerCase() : "money";
        
        EmbedBuilder embed = new EmbedBuilder()
                .setColor(Color.GOLD)
                .setTimestamp(Instant.now());
        
        switch (type) {
            case "money", "balance", "eco" -> {
                embed.setTitle("💰 Top Richesse");
                
                // Get top balances from database
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    List<String> topPlayers = getTopBalances(10);
                    StringBuilder sb = new StringBuilder();
                    int rank = 1;
                    for (String entry : topPlayers) {
                        sb.append(rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : rank + ".");
                        sb.append(" ").append(entry).append("\n");
                        rank++;
                    }
                    
                    embed.setDescription(sb.length() > 0 ? sb.toString() : "*Aucune donnée*");
                    event.getChannel().sendMessageEmbeds(embed.build()).queue();
                });
                return;
            }
            case "kills", "pvp" -> {
                embed.setTitle("⚔️ Top Kills PvP");
            }
            case "playtime", "time" -> {
                embed.setTitle("⏱️ Top Temps de Jeu");
            }
            default -> {
                embed.setTitle("📊 Leaderboards Disponibles");
                embed.setDescription("""
                    `!leaderboard money` - Top richesse
                    `!leaderboard kills` - Top kills PvP
                    `!leaderboard playtime` - Top temps de jeu
                    """);
            }
        }
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }
    
    private List<String> getTopBalances(int limit) {
        List<String> result = new ArrayList<>();
        
        // Query database for top balances
        try (var conn = plugin.getDatabaseManager().getConnection();
             var stmt = conn.prepareStatement(
                 "SELECT username, balance FROM kc_players ORDER BY balance DESC LIMIT ?")) {
            
            stmt.setInt(1, limit);
            var rs = stmt.executeQuery();
            
            EconomyModule eco = plugin.getModuleManager().getModule(EconomyModule.class);
            
            while (rs.next()) {
                String name = rs.getString("username");
                double balance = rs.getDouble("balance");
                result.add("**" + name + "** - " + (eco != null ? eco.format(balance) : balance + "$"));
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to get top balances", e);
        }
        
        return result;
    }

    private void handleLinkCommand(MessageReceivedEvent event, String[] args) {
        if (args.length < 2) {
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("🔗 Lier son compte")
                    .setDescription("""
                        **Comment lier votre compte:**
                        1. Connectez-vous sur le serveur Minecraft
                        2. Tapez `/link` en jeu pour obtenir un code
                        3. Utilisez `!link <code>` ici
                        """)
                    .setColor(Color.BLUE);
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
            return;
        }
        
        String code = args[1];
        String discordId = event.getAuthor().getId();
        
        // Find player with this pending code
        UUID playerUuid = null;
        for (Map.Entry<UUID, String> entry : pendingLinks.entrySet()) {
            if (entry.getValue().equals(code)) {
                playerUuid = entry.getKey();
                break;
            }
        }
        
        if (playerUuid == null) {
            event.getChannel().sendMessage("❌ Code invalide ou expiré.").queue();
            return;
        }
        
        // Link accounts
        linkedAccounts.put(discordId, playerUuid);
        pendingLinks.remove(playerUuid);
        
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
        event.getChannel().sendMessage("✅ Compte lié à **" + player.getName() + "**!").queue();
    }
    
    /**
     * Generate a link code for a player
     */
    public String generateLinkCode(UUID playerUuid) {
        String code = String.format("%06d", new Random().nextInt(1000000));
        pendingLinks.put(playerUuid, code);
        
        // Expire code after 5 minutes
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            pendingLinks.remove(playerUuid, code);
        }, 20L * 60 * 5);
        
        return code;
    }

    private void sendStatsEmbed(MessageReceivedEvent event, String[] args) {
        String playerName = args.length > 1 ? args[1] : null;
        
        if (playerName == null) {
            // Check if sender has linked account
            UUID linkedUuid = linkedAccounts.get(event.getAuthor().getId());
            if (linkedUuid != null) {
                playerName = Bukkit.getOfflinePlayer(linkedUuid).getName();
            }
        }
        
        if (playerName == null) {
            event.getChannel().sendMessage("❌ Usage: `!stats <joueur>` ou liez votre compte avec `!link`").queue();
            return;
        }
        
        final String finalName = playerName;
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(finalName);
            KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(offlinePlayer.getUniqueId());
            
            if (kPlayer == null) {
                event.getChannel().sendMessage("❌ Joueur non trouvé.").queue();
                return;
            }
            
            EconomyModule eco = plugin.getModuleManager().getModule(EconomyModule.class);
            
            double kills = kPlayer.getStat("kills");
            double deaths = kPlayer.getStat("deaths");
            double kdr = deaths == 0 ? kills : kills / deaths;
            
            EmbedBuilder embed = new EmbedBuilder()
                    .setTitle("📊 Stats de " + finalName)
                    .setColor(Color.CYAN)
                    .addField("💰 Solde", eco != null ? eco.format(kPlayer.getBalance()) : kPlayer.getBalance() + "$", true)
                    .addField("⏱️ Temps de jeu", kPlayer.getPlayTimeFormatted(), true)
                    .addField("⚔️ Kills", String.valueOf((int) kills), true)
                    .addField("☠️ Morts", String.valueOf((int) deaths), true)
                    .addField("📈 K/D", String.format("%.2f", kdr), true)
                    .addField("🐉 Mobs tués", String.valueOf((int) kPlayer.getStat("mob_kills")), true)
                    .setTimestamp(Instant.now());
            
            event.getChannel().sendMessageEmbeds(embed.build()).queue();
        });
    }

    private void sendEventsEmbed(MessageReceivedEvent event) {
        EventsModule events = plugin.getModuleManager().getModule(EventsModule.class);
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("🎉 Événements")
                .setColor(Color.MAGENTA)
                .setTimestamp(Instant.now());
        
        var activeEvents = events.getActiveEvents();
        if (activeEvents.isEmpty()) {
            embed.setDescription("*Aucun événement en cours*\n\nLes événements automatiques ont lieu à:\n• 12:00 - Drop Party\n• 18:00 - Drop Party\n• 14:00 & 20:00 - Double XP\n• 19:00 - Double Money");
        } else {
            StringBuilder sb = new StringBuilder("**Événements actifs:**\n");
            for (var e : activeEvents) {
                String emoji = switch (e.type()) {
                    case DROP_PARTY -> "🎁";
                    case DOUBLE_XP -> "⚡";
                    case DOUBLE_MONEY -> "💰";
                    default -> "🎮";
                };
                sb.append(emoji).append(" ").append(e.type().name().replace("_", " ")).append("\n");
            }
            embed.setDescription(sb.toString());
        }
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void sendTpsEmbed(MessageReceivedEvent event) {
        // Check admin role
        if (!hasAdminRole(event)) {
            event.getChannel().sendMessage("❌ Vous n'avez pas la permission d'utiliser cette commande.").queue();
            return;
        }
        
        double[] tps = Bukkit.getTPS();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📊 TPS du Serveur")
                .addField("1 min", String.format("%.2f", tps[0]), true)
                .addField("5 min", String.format("%.2f", tps[1]), true)
                .addField("15 min", String.format("%.2f", tps[2]), true)
                .setColor(tps[0] >= 18 ? Color.GREEN : tps[0] >= 15 ? Color.YELLOW : Color.RED)
                .setTimestamp(Instant.now());
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private void sendHelpEmbed(MessageReceivedEvent event) {
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("📖 Commandes du Bot")
                .setColor(Color.BLUE)
                .addField("📊 `!status`", "Affiche le statut du serveur", false)
                .addField("👥 `!players`", "Liste les joueurs en ligne", false)
                .addField("🏆 `!leaderboard <type>`", "Classements (money, kills, playtime)", false)
                .addField("📈 `!stats <joueur>`", "Statistiques d'un joueur", false)
                .addField("🎉 `!events`", "Événements actifs et programmés", false)
                .addField("🔗 `!link <code>`", "Lier son compte Minecraft", false)
                .addField("⚙️ `!tps`", "TPS du serveur (admin)", false)
                .setTimestamp(Instant.now());
        
        event.getChannel().sendMessageEmbeds(embed.build()).queue();
    }

    private boolean hasAdminRole(MessageReceivedEvent event) {
        if (adminRoleId == null || adminRoleId.isEmpty()) return true;
        if (event.getMember() == null) return false;
        
        return event.getMember().getRoles().stream()
                .anyMatch(role -> role.getId().equals(adminRoleId));
    }

    /**
     * Send a message to the server channel
     */
    public void sendServerMessage(String title, String description, Color color) {
        if (!enabled || serverChannelId == null || serverChannelId.isEmpty()) return;
        
        TextChannel channel = jda.getTextChannelById(serverChannelId);
        if (channel == null) return;
        
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle(title)
                .setDescription(description)
                .setColor(color)
                .setTimestamp(Instant.now());
        
        channel.sendMessageEmbeds(embed.build()).queue();
    }

    /**
     * Send a plain message
     */
    public void sendServerMessage(String message) {
        if (!enabled || serverChannelId == null || serverChannelId.isEmpty()) return;
        
        TextChannel channel = jda.getTextChannelById(serverChannelId);
        if (channel != null) {
            channel.sendMessage(message).queue();
        }
    }

    /**
     * Send player join message
     */
    public void sendJoinMessage(Player player) {
        sendServerMessage("🟢 **" + player.getName() + "** a rejoint le serveur");
    }

    /**
     * Send player leave message
     */
    public void sendLeaveMessage(Player player) {
        sendServerMessage("🔴 **" + player.getName() + "** a quitté le serveur");
    }

    /**
     * Broadcast a chat message from Minecraft to Discord
     */
    public void broadcastChat(Player player, String message) {
        sendServerMessage("**" + player.getName() + "**: " + message);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public JDA getJda() {
        return jda;
    }
    
    public Map<String, UUID> getLinkedAccounts() {
        return linkedAccounts;
    }
}
