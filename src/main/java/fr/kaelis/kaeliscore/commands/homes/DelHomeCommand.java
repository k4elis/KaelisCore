package fr.kaelis.kaeliscore.commands.homes;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.homes.HomesModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DelHome command to delete homes
 */
public class DelHomeCommand extends BaseCommand {

    public DelHomeCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        
        if (args.length == 0) {
            plugin.getMessageManager().send(player, "homes.delete-usage");
            return;
        }

        HomesModule homes = plugin.getModuleManager().getModule(HomesModule.class);
        String homeName = args[0];

        homes.deleteHome(player.getUniqueId(), homeName).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    plugin.getMessageManager().send(player, "homes.deleted", "name", homeName);
                } else {
                    plugin.getMessageManager().send(player, "homes.not-found", "name", homeName);
                }
            });
        });
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1 && sender instanceof Player player) {
            HomesModule homes = plugin.getModuleManager().getModule(HomesModule.class);
            try {
                return homes.getHomes(player.getUniqueId()).get().keySet().stream()
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            } catch (Exception e) {
                return super.tabComplete(sender, args);
            }
        }
        return super.tabComplete(sender, args);
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.homes.delete";
    }
}
