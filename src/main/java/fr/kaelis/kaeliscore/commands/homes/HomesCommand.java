package fr.kaelis.kaeliscore.commands.homes;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.homes.HomesGui;
import fr.kaelis.kaeliscore.modules.homes.HomesModule;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * Homes command to list all homes
 */
public class HomesCommand extends BaseCommand {

    public HomesCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        
        // Open GUI
        plugin.getGuiManager().open(player, new HomesGui(plugin));
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.homes.teleport";
    }
}
