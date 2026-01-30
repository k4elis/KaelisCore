package fr.kaelis.kaeliscore.config;

import fr.kaelis.kaeliscore.KaelisCore;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages all messages with full customization support
 * Uses MiniMessage format for modern formatting
 */
public class MessageManager {

    private final KaelisCore plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration messages;
    private File messagesFile;
    
    // Prefix cache
    private String prefix;

    public MessageManager(KaelisCore plugin) {
        this.plugin = plugin;
        loadMessages();
    }

    private void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);

        // Load defaults
        InputStream defaultStream = plugin.getResource("messages.yml");
        if (defaultStream != null) {
            YamlConfiguration defaultMessages = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            messages.setDefaults(defaultMessages);
        }
        
        // Cache prefix
        this.prefix = messages.getString("prefix", "<gradient:#6366f1:#8b5cf6>[KaelisCore]</gradient> ");
    }

    /**
     * Get a raw message string from config
     */
    public String getRaw(String path) {
        return messages.getString(path, "<red>Message not found: " + path + "</red>");
    }

    /**
     * Get a raw message string with prefix
     */
    public String getRawPrefixed(String path) {
        return prefix + getRaw(path);
    }

    /**
     * Get a component message
     */
    public Component get(String path) {
        return miniMessage.deserialize(getRaw(path));
    }

    /**
     * Get a component message with prefix
     */
    public Component getPrefixed(String path) {
        return miniMessage.deserialize(getRawPrefixed(path));
    }

    /**
     * Get a component message with placeholders
     */
    public Component get(String path, TagResolver... resolvers) {
        return miniMessage.deserialize(getRaw(path), resolvers);
    }

    /**
     * Get a component message with prefix and placeholders
     */
    public Component getPrefixed(String path, TagResolver... resolvers) {
        return miniMessage.deserialize(getRawPrefixed(path), resolvers);
    }

    /**
     * Get a component message with simple string placeholders
     */
    public Component get(String path, String... replacements) {
        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                builder.resolver(Placeholder.parsed(replacements[i], replacements[i + 1]));
            }
        }
        return miniMessage.deserialize(getRaw(path), builder.build());
    }

    /**
     * Get a component message with prefix and simple string placeholders
     */
    public Component getPrefixed(String path, String... replacements) {
        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                builder.resolver(Placeholder.parsed(replacements[i], replacements[i + 1]));
            }
        }
        return miniMessage.deserialize(getRawPrefixed(path), builder.build());
    }

    /**
     * Send a message to a player
     */
    public void send(Player player, String path) {
        player.sendMessage(getPrefixed(path));
    }

    /**
     * Send a message to a player with placeholders
     */
    public void send(Player player, String path, String... replacements) {
        player.sendMessage(getPrefixed(path, replacements));
    }

    /**
     * Send a message to a player with tag resolvers
     */
    public void send(Player player, String path, TagResolver... resolvers) {
        player.sendMessage(getPrefixed(path, resolvers));
    }

    /**
     * Get a list of messages
     */
    public List<String> getList(String path) {
        return messages.getStringList(path);
    }

    /**
     * Get a list of components
     */
    public List<Component> getComponentList(String path) {
        return getList(path).stream()
                .map(miniMessage::deserialize)
                .toList();
    }

    /**
     * Get prefix
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Get prefix as component
     */
    public Component getPrefixComponent() {
        return miniMessage.deserialize(prefix);
    }

    /**
     * Reload messages
     */
    public void reload() {
        loadMessages();
    }

    /**
     * Save messages
     */
    public void save() {
        try {
            messages.save(messagesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save messages.yml", e);
        }
    }

    /**
     * Parse a string with MiniMessage
     */
    public Component parse(String message) {
        return miniMessage.deserialize(message);
    }

    /**
     * Parse a string with MiniMessage and placeholders
     */
    public Component parse(String message, String... replacements) {
        TagResolver.Builder builder = TagResolver.builder();
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 < replacements.length) {
                builder.resolver(Placeholder.parsed(replacements[i], replacements[i + 1]));
            }
        }
        return miniMessage.deserialize(message, builder.build());
    }
}
