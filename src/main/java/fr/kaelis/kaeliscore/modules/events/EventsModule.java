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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Events module for automatic server events
 */
public class EventsModule extends AbstractModule {

    private final Map<String, ServerEvent> activeEvents = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> eventParticipants = new ConcurrentHashMap<>();
    private final List<ScheduledEvent> scheduledEvents = new ArrayList<>();
    private BukkitTask schedulerTask;

    public EventsModule(KaelisCore plugin) {
        super(plugin, "Events", "events");
    }

    @Override
    public void onEnable() {
        loadScheduledEvents();
        
        // Start event scheduler - check every minute
        schedulerTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkScheduledEvents, 
            20L * 60, 20L * 60);
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

    private void loadScheduledEvents() {
        var config = getConfig().getConfigurationSection("scheduled-events");
        if (config == null) return;

        for (String eventId : config.getKeys(false)) {
            var eventConfig = config.getConfigurationSection(eventId);
            if (eventConfig == null) continue;

            String typeStr = eventConfig.getString("type", "drop_party");
            EventType type = EventType.valueOf(typeStr.toUpperCase());
            List<String> times = eventConfig.getStringList("times");
            int duration = eventConfig.getInt("duration", 10);
            boolean enabled = eventConfig.getBoolean("enabled", true);

            if (enabled && !times.isEmpty()) {
                scheduledEvents.add(new ScheduledEvent(eventId, type, times, duration));
                plugin.getLogger().info("Scheduled event: " + eventId + " at " + times);
            }
        }
    }

    private void checkScheduledEvents() {
        LocalTime now = LocalTime.now();
        String currentTime = now.format(DateTimeFormatter.ofPattern("HH:mm"));

        for (ScheduledEvent scheduled : scheduledEvents) {
            if (scheduled.times().contains(currentTime) && !isEventTypeActive(scheduled.type())) {
                // Start the event
                plugin.getLogger().info("Starting scheduled event: " + scheduled.id());
                switch (scheduled.type()) {
                    case DROP_PARTY -> {
                        // Use spawn or world spawn
                        Location loc = Bukkit.getWorlds().get(0).getSpawnLocation();
                        List<ItemStack> items = generateDropPartyItems();
                        startDropParty(loc, scheduled.duration(), items);
                    }
                    case DOUBLE_XP -> startDoubleXP(scheduled.duration());
                    case DOUBLE_MONEY -> startDoubleMoney(scheduled.duration());
                    default -> {}
                }
            }
        }
    }

    private List<ItemStack> generateDropPartyItems() {
        List<ItemStack> items = new ArrayList<>();
        Random random = new Random();
        
        // Common items
        for (int i = 0; i < 20; i++) {
            items.add(new ItemStack(Material.DIAMOND, random.nextInt(3) + 1));
            items.add(new ItemStack(Material.IRON_INGOT, random.nextInt(10) + 5));
            items.add(new ItemStack(Material.GOLD_INGOT, random.nextInt(5) + 1));
            items.add(new ItemStack(Material.EMERALD, random.nextInt(3) + 1));
        }
        
        // Rare items
        for (int i = 0; i < 5; i++) {
            items.add(new ItemStack(Material.DIAMOND_BLOCK, 1));
            items.add(new ItemStack(Material.NETHERITE_INGOT, 1));
        }
        
        // Equipment
        items.add(new ItemStack(Material.DIAMOND_SWORD, 1));
        items.add(new ItemStack(Material.DIAMOND_PICKAXE, 1));
        items.add(new ItemStack(Material.DIAMOND_HELMET, 1));
        items.add(new ItemStack(Material.DIAMOND_CHESTPLATE, 1));
        
        Collections.shuffle(items);
        return items;
    }

    private boolean isEventTypeActive(EventType type) {
        return activeEvents.values().stream().anyMatch(e -> e.type() == type);
    }

    /**
     * Start a drop party event
     */
    public void startDropParty(Location location, int duration, List<ItemStack> items) {
        String eventId = "drop_party_" + System.currentTimeMillis();
        ServerEvent event = new ServerEvent(eventId, EventType.DROP_PARTY, location, duration, new HashMap<>());
        activeEvents.put(eventId, event);

        // Broadcast start
        broadcastEvent("<gold><bold>🎉 DROP PARTY!</bold></gold> <yellow>Rendez-vous au spawn pour des items gratuits!");

        // Play announcement sound
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1f, 1f);
        }

        // Schedule item drops
        Random random = new Random();
        List<ItemStack> itemsCopy = new ArrayList<>(items);
        int itemsPerDrop = Math.max(1, itemsCopy.size() / (duration / 2));
        
        for (int i = 0; i < duration; i += 2) {
            final int index = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                if (!activeEvents.containsKey(eventId)) return;
                
                for (int j = 0; j < itemsPerDrop && !itemsCopy.isEmpty(); j++) {
                    ItemStack item = itemsCopy.remove(random.nextInt(itemsCopy.size()));
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

        broadcastEvent("<gold><bold>⚡ DOUBLE XP!</bold></gold> <yellow>Gagnez le double d'XP pendant " + durationMinutes + " minutes!");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        }

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

        broadcastEvent("<gold><bold>💰 DOUBLE MONEY!</bold></gold> <yellow>Gagnez le double d'argent pendant " + durationMinutes + " minutes!");

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_YES, 1f, 1f);
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> endEvent(eventId), durationMinutes * 60 * 20L);
    }

    private void broadcastEvent(String message) {
        var chatModule = plugin.getModuleManager().getModule(fr.kaelis.kaeliscore.modules.chat.ChatModule.class);
        if (chatModule != null) {
            chatModule.broadcastPrefixed(message);
        } else {
            Bukkit.broadcast(plugin.getMessageManager().parse(plugin.getMessageManager().get("prefix") + message));
        }
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

        broadcastEvent("<gray>L'événement <yellow>" + eventName + " <gray>est terminé!");
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
    public record ScheduledEvent(String id, EventType type, List<String> times, int duration) {}
}
