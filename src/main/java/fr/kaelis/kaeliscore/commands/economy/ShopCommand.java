package fr.kaelis.kaeliscore.commands.economy;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.economy.ShopGui;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Shop command to open shop GUI
 */
public class ShopCommand extends BaseCommand {

    public ShopCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        String category = args.length > 0 ? args[0] : null;
        plugin.getGuiManager().open(player, new ShopGui(plugin, category));
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.economy.shop";
    }
}
