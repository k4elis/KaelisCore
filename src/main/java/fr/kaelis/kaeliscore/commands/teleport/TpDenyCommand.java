package fr.kaelis.kaeliscore.commands.teleport;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.modules.teleport.TeleportModule;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * TPA Deny command
 */
public class TpDenyCommand extends BaseCommand {

    public TpDenyCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        TeleportModule teleport = plugin.getModuleManager().getModule(TeleportModule.class);
        teleport.denyTpa(player);
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.teleport.tpa";
    }
}
