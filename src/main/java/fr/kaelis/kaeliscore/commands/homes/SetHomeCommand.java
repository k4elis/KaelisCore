package fr.kaelis.kaeliscore.commands.homes;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.homes.HomesModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * SetHome command to create homes
 */
public class SetHomeCommand extends BaseCommand {

    public SetHomeCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        HomesModule homes = plugin.getModuleManager().getModule(HomesModule.class);
        
        String homeName = args.length > 0 ? args[0] : "home";
        
        // Validate name
        if (!homeName.matches("^[a-zA-Z0-9_-]+$")) {
            plugin.getMessageManager().send(player, "homes.invalid-name");
            return;
        }
        
        if (homeName.length() > 32) {
            plugin.getMessageManager().send(player, "homes.name-too-long");
            return;
        }

        homes.setHome(player, homeName).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    plugin.getMessageManager().send(player, "homes.set", "name", homeName);
                } else {
                    plugin.getMessageManager().send(player, "homes.limit-reached",
                        "max", String.valueOf(homes.getMaxHomes(player)));
                }
            });
        });
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.homes.set";
    }
}
