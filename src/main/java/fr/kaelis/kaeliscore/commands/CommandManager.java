package fr.kaelis.kaeliscore.commands;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.economy.BalanceCommand;
import fr.kaelis.kaeliscore.commands.economy.EconomyCommand;
import fr.kaelis.kaeliscore.commands.economy.PayCommand;
import fr.kaelis.kaeliscore.commands.economy.ShopCommand;
import fr.kaelis.kaeliscore.commands.homes.DelHomeCommand;
import fr.kaelis.kaeliscore.commands.homes.HomeCommand;
import fr.kaelis.kaeliscore.commands.homes.HomesCommand;
import fr.kaelis.kaeliscore.commands.homes.SetHomeCommand;
import fr.kaelis.kaeliscore.commands.kits.KitCommand;
import fr.kaelis.kaeliscore.commands.quests.QuestCommand;
import fr.kaelis.kaeliscore.commands.stats.StatsCommand;
import fr.kaelis.kaeliscore.commands.teleport.*;
import fr.kaelis.kaeliscore.commands.warps.DelWarpCommand;
import fr.kaelis.kaeliscore.commands.warps.SetWarpCommand;
import fr.kaelis.kaeliscore.commands.warps.WarpCommand;
import org.bukkit.command.PluginCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages all plugin commands
 */
public class CommandManager {

    private final KaelisCore plugin;
    private final Map<String, BaseCommand> commands = new HashMap<>();

    public CommandManager(KaelisCore plugin) {
        this.plugin = plugin;
    }

    public void registerCommands() {
        // Main command
        registerCommand("kaeliscore", new KaelisCoreCommand(plugin));
        
        // Economy commands
        registerCommand("balance", new BalanceCommand(plugin));
        registerCommand("pay", new PayCommand(plugin));
        registerCommand("shop", new ShopCommand(plugin));
        registerCommand("economy", new EconomyCommand(plugin));
        
        // Home commands
        registerCommand("home", new HomeCommand(plugin));
        registerCommand("sethome", new SetHomeCommand(plugin));
        registerCommand("delhome", new DelHomeCommand(plugin));
        registerCommand("homes", new HomesCommand(plugin));
        
        // Warp commands
        registerCommand("warp", new WarpCommand(plugin));
        registerCommand("setwarp", new SetWarpCommand(plugin));
        registerCommand("delwarp", new DelWarpCommand(plugin));
        
        // Teleport commands
        registerCommand("spawn", new SpawnCommand(plugin));
        registerCommand("setspawn", new SetSpawnCommand(plugin));
        registerCommand("tpa", new TpaCommand(plugin));
        registerCommand("tpaccept", new TpAcceptCommand(plugin));
        registerCommand("tpdeny", new TpDenyCommand(plugin));
        registerCommand("back", new BackCommand(plugin));
        
        // Kit command
        registerCommand("kit", new KitCommand(plugin));
        
        // Quest command
        registerCommand("quest", new QuestCommand(plugin));
        
        // Stats command
        registerCommand("stats", new StatsCommand(plugin));
    }

    private void registerCommand(String name, BaseCommand command) {
        PluginCommand pluginCommand = plugin.getCommand(name);
        if (pluginCommand != null) {
            pluginCommand.setExecutor(command);
            pluginCommand.setTabCompleter(command);
            commands.put(name, command);
        } else {
            plugin.getLogger().warning("Command not found in plugin.yml: " + name);
        }
    }

    public BaseCommand getCommand(String name) {
        return commands.get(name);
    }
}
