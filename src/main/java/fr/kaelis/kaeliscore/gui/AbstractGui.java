package fr.kaelis.kaeliscore.gui;

import fr.kaelis.kaeliscore.KaelisCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Abstract base class for all GUIs
 */
public abstract class AbstractGui {

    protected final KaelisCore plugin;
    protected final int rows;
    protected final Component title;
    protected Inventory inventory;
    protected GuiManager.GuiHolder holder;
    
    protected final Map<Integer, GuiItem> items = new HashMap<>();
    protected final Map<Integer, Consumer<ClickContext>> clickHandlers = new HashMap<>();

    public AbstractGui(KaelisCore plugin, int rows, Component title) {
        this.plugin = plugin;
        this.rows = Math.min(6, Math.max(1, rows));
        this.title = title;
    }

    /**
     * Build the GUI contents
     */
    protected abstract void build(Player player);

    /**
     * Open the GUI for a player
     */
    public void open(Player player) {
        holder = new GuiManager.GuiHolder(this);
        inventory = Bukkit.createInventory(holder, rows * 9, title);
        holder.setInventory(inventory);

        items.clear();
        clickHandlers.clear();
        
        build(player);

        // Fill with items
        for (Map.Entry<Integer, GuiItem> entry : items.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItemStack());
        }

        player.openInventory(inventory);
    }

    /**
     * Handle a click event
     */
    public void handleClick(Player player, int slot, ClickType clickType) {
        Consumer<ClickContext> handler = clickHandlers.get(slot);
        if (handler != null) {
            handler.accept(new ClickContext(player, slot, clickType, this));
        }
    }

    /**
     * Called when the GUI is closed
     */
    public void onClose(Player player) {
        // Override in subclasses if needed
    }

    /**
     * Set an item in the GUI
     */
    protected void setItem(int slot, GuiItem item) {
        items.put(slot, item);
        if (item.getClickHandler() != null) {
            clickHandlers.put(slot, item.getClickHandler());
        }
    }

    /**
     * Set an item with a click handler
     */
    protected void setItem(int slot, ItemStack item, Consumer<ClickContext> handler) {
        setItem(slot, new GuiItem(item, handler));
    }

    /**
     * Fill the border with a material
     */
    protected void fillBorder(Material material) {
        ItemStack filler = createItem(material, Component.empty());
        for (int i = 0; i < 9; i++) {
            setItem(i, new GuiItem(filler, null));
            setItem((rows - 1) * 9 + i, new GuiItem(filler, null));
        }
        for (int i = 1; i < rows - 1; i++) {
            setItem(i * 9, new GuiItem(filler, null));
            setItem(i * 9 + 8, new GuiItem(filler, null));
        }
    }

    /**
     * Fill empty slots with a material
     */
    protected void fillEmpty(Material material) {
        ItemStack filler = createItem(material, Component.empty());
        for (int i = 0; i < rows * 9; i++) {
            if (!items.containsKey(i)) {
                setItem(i, new GuiItem(filler, null));
            }
        }
    }

    /**
     * Create a simple item
     */
    protected ItemStack createItem(Material material, Component name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create an item with lore
     */
    protected ItemStack createItem(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name);
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create an item with custom model data
     */
    protected ItemStack createItem(Material material, Component name, List<Component> lore, int customModelData) {
        ItemStack item = createItem(material, name, lore);
        ItemMeta meta = item.getItemMeta();
        meta.setCustomModelData(customModelData);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Refresh the GUI
     */
    public void refresh(Player player) {
        items.clear();
        clickHandlers.clear();
        build(player);
        
        inventory.clear();
        for (Map.Entry<Integer, GuiItem> entry : items.entrySet()) {
            inventory.setItem(entry.getKey(), entry.getValue().getItemStack());
        }
    }

    /**
     * Update a specific slot
     */
    public void updateSlot(int slot, GuiItem item) {
        items.put(slot, item);
        if (item.getClickHandler() != null) {
            clickHandlers.put(slot, item.getClickHandler());
        }
        inventory.setItem(slot, item.getItemStack());
    }

    public Inventory getInventory() {
        return inventory;
    }

    public int getRows() {
        return rows;
    }

    public Component getTitle() {
        return title;
    }

    /**
     * Click context for handlers
     */
    public record ClickContext(Player player, int slot, ClickType clickType, AbstractGui gui) {
        public boolean isLeftClick() {
            return clickType.isLeftClick();
        }

        public boolean isRightClick() {
            return clickType.isRightClick();
        }

        public boolean isShiftClick() {
            return clickType.isShiftClick();
        }

        public void close() {
            player.closeInventory();
        }

        public void refresh() {
            gui.refresh(player);
        }
    }
}
