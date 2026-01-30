package fr.kaelis.kaeliscore.integrations.discord;

import fr.kaelis.kaeliscore.KaelisCore;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.awt.*;
import java.time.Instant;
import java.util.logging.Level;

/**
 * Discord bot integration
 */
public class DiscordBot extends ListenerAdapter {

    private final KaelisCore plugin;
    private JDA jda;
    private String serverChannelId;
    private String consoleChannelId;
    private String adminRoleId;
    private boolean enabled;

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
            sendServerMessage("🟢 **Server Started!**", "The Minecraft server is now online.", Color.GREEN);
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Failed to start Discord bot", e);
            enabled = false;
        }
    }

    public void shutdown() {
        if (jda != null) {
            // Send shutdown message
            sendServerMessage("🔴 **Server Stopped!**", "The Minecraft server is shutting down.", Color.RED);
            
            jda.shutdown();
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
                Bukkit.broadcast(KaelisCore.colorize(
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
            case "players", "list" -> {
                int online = Bukkit.getOnlinePlayers().size();
                int max = Bukkit.getMaxPlayers();
                
                StringBuilder players = new StringBuilder();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (!players.isEmpty()) players.append(", ");
                    players.append(player.getName());
                }
                
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("🎮 Online Players")
                        .setDescription(players.length() > 0 ? players.toString() : "No players online")
                        .setColor(Color.CYAN)
                        .setFooter(online + "/" + max + " players")
                        .setTimestamp(Instant.now());
                
                event.getChannel().sendMessageEmbeds(embed.build()).queue();
            }
            case "tps" -> {
                // Check admin role
                if (!hasAdminRole(event)) {
                    event.getChannel().sendMessage("❌ You don't have permission to use this command.").queue();
                    return;
                }
                
                double[] tps = Bukkit.getTPS();
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("📊 Server TPS")
                        .addField("1 min", String.format("%.2f", tps[0]), true)
                        .addField("5 min", String.format("%.2f", tps[1]), true)
                        .addField("15 min", String.format("%.2f", tps[2]), true)
                        .setColor(tps[0] >= 18 ? Color.GREEN : tps[0] >= 15 ? Color.YELLOW : Color.RED)
                        .setTimestamp(Instant.now());
                
                event.getChannel().sendMessageEmbeds(embed.build()).queue();
            }
            case "help" -> {
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("📖 Bot Commands")
                        .addField("!players", "Show online players", false)
                        .addField("!tps", "Show server TPS (admin only)", false)
                        .addField("!help", "Show this help message", false)
                        .setColor(Color.BLUE)
                        .setTimestamp(Instant.now());
                
                event.getChannel().sendMessageEmbeds(embed.build()).queue();
            }
        }
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
        sendServerMessage("🟢 **" + player.getName() + "** joined the server");
    }

    /**
     * Send player leave message
     */
    public void sendLeaveMessage(Player player) {
        sendServerMessage("🔴 **" + player.getName() + "** left the server");
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
}
