package fr.kaelis.kaeliscore.listeners;

import fr.kaelis.kaeliscore.KaelisCore;
import org.bukkit.Bukkit;

/**
 * Manages all event listeners
 */
public class ListenerManager {

    private final KaelisCore plugin;

    public ListenerManager(KaelisCore plugin) {
        this.plugin = plugin;
    }

    public void registerListeners() {
        Bukkit.getPluginManager().registerEvents(new PlayerListener(plugin), plugin);
    }
}
