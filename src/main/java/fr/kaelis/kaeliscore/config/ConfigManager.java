package fr.kaelis.kaeliscore.config;

import fr.kaelis.kaeliscore.KaelisCore;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages all configuration files for KaelisCore
 */
public class ConfigManager {

    private final KaelisCore plugin;
    private final Map<String, FileConfiguration> configs = new HashMap<>();
    private final Map<String, File> configFiles = new HashMap<>();

    public ConfigManager(KaelisCore plugin) {
        this.plugin = plugin;
        loadConfigs();
    }

    private void loadConfigs() {
        // Main config
        plugin.saveDefaultConfig();
        configs.put("config", plugin.getConfig());
        configFiles.put("config", new File(plugin.getDataFolder(), "config.yml"));

        // Load other configs
        loadConfig("database");
        loadConfig("economy");
        loadConfig("homes");
        loadConfig("claims");
        loadConfig("kits");
        loadConfig("chat");
        loadConfig("quests");
        loadConfig("stats");
        loadConfig("events");
        loadConfig("antigrief");
        loadConfig("discord");
        loadConfig("gui");
    }

    private void loadConfig(String name) {
        File file = new File(plugin.getDataFolder(), name + ".yml");
        if (!file.exists()) {
            plugin.saveResource(name + ".yml", false);
        }
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Load defaults from jar
        InputStream defaultStream = plugin.getResource(name + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                    new InputStreamReader(defaultStream, StandardCharsets.UTF_8));
            config.setDefaults(defaultConfig);
        }

        configs.put(name, config);
        configFiles.put(name, file);
    }

    public FileConfiguration getConfig() {
        return configs.get("config");
    }

    public FileConfiguration getConfig(String name) {
        return configs.get(name);
    }

    public void saveConfig(String name) {
        FileConfiguration config = configs.get(name);
        File file = configFiles.get(name);
        if (config != null && file != null) {
            try {
                config.save(file);
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config " + name + ".yml", e);
            }
        }
    }

    public void reloadConfig(String name) {
        File file = configFiles.get(name);
        if (file != null && file.exists()) {
            configs.put(name, YamlConfiguration.loadConfiguration(file));
        }
    }

    public void reloadAll() {
        plugin.reloadConfig();
        configs.put("config", plugin.getConfig());
        for (String name : configFiles.keySet()) {
            if (!name.equals("config")) {
                reloadConfig(name);
            }
        }
    }

    public void saveAll() {
        for (String name : configs.keySet()) {
            saveConfig(name);
        }
    }
}
