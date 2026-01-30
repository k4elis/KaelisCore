package fr.kaelis.kaeliscore.commands.economy;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Economy admin command
 */
public class EconomyCommand extends BaseCommand {

    public EconomyCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            showUsage(sender);
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        
        if (target == null) {
            sender.sendMessage(KaelisCore.colorize("<red>Player not found!"));
            return;
        }

        EconomyModule economy = plugin.getModuleManager().getModule(EconomyModule.class);

        switch (action) {
            case "give", "add" -> {
                if (args.length < 3) {
                    showUsage(sender);
                    return;
                }
                double amount = parseAmount(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(KaelisCore.colorize("<red>Invalid amount!"));
                    return;
                }
                economy.deposit(target, amount);
                sender.sendMessage(KaelisCore.colorize("<green>Gave " + economy.format(amount) + " to " + target.getName()));
            }
            case "take", "remove" -> {
                if (args.length < 3) {
                    showUsage(sender);
                    return;
                }
                double amount = parseAmount(args[2]);
                if (amount <= 0) {
                    sender.sendMessage(KaelisCore.colorize("<red>Invalid amount!"));
                    return;
                }
                economy.withdraw(target, amount);
                sender.sendMessage(KaelisCore.colorize("<green>Took " + economy.format(amount) + " from " + target.getName()));
            }
            case "set" -> {
                if (args.length < 3) {
                    showUsage(sender);
                    return;
                }
                double amount = parseAmount(args[2]);
                if (amount < 0) {
                    sender.sendMessage(KaelisCore.colorize("<red>Invalid amount!"));
                    return;
                }
                economy.setBalance(target, amount);
                sender.sendMessage(KaelisCore.colorize("<green>Set " + target.getName() + "'s balance to " + economy.format(amount)));
            }
            case "check" -> {
                double balance = economy.getBalance(target);
                sender.sendMessage(KaelisCore.colorize("<gray>" + target.getName() + "'s balance: <white>" + economy.format(balance)));
            }
            default -> showUsage(sender);
        }
    }

    private void showUsage(CommandSender sender) {
        sender.sendMessage(KaelisCore.colorize("<gold>Economy Admin Commands:"));
        sender.sendMessage(KaelisCore.colorize("<yellow>/eco give <player> <amount>"));
        sender.sendMessage(KaelisCore.colorize("<yellow>/eco take <player> <amount>"));
        sender.sendMessage(KaelisCore.colorize("<yellow>/eco set <player> <amount>"));
        sender.sendMessage(KaelisCore.colorize("<yellow>/eco check <player>"));
    }

    private double parseAmount(String str) {
        try {
            return Double.parseDouble(str);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("give", "take", "set", "check").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.economy.admin";
    }

    @Override
    protected boolean requiresPlayer() {
        return false;
    }
}
