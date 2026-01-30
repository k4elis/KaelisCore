package fr.kaelis.kaeliscore.modules;

import fr.kaelis.kaeliscore.KaelisCore;
import org.bukkit.configuration.file.FileConfiguration;

/**
 * Abstract base class for all modules
 */
public abstract class AbstractModule {

    protected final KaelisCore plugin;
    protected final String name;
    protected final String configName;
    protected boolean enabled;

    public AbstractModule(KaelisCore plugin, String name, String configName) {
        this.plugin = plugin;
        this.name = name;
        this.configName = configName;
        
        // Check if module is enabled in config
        FileConfiguration config = plugin.getConfigManager().getConfig(configName);
        this.enabled = config != null && config.getBoolean("enabled", true);
    }

    /**
     * Called when the module is enabled
     */
    public abstract void onEnable();

    /**
     * Called when the module is disabled
     */
    public abstract void onDisable();

    /**
     * Get the module's configuration
     */
    public FileConfiguration getConfig() {
        return plugin.getConfigManager().getConfig(configName);
    }

    /**
     * Save the module's configuration
     */
    public void saveConfig() {
        plugin.getConfigManager().saveConfig(configName);
    }

    /**
     * Reload the module's configuration
     */
    public void reloadConfig() {
        plugin.getConfigManager().reloadConfig(configName);
    }

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public KaelisCore getPlugin() {
        return plugin;
    }
}
