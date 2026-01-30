package fr.kaelis.kaeliscore.commands.economy;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Balance command to check money
 */
public class BalanceCommand extends BaseCommand {

    public BalanceCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        EconomyModule economy = plugin.getModuleManager().getModule(EconomyModule.class);
        
        if (args.length > 0 && sender.hasPermission("kaeliscore.economy.balance.others")) {
            // Check another player's balance
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getMessageManager().send(player, "general.player-not-found");
                return;
            }
            
            double balance = economy.getBalance(target);
            plugin.getMessageManager().send(player, "economy.balance-other",
                "player", target.getName(),
                "balance", economy.format(balance));
        } else {
            // Check own balance
            double balance = economy.getBalance(player);
            plugin.getMessageManager().send(player, "economy.balance",
                "balance", economy.format(balance));
        }
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.economy.balance";
    }
}
