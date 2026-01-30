package fr.kaelis.kaeliscore.commands.warps;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.warps.WarpsModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

/**
 * DelWarp command to delete warps
 */
public class DelWarpCommand extends BaseCommand {

    public DelWarpCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);

        if (args.length == 0) {
            plugin.getMessageManager().send(player, "warps.delwarp-usage");
            return;
        }

        String warpName = args[0];
        WarpsModule warps = plugin.getModuleManager().getModule(WarpsModule.class);

        if (warps.getWarp(warpName) == null) {
            plugin.getMessageManager().send(player, "warps.not-found", "name", warpName);
            return;
        }

        warps.deleteWarp(warpName).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    plugin.getMessageManager().send(player, "warps.deleted", "name", warpName);
                } else {
                    plugin.getMessageManager().send(player, "warps.delete-failed", "name", warpName);
                }
            });
        });
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            WarpsModule warps = plugin.getModuleManager().getModule(WarpsModule.class);
            return warps.getAllWarps().stream()
                    .map(WarpsModule.Warp::name)
                    .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.warps.delete";
    }
}
