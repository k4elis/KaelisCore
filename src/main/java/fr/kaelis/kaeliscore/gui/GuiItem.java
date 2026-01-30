package fr.kaelis.kaeliscore.gui;

import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

/**
 * Represents an item in a GUI with optional click handler
 */
public class GuiItem {

    private final ItemStack itemStack;
    private final Consumer<AbstractGui.ClickContext> clickHandler;

    public GuiItem(ItemStack itemStack, Consumer<AbstractGui.ClickContext> clickHandler) {
        this.itemStack = itemStack;
        this.clickHandler = clickHandler;
    }

    public GuiItem(ItemStack itemStack) {
        this(itemStack, null);
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public Consumer<AbstractGui.ClickContext> getClickHandler() {
        return clickHandler;
    }
}
