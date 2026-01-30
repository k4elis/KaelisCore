package fr.kaelis.kaeliscore.commands.warps;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.warps.WarpsModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * SetWarp command to create warps
 */
public class SetWarpCommand extends BaseCommand {

    public SetWarpCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);

        if (args.length == 0) {
            plugin.getMessageManager().send(player, "warps.setwarp-usage");
            return;
        }

        String warpName = args[0];
        
        // Validate name
        if (!warpName.matches("^[a-zA-Z0-9_-]+$")) {
            plugin.getMessageManager().send(player, "warps.invalid-name");
            return;
        }

        if (warpName.length() > 32) {
            plugin.getMessageManager().send(player, "warps.name-too-long");
            return;
        }

        // Check if warp exists
        WarpsModule warps = plugin.getModuleManager().getModule(WarpsModule.class);
        if (warps.getWarp(warpName) != null) {
            plugin.getMessageManager().send(player, "warps.already-exists", "name", warpName);
            return;
        }

        // Optional permission and category
        String permission = args.length > 1 ? args[1] : null;
        String category = args.length > 2 ? args[2] : null;

        warps.createWarp(warpName, player.getLocation(), permission, category).thenAccept(success -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (success) {
                    plugin.getMessageManager().send(player, "warps.created", "name", warpName);
                } else {
                    plugin.getMessageManager().send(player, "warps.create-failed", "name", warpName);
                }
            });
        });
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.warps.create";
    }
}
