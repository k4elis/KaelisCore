package fr.kaelis.kaeliscore.listeners;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.homes.HomesModule;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Main player event listener
 */
public class PlayerListener implements Listener {

    private final KaelisCore plugin;

    public PlayerListener(KaelisCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        plugin.getPlayerDataManager().handleJoin(event.getPlayer());
        
        // Discord notification
        if (plugin.getDiscordBot() != null) {
            plugin.getDiscordBot().sendJoinMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getPlayerDataManager().handleQuit(event.getPlayer());
        
        // Discord notification
        if (plugin.getDiscordBot() != null) {
            plugin.getDiscordBot().sendLeaveMessage(event.getPlayer());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Cancel teleport if player moves
        if (event.getFrom().getBlockX() != event.getTo().getBlockX() ||
            event.getFrom().getBlockY() != event.getTo().getBlockY() ||
            event.getFrom().getBlockZ() != event.getTo().getBlockZ()) {
            
            HomesModule homes = plugin.getModuleManager().getModule(HomesModule.class);
            if (homes != null) {
                homes.cancelTeleport(event.getPlayer());
            }
        }
    }
}
