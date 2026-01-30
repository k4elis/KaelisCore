package fr.kaelis.kaeliscore.commands.homes;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.homes.HomesModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Home command to teleport
 */
public class HomeCommand extends BaseCommand {

    public HomeCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        HomesModule homes = plugin.getModuleManager().getModule(HomesModule.class);
        
        String homeName = args.length > 0 ? args[0] : "home";
        homes.teleportToHome(player, homeName);
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
        return "kaeliscore.homes.teleport";
    }
}
