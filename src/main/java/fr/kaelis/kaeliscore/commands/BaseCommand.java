package fr.kaelis.kaeliscore.commands;

import fr.kaelis.kaeliscore.KaelisCore;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

/**
 * Base class for all commands
 */
public abstract class BaseCommand implements CommandExecutor, TabCompleter {

    protected final KaelisCore plugin;

    public BaseCommand(KaelisCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (requiresPlayer() && !(sender instanceof Player)) {
            plugin.getMessageManager().send((Player) sender, "general.player-only");
            return true;
        }
        
        if (getPermission() != null && !sender.hasPermission(getPermission())) {
            if (sender instanceof Player player) {
                plugin.getMessageManager().send(player, "general.no-permission");
            } else {
                sender.sendMessage("You don't have permission to use this command.");
            }
            return true;
        }

        execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (getPermission() != null && !sender.hasPermission(getPermission())) {
            return Collections.emptyList();
        }
        return tabComplete(sender, args);
    }

    /**
     * Execute the command
     */
    protected abstract void execute(CommandSender sender, String[] args);

    /**
     * Tab complete the command
     */
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }

    /**
     * Get the required permission
     */
    protected String getPermission() {
        return null;
    }

    /**
     * Whether the command requires a player
     */
    protected boolean requiresPlayer() {
        return true;
    }

    /**
     * Get player from sender
     */
    protected Player getPlayer(CommandSender sender) {
        return sender instanceof Player ? (Player) sender : null;
    }
}
