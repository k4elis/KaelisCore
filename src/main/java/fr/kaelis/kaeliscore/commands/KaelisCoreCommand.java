package fr.kaelis.kaeliscore.commands;

import fr.kaelis.kaeliscore.KaelisCore;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main KaelisCore admin command
 */
public class KaelisCoreCommand extends BaseCommand {

    public KaelisCoreCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            showHelp(sender);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload" -> {
                plugin.getConfigManager().reloadAll();
                plugin.getMessageManager().reload();
                sender.sendMessage(KaelisCore.colorize("<green>Configuration reloaded!"));
            }
            case "info" -> {
                sender.sendMessage(KaelisCore.colorize("<gradient:#6366f1:#8b5cf6><bold>KaelisCore</bold></gradient>"));
                sender.sendMessage(KaelisCore.colorize("<gray>Version: <white>" + plugin.getDescription().getVersion()));
                sender.sendMessage(KaelisCore.colorize("<gray>Author: <white>" + plugin.getDescription().getAuthors()));
                sender.sendMessage(KaelisCore.colorize("<gray>Modules: <white>" + plugin.getModuleManager().getModules().size()));
            }
            case "modules" -> {
                sender.sendMessage(KaelisCore.colorize("<gradient:#6366f1:#8b5cf6><bold>Loaded Modules</bold></gradient>"));
                plugin.getModuleManager().getModules().forEach((name, module) -> {
                    String status = module.isEnabled() ? "<green>✓" : "<red>✗";
                    sender.sendMessage(KaelisCore.colorize(status + " <gray>" + name));
                });
            }
            default -> showHelp(sender);
        }
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(KaelisCore.colorize("<gradient:#6366f1:#8b5cf6><bold>KaelisCore Commands</bold></gradient>"));
        sender.sendMessage(KaelisCore.colorize("<yellow>/kc reload <gray>- Reload configuration"));
        sender.sendMessage(KaelisCore.colorize("<yellow>/kc info <gray>- Plugin information"));
        sender.sendMessage(KaelisCore.colorize("<yellow>/kc modules <gray>- List loaded modules"));
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("reload", "info", "modules").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.admin";
    }

    @Override
    protected boolean requiresPlayer() {
        return false;
    }
}
