package fr.kaelis.kaeliscore.modules.homes;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.gui.AbstractGui;
import fr.kaelis.kaeliscore.gui.GuiItem;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * GUI for viewing and managing homes
 */
public class HomesGui extends AbstractGui {

    private final HomesModule homesModule;
    private Map<String, Location> homes;

    public HomesGui(KaelisCore plugin) {
        super(plugin, 3, plugin.getMessageManager().get("homes.gui-title"));
        this.homesModule = plugin.getModuleManager().getModule(HomesModule.class);
    }

    @Override
    protected void build(Player player) {
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // Load homes asynchronously and update GUI
        homesModule.getHomes(player.getUniqueId()).thenAccept(loadedHomes -> {
            this.homes = loadedHomes;
            
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                int slot = 10;
                for (Map.Entry<String, Location> entry : homes.entrySet()) {
                    if (slot > 16) break;
                    
                    String name = entry.getKey();
                    Location loc = entry.getValue();
                    
                    List<Component> lore = new ArrayList<>();
                    lore.add(plugin.getMessageManager().parse("<gray>World: <white>" + loc.getWorld().getName()));
                    lore.add(plugin.getMessageManager().parse("<gray>Position: <white>" + 
                        String.format("%.0f, %.0f, %.0f", loc.getX(), loc.getY(), loc.getZ())));
                    lore.add(Component.empty());
                    lore.add(plugin.getMessageManager().parse("<yellow>Left-click to teleport"));
                    lore.add(plugin.getMessageManager().parse("<red>Right-click to delete"));
                    
                    ItemStack item = createItem(
                        Material.RED_BED,
                        plugin.getMessageManager().parse("<green>" + name),
                        lore
                    );
                    
                    setItem(slot, item, ctx -> {
                        if (ctx.isLeftClick()) {
                            ctx.close();
                            homesModule.teleportToHome(player, name);
                        } else if (ctx.isRightClick()) {
                            homesModule.deleteHome(player.getUniqueId(), name).thenAccept(success -> {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    if (success) {
                                        plugin.getMessageManager().send(player, "homes.deleted", "name", name);
                                        refresh(player);
                                    } else {
                                        plugin.getMessageManager().send(player, "homes.delete-failed", "name", name);
                                    }
                                });
                            });
                        }
                    });
                    
                    // Update the inventory
                    inventory.setItem(slot, item);
                    slot++;
                }
                
                // Add "Create Home" button
                int maxHomes = homesModule.getMaxHomes(player);
                int currentHomes = homes.size();
                
                if (currentHomes < maxHomes) {
                    ItemStack createItem = createItem(
                        Material.EMERALD,
                        plugin.getMessageManager().parse("<green>Create New Home"),
                        List.of(
                            plugin.getMessageManager().parse("<gray>Homes: <white>" + currentHomes + "/" + maxHomes),
                            Component.empty(),
                            plugin.getMessageManager().parse("<yellow>Use /sethome <name> to create")
                        )
                    );
                    setItem(22, new GuiItem(createItem));
                    inventory.setItem(22, createItem);
                }
            });
        });
    }
}
