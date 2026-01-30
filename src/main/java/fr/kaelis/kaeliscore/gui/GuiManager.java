package fr.kaelis.kaeliscore.gui;

import fr.kaelis.kaeliscore.KaelisCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all GUI menus
 */
public class GuiManager implements Listener {

    private final KaelisCore plugin;
    private final Map<UUID, AbstractGui> openGuis = new HashMap<>();

    public GuiManager(KaelisCore plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /**
     * Open a GUI for a player
     */
    public void open(Player player, AbstractGui gui) {
        // Close any existing GUI
        if (openGuis.containsKey(player.getUniqueId())) {
            openGuis.get(player.getUniqueId()).onClose(player);
        }

        openGuis.put(player.getUniqueId(), gui);
        gui.open(player);
    }

    /**
     * Get the current GUI for a player
     */
    public AbstractGui getGui(Player player) {
        return openGuis.get(player.getUniqueId());
    }

    /**
     * Check if a player has a GUI open
     */
    public boolean hasGuiOpen(Player player) {
        return openGuis.containsKey(player.getUniqueId());
    }

    /**
     * Close a player's GUI
     */
    public void close(Player player) {
        AbstractGui gui = openGuis.remove(player.getUniqueId());
        if (gui != null) {
            gui.onClose(player);
        }
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        AbstractGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        // Check if it's our GUI
        if (event.getInventory().getHolder() instanceof GuiHolder holder) {
            if (holder.getGui() == gui) {
                event.setCancelled(true);
                
                int slot = event.getRawSlot();
                if (slot >= 0 && slot < event.getInventory().getSize()) {
                    gui.handleClick(player, slot, event.getClick());
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        AbstractGui gui = openGuis.get(player.getUniqueId());
        if (gui == null) return;

        if (event.getInventory().getHolder() instanceof GuiHolder holder) {
            if (holder.getGui() == gui) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;

        AbstractGui gui = openGuis.remove(player.getUniqueId());
        if (gui != null) {
            gui.onClose(player);
        }
    }

    /**
     * Custom inventory holder for our GUIs
     */
    public static class GuiHolder implements InventoryHolder {
        private final AbstractGui gui;
        private Inventory inventory;

        public GuiHolder(AbstractGui gui) {
            this.gui = gui;
        }

        public AbstractGui getGui() {
            return gui;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }

        public void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }
    }

    public KaelisCore getPlugin() {
        return plugin;
    }
}
