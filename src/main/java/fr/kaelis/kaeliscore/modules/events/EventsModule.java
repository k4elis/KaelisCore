package fr.kaelis.kaeliscore.modules.events;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Events module for automatic server events
 */
public class EventsModule extends AbstractModule {

    private final Map<String, ServerEvent> activeEvents = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> eventParticipants = new ConcurrentHashMap<>();
    private BukkitTask schedulerTask;

    public EventsModule(KaelisCore plugin) {
        super(plugin, "Events", "events");
    }

    @Override
    public void onEnable() {
        // Start event scheduler
        int checkInterval = getConfig().getInt("scheduler.check-interval", 300); // 5 minutes default
        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkScheduledEvents, 
            20L * 60, 20L * checkInterval);
    }

    @Override
    public void onDisable() {
        if (schedulerTask != null) {
            schedulerTask.cancel();
        }
        
        // End all active events
        for (ServerEvent event : activeEvents.values()) {
            endEvent(event.id());
        }
    }

    private void checkScheduledEvents() {
        // Check for scheduled events
        // This would read from config and start events at specific times
    }

    /**
     * Start a drop party event
     */
    public void startDropParty(Location location, int duration, List<ItemStack> items) {
        String eventId = "drop_party_" + System.currentTimeMillis();
        ServerEvent event = new ServerEvent(eventId, EventType.DROP_PARTY, location, duration, new HashMap<>());
        activeEvents.put(eventId, event);

        // Broadcast start
        plugin.getModuleManager().getModule(fr.kaelis.kaeliscore.modules.chat.ChatModule.class)
            .broadcastPrefixed("<gold><bold>DROP PARTY!</bold></gold> <yellow>Head to spawn for free items!");

        // Schedule item drops
        Random random = new Random();
        int itemsPerDrop = Math.max(1, items.size() / (duration / 2));
        
        for (int i = 0; i < duration; i += 2) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!activeEvents.containsKey(eventId)) return;
                
                for (int j = 0; j < itemsPerDrop && !items.isEmpty(); j++) {
                    ItemStack item = items.remove(random.nextInt(items.size()));
                    Location dropLoc = location.clone().add(
                        random.nextDouble() * 10 - 5,
                        random.nextDouble() * 3,
                        random.nextDouble() * 10 - 5
                    );
                    location.getWorld().dropItem(dropLoc, item);
                }
                
                // Play sound
                for (Player player : location.getWorld().getPlayers()) {
                    if (player.getLocation().distance(location) < 50) {
                        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 0.5f, 1f);
                    }
                }
            }, i * 20L);
        }

        // Schedule end
        Bukkit.getScheduler().runTaskLater(plugin, () -> endEvent(eventId), duration * 20L);
    }

    /**
     * Start a double XP event
     */
    public void startDoubleXP(int durationMinutes) {
        String eventId = "double_xp_" + System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("multiplier", 2.0);
        
        ServerEvent event = new ServerEvent(eventId, EventType.DOUBLE_XP, null, durationMinutes * 60, data);
        activeEvents.put(eventId, event);

        plugin.getModuleManager().getModule(fr.kaelis.kaeliscore.modules.chat.ChatModule.class)
            .broadcastPrefixed("<gold><bold>DOUBLE XP!</bold></gold> <yellow>Earn double XP for " + durationMinutes + " minutes!");

        // Schedule end
        Bukkit.getScheduler().runTaskLater(plugin, () -> endEvent(eventId), durationMinutes * 60 * 20L);
    }

    /**
     * Start a double money event
     */
    public void startDoubleMoney(int durationMinutes) {
        String eventId = "double_money_" + System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("multiplier", 2.0);
        
        ServerEvent event = new ServerEvent(eventId, EventType.DOUBLE_MONEY, null, durationMinutes * 60, data);
        activeEvents.put(eventId, event);

        plugin.getModuleManager().getModule(fr.kaelis.kaeliscore.modules.chat.ChatModule.class)
            .broadcastPrefixed("<gold><bold>DOUBLE MONEY!</bold></gold> <yellow>Earn double money for " + durationMinutes + " minutes!");

        Bukkit.getScheduler().runTaskLater(plugin, () -> endEvent(eventId), durationMinutes * 60 * 20L);
    }

    /**
     * Check if a multiplier event is active
     */
    public double getXPMultiplier() {
        for (ServerEvent event : activeEvents.values()) {
            if (event.type() == EventType.DOUBLE_XP) {
                return (double) event.data().getOrDefault("multiplier", 1.0);
            }
        }
        return 1.0;
    }

    public double getMoneyMultiplier() {
        for (ServerEvent event : activeEvents.values()) {
            if (event.type() == EventType.DOUBLE_MONEY) {
                return (double) event.data().getOrDefault("multiplier", 1.0);
            }
        }
        return 1.0;
    }

    /**
     * End an event
     */
    public void endEvent(String eventId) {
        ServerEvent event = activeEvents.remove(eventId);
        if (event == null) return;

        String eventName = switch (event.type()) {
            case DROP_PARTY -> "Drop Party";
            case DOUBLE_XP -> "Double XP";
            case DOUBLE_MONEY -> "Double Money";
            case PVP_ARENA -> "PvP Arena";
            case TREASURE_HUNT -> "Treasure Hunt";
        };

        plugin.getModuleManager().getModule(fr.kaelis.kaeliscore.modules.chat.ChatModule.class)
            .broadcastPrefixed("<gray>The <yellow>" + eventName + " <gray>event has ended!");
    }

    /**
     * Get all active events
     */
    public Collection<ServerEvent> getActiveEvents() {
        return activeEvents.values();
    }

    /**
     * Check if an event type is active
     */
    public boolean isEventActive(EventType type) {
        for (ServerEvent event : activeEvents.values()) {
            if (event.type() == type) return true;
        }
        return false;
    }

    public enum EventType {
        DROP_PARTY,
        DOUBLE_XP,
        DOUBLE_MONEY,
        PVP_ARENA,
        TREASURE_HUNT
    }

    public record ServerEvent(String id, EventType type, Location location, int duration, Map<String, Object> data) {}
}
