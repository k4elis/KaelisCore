package fr.kaelis.kaeliscore.modules.chat;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Chat module for formatting and management
 */
public class ChatModule extends AbstractModule implements Listener {

    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private String chatFormat;
    private boolean enableColors;
    private boolean enableFormatting;
    private List<Pattern> blockedPatterns;
    private final Map<UUID, Long> chatCooldowns = new HashMap<>();
    private int chatCooldownSeconds;
    private boolean antiSpam;

    public ChatModule(KaelisCore plugin) {
        super(plugin, "Chat", "chat");
    }

    @Override
    public void onEnable() {
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
        // Nothing to clean up
    }

    private void loadConfig() {
        chatFormat = getConfig().getString("format", "<gray>[<player_prefix><player_name><gray>] <white><message>");
        enableColors = getConfig().getBoolean("allow-colors", false);
        enableFormatting = getConfig().getBoolean("allow-formatting", false);
        chatCooldownSeconds = getConfig().getInt("cooldown", 0);
        antiSpam = getConfig().getBoolean("anti-spam", true);

        // Load blocked patterns
        blockedPatterns = new ArrayList<>();
        List<String> blocked = getConfig().getStringList("blocked-patterns");
        for (String pattern : blocked) {
            try {
                blockedPatterns.add(Pattern.compile(pattern, Pattern.CASE_INSENSITIVE));
            } catch (Exception e) {
                plugin.getLogger().warning("Invalid regex pattern: " + pattern);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event) {
        if (!isEnabled()) return;

        Player player = event.getPlayer();
        String message = PlainTextComponentSerializer.plainText().serialize(event.message());

        // Check cooldown
        if (chatCooldownSeconds > 0 && !player.hasPermission("kaeliscore.chat.bypass-cooldown")) {
            Long lastChat = chatCooldowns.get(player.getUniqueId());
            if (lastChat != null) {
                long remaining = (lastChat + (chatCooldownSeconds * 1000L)) - System.currentTimeMillis();
                if (remaining > 0) {
                    event.setCancelled(true);
                    plugin.getMessageManager().send(player, "chat.cooldown",
                        "seconds", String.valueOf(Math.ceil(remaining / 1000.0)));
                    return;
                }
            }
        }

        // Check blocked patterns
        if (!player.hasPermission("kaeliscore.chat.bypass-filter")) {
            for (Pattern pattern : blockedPatterns) {
                if (pattern.matcher(message).find()) {
                    event.setCancelled(true);
                    plugin.getMessageManager().send(player, "chat.blocked");
                    return;
                }
            }
        }

        // Update cooldown
        chatCooldowns.put(player.getUniqueId(), System.currentTimeMillis());

        // Format message
        event.renderer((source, sourceDisplayName, msg, viewer) -> {
            return formatMessage(source, message);
        });
    }

    /**
     * Format a chat message
     */
    public Component formatMessage(Player player, String message) {
        String format = chatFormat;

        // Replace placeholders
        format = format.replace("<player_name>", player.getName());
        format = format.replace("<player_displayname>", PlainTextComponentSerializer.plainText().serialize(player.displayName()));
        
        // Get prefix/suffix from LuckPerms or Vault if available
        String prefix = getPrefix(player);
        String suffix = getSuffix(player);
        
        format = format.replace("<player_prefix>", prefix);
        format = format.replace("<player_suffix>", suffix);

        // Handle message colors
        String processedMessage = message;
        if (player.hasPermission("kaeliscore.chat.color")) {
            // Allow color codes
            processedMessage = message;
        } else if (!enableColors) {
            // Strip color codes from message
            processedMessage = MiniMessage.miniMessage().stripTags(message);
        }

        format = format.replace("<message>", processedMessage);

        return miniMessage.deserialize(format);
    }

    private String getPrefix(Player player) {
        // Try to get from LuckPerms
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                var luckPerms = net.luckperms.api.LuckPermsProvider.get();
                var user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String prefix = user.getCachedData().getMetaData().getPrefix();
                    return prefix != null ? prefix : "";
                }
            } catch (Exception ignored) {}
        }
        
        // Try Vault
        if (plugin.getVaultHook() != null && plugin.getVaultHook().getChat() != null) {
            String prefix = plugin.getVaultHook().getChat().getPlayerPrefix(player);
            return prefix != null ? prefix : "";
        }
        
        return "";
    }

    private String getSuffix(Player player) {
        // Try to get from LuckPerms
        if (Bukkit.getPluginManager().isPluginEnabled("LuckPerms")) {
            try {
                var luckPerms = net.luckperms.api.LuckPermsProvider.get();
                var user = luckPerms.getUserManager().getUser(player.getUniqueId());
                if (user != null) {
                    String suffix = user.getCachedData().getMetaData().getSuffix();
                    return suffix != null ? suffix : "";
                }
            } catch (Exception ignored) {}
        }
        
        // Try Vault
        if (plugin.getVaultHook() != null && plugin.getVaultHook().getChat() != null) {
            String suffix = plugin.getVaultHook().getChat().getPlayerSuffix(player);
            return suffix != null ? suffix : "";
        }
        
        return "";
    }

    /**
     * Send a broadcast message
     */
    public void broadcast(String message) {
        Component component = miniMessage.deserialize(message);
        Bukkit.broadcast(component);
    }

    /**
     * Send a broadcast with prefix
     */
    public void broadcastPrefixed(String message) {
        Component component = miniMessage.deserialize(plugin.getMessageManager().getPrefix() + message);
        Bukkit.broadcast(component);
    }
}
