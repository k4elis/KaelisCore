package fr.kaelis.kaeliscore.gui;

import fr.kaelis.kaeliscore.KaelisCore;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * A paginated GUI that supports multiple pages
 */
public abstract class PaginatedGui extends AbstractGui {

    protected int currentPage = 0;
    protected List<?> items = new ArrayList<>();
    
    // Slots for pagination items
    protected int[] contentSlots;
    protected int previousPageSlot = -1;
    protected int nextPageSlot = -1;
    protected int pageInfoSlot = -1;

    public PaginatedGui(KaelisCore plugin, int rows, Component title) {
        super(plugin, rows, title);
        
        // Default content slots (middle area)
        if (rows >= 3) {
            contentSlots = calculateContentSlots(rows);
        } else {
            contentSlots = new int[rows * 9];
            for (int i = 0; i < contentSlots.length; i++) {
                contentSlots[i] = i;
            }
        }
    }

    private int[] calculateContentSlots(int rows) {
        // For 3+ rows, use middle area (exclude first and last row, and first/last column)
        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row < rows - 1; row++) {
            for (int col = 1; col < 8; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    protected void build(Player player) {
        // Build base GUI
        buildBase(player);
        
        // Load items for this GUI
        this.items = loadItems(player);
        
        // Build pagination
        buildPagination(player);
    }

    /**
     * Build the base GUI (borders, decorations, etc)
     */
    protected abstract void buildBase(Player player);

    /**
     * Load items to display in the paginated area
     */
    protected abstract List<?> loadItems(Player player);

    /**
     * Build the item for a specific content item
     */
    protected abstract GuiItem buildItem(Player player, Object item, int index);

    protected void buildPagination(Player player) {
        int totalPages = getTotalPages();
        int startIndex = currentPage * contentSlots.length;
        
        // Fill content slots
        for (int i = 0; i < contentSlots.length; i++) {
            int itemIndex = startIndex + i;
            if (itemIndex < items.size()) {
                GuiItem guiItem = buildItem(player, items.get(itemIndex), itemIndex);
                setItem(contentSlots[i], guiItem);
            }
        }
        
        // Previous page button
        if (previousPageSlot >= 0) {
            if (currentPage > 0) {
                ItemStack prevItem = createItem(
                    Material.ARROW,
                    plugin.getMessageManager().get("gui.previous-page"),
                    List.of(plugin.getMessageManager().get("gui.page-info", 
                        "current", String.valueOf(currentPage),
                        "total", String.valueOf(totalPages)))
                );
                setItem(previousPageSlot, prevItem, ctx -> {
                    currentPage--;
                    refresh(player);
                });
            } else {
                setItem(previousPageSlot, createItem(Material.GRAY_STAINED_GLASS_PANE, Component.empty()), null);
            }
        }
        
        // Next page button
        if (nextPageSlot >= 0) {
            if (currentPage < totalPages - 1) {
                ItemStack nextItem = createItem(
                    Material.ARROW,
                    plugin.getMessageManager().get("gui.next-page"),
                    List.of(plugin.getMessageManager().get("gui.page-info",
                        "current", String.valueOf(currentPage + 2),
                        "total", String.valueOf(totalPages)))
                );
                setItem(nextPageSlot, nextItem, ctx -> {
                    currentPage++;
                    refresh(player);
                });
            } else {
                setItem(nextPageSlot, createItem(Material.GRAY_STAINED_GLASS_PANE, Component.empty()), null);
            }
        }
        
        // Page info
        if (pageInfoSlot >= 0) {
            ItemStack pageInfo = createItem(
                Material.PAPER,
                plugin.getMessageManager().get("gui.page-indicator",
                    "current", String.valueOf(currentPage + 1),
                    "total", String.valueOf(totalPages))
            );
            setItem(pageInfoSlot, new GuiItem(pageInfo));
        }
    }

    public int getTotalPages() {
        return Math.max(1, (int) Math.ceil((double) items.size() / contentSlots.length));
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setPage(int page) {
        this.currentPage = Math.max(0, Math.min(page, getTotalPages() - 1));
    }

    public void setContentSlots(int[] slots) {
        this.contentSlots = slots;
    }

    public void setPreviousPageSlot(int slot) {
        this.previousPageSlot = slot;
    }

    public void setNextPageSlot(int slot) {
        this.nextPageSlot = slot;
    }

    public void setPageInfoSlot(int slot) {
        this.pageInfoSlot = slot;
    }
}
