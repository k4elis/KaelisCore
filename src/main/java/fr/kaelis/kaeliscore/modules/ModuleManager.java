package fr.kaelis.kaeliscore.modules;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.antigrief.AntiGriefModule;
import fr.kaelis.kaeliscore.modules.chat.ChatModule;
import fr.kaelis.kaeliscore.modules.claims.ClaimsModule;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import fr.kaelis.kaeliscore.modules.events.EventsModule;
import fr.kaelis.kaeliscore.modules.homes.HomesModule;
import fr.kaelis.kaeliscore.modules.kits.KitsModule;
import fr.kaelis.kaeliscore.modules.quests.QuestsModule;
import fr.kaelis.kaeliscore.modules.ranks.RanksModule;
import fr.kaelis.kaeliscore.modules.scoreboard.ScoreboardModule;
import fr.kaelis.kaeliscore.modules.stats.StatsModule;
import fr.kaelis.kaeliscore.modules.teleport.TeleportModule;
import fr.kaelis.kaeliscore.modules.warps.WarpsModule;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages all plugin modules
 */
public class ModuleManager {

    private final KaelisCore plugin;
    private final Map<String, AbstractModule> modules = new HashMap<>();

    public ModuleManager(KaelisCore plugin) {
        this.plugin = plugin;
    }

    public void loadModules() {
        // Register all modules
        registerModule(new EconomyModule(plugin));
        registerModule(new HomesModule(plugin));
        registerModule(new WarpsModule(plugin));
        registerModule(new TeleportModule(plugin));
        registerModule(new ClaimsModule(plugin));
        registerModule(new KitsModule(plugin));
        registerModule(new ChatModule(plugin));
        registerModule(new StatsModule(plugin));
        registerModule(new QuestsModule(plugin));
        registerModule(new EventsModule(plugin));
        registerModule(new AntiGriefModule(plugin));
        registerModule(new RanksModule(plugin));
        registerModule(new ScoreboardModule(plugin));

        // Enable modules
        for (AbstractModule module : modules.values()) {
            if (module.isEnabled()) {
                try {
                    module.onEnable();
                    plugin.getLogger().info("Module " + module.getName() + " enabled");
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Failed to enable module " + module.getName(), e);
                }
            }
        }
    }

    public void disableModules() {
        for (AbstractModule module : modules.values()) {
            if (module.isEnabled()) {
                try {
                    module.onDisable();
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error disabling module " + module.getName(), e);
                }
            }
        }
        modules.clear();
    }

    private void registerModule(AbstractModule module) {
        modules.put(module.getName().toLowerCase(), module);
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractModule> T getModule(String name) {
        return (T) modules.get(name.toLowerCase());
    }

    @SuppressWarnings("unchecked")
    public <T extends AbstractModule> T getModule(Class<T> moduleClass) {
        for (AbstractModule module : modules.values()) {
            if (moduleClass.isInstance(module)) {
                return (T) module;
            }
        }
        return null;
    }

    public Map<String, AbstractModule> getModules() {
        return modules;
    }
}
