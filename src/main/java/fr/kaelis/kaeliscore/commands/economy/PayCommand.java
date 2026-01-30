package fr.kaelis.kaeliscore.commands.economy;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Pay command to transfer money
 */
public class PayCommand extends BaseCommand {

    public PayCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        
        if (args.length < 2) {
            plugin.getMessageManager().send(player, "economy.pay-usage");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            plugin.getMessageManager().send(player, "general.player-not-found");
            return;
        }

        if (target.equals(player)) {
            plugin.getMessageManager().send(player, "economy.pay-self");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            plugin.getMessageManager().send(player, "economy.invalid-amount");
            return;
        }

        if (amount <= 0) {
            plugin.getMessageManager().send(player, "economy.invalid-amount");
            return;
        }

        EconomyModule economy = plugin.getModuleManager().getModule(EconomyModule.class);
        
        if (!economy.has(player, amount)) {
            plugin.getMessageManager().send(player, "economy.not-enough-money");
            return;
        }

        if (economy.pay(player, target, amount)) {
            plugin.getMessageManager().send(player, "economy.paid",
                "player", target.getName(),
                "amount", economy.format(amount));
            plugin.getMessageManager().send(target, "economy.received",
                "player", player.getName(),
                "amount", economy.format(amount));
        } else {
            plugin.getMessageManager().send(player, "economy.pay-failed");
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.economy.pay";
    }
}
