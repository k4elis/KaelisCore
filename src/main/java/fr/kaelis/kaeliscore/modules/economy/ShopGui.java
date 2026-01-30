package fr.kaelis.kaeliscore.modules.economy;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.gui.AbstractGui;
import fr.kaelis.kaeliscore.gui.GuiItem;
import fr.kaelis.kaeliscore.gui.PaginatedGui;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Shop GUI for buying and selling items
 */
public class ShopGui extends PaginatedGui {

    private final EconomyModule economyModule;
    private String currentCategory;
    private final List<String> categories;

    public ShopGui(KaelisCore plugin, String category) {
        super(plugin, 6, plugin.getMessageManager().get("shop.title"));
        this.economyModule = plugin.getModuleManager().getModule(EconomyModule.class);
        this.currentCategory = category;
        
        // Load categories
        this.categories = new ArrayList<>();
        ConfigurationSection categoriesSection = plugin.getConfigManager().getConfig("economy").getConfigurationSection("shop.categories");
        if (categoriesSection != null) {
            categories.addAll(categoriesSection.getKeys(false));
        }
        
        // Set pagination slots
        setContentSlots(new int[]{
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34,
            37, 38, 39, 40, 41, 42, 43
        });
        setPreviousPageSlot(45);
        setNextPageSlot(53);
        setPageInfoSlot(49);
    }

    @Override
    protected void buildBase(Player player) {
        // Fill border with dark glass
        fillBorder(Material.GRAY_STAINED_GLASS_PANE);
        
        // Add category buttons in bottom row
        int slot = 46;
        for (String category : categories) {
            if (slot > 52) break;
            if (slot == 49) slot++; // Skip page info slot
            
            ConfigurationSection catConfig = plugin.getConfigManager().getConfig("economy")
                    .getConfigurationSection("shop.categories." + category);
            
            if (catConfig != null) {
                Material icon = Material.matchMaterial(catConfig.getString("icon", "CHEST"));
                if (icon == null) icon = Material.CHEST;
                
                String displayName = catConfig.getString("name", category);
                boolean isSelected = category.equals(currentCategory);
                
                ItemStack item = createItem(
                    isSelected ? Material.LIME_STAINED_GLASS_PANE : icon,
                    plugin.getMessageManager().parse(isSelected ? "<green><bold>" + displayName : "<gray>" + displayName),
                    List.of(plugin.getMessageManager().parse("<yellow>Click to view " + displayName))
                );
                
                final String cat = category;
                setItem(slot, item, ctx -> {
                    currentCategory = cat;
                    currentPage = 0;
                    ctx.refresh();
                });
            }
            slot++;
        }
        
        // Balance display
        double balance = economyModule.getBalance(player);
        ItemStack balanceItem = createItem(
            Material.GOLD_INGOT,
            plugin.getMessageManager().get("shop.balance-title"),
            List.of(
                plugin.getMessageManager().parse("<gray>Your balance: <yellow>" + economyModule.format(balance))
            )
        );
        setItem(4, new GuiItem(balanceItem));
    }

    @Override
    protected List<?> loadItems(Player player) {
        List<ShopItem> items = new ArrayList<>();
        
        if (currentCategory == null && !categories.isEmpty()) {
            currentCategory = categories.get(0);
        }
        
        if (currentCategory != null) {
            ConfigurationSection itemsSection = plugin.getConfigManager().getConfig("economy")
                    .getConfigurationSection("shop.categories." + currentCategory + ".items");
            
            if (itemsSection != null) {
                for (String key : itemsSection.getKeys(false)) {
                    ConfigurationSection itemConfig = itemsSection.getConfigurationSection(key);
                    if (itemConfig != null) {
                        Material material = Material.matchMaterial(itemConfig.getString("material", "STONE"));
                        if (material != null) {
                            items.add(new ShopItem(
                                key,
                                material,
                                itemConfig.getString("name", key),
                                itemConfig.getDouble("buy-price", 0),
                                itemConfig.getDouble("sell-price", 0),
                                itemConfig.getInt("amount", 1)
                            ));
                        }
                    }
                }
            }
        }
        
        return items;
    }

    @Override
    protected GuiItem buildItem(Player player, Object item, int index) {
        ShopItem shopItem = (ShopItem) item;
        
        List<Component> lore = new ArrayList<>();
        if (shopItem.buyPrice() > 0) {
            lore.add(plugin.getMessageManager().parse("<green>Buy: " + economyModule.format(shopItem.buyPrice())));
        }
        if (shopItem.sellPrice() > 0) {
            lore.add(plugin.getMessageManager().parse("<red>Sell: " + economyModule.format(shopItem.sellPrice())));
        }
        lore.add(Component.empty());
        lore.add(plugin.getMessageManager().parse("<yellow>Left-click to buy"));
        lore.add(plugin.getMessageManager().parse("<yellow>Right-click to sell"));
        
        ItemStack itemStack = new ItemStack(shopItem.material(), shopItem.amount());
        var meta = itemStack.getItemMeta();
        meta.displayName(plugin.getMessageManager().parse("<white>" + shopItem.displayName()));
        meta.lore(lore);
        itemStack.setItemMeta(meta);
        
        return new GuiItem(itemStack, ctx -> {
            if (ctx.isLeftClick() && shopItem.buyPrice() > 0) {
                // Buy
                handleBuy(player, shopItem);
            } else if (ctx.isRightClick() && shopItem.sellPrice() > 0) {
                // Sell
                handleSell(player, shopItem);
            }
            ctx.refresh();
        });
    }

    private void handleBuy(Player player, ShopItem shopItem) {
        if (!economyModule.has(player, shopItem.buyPrice())) {
            plugin.getMessageManager().send(player, "shop.not-enough-money");
            return;
        }
        
        if (player.getInventory().firstEmpty() == -1) {
            plugin.getMessageManager().send(player, "shop.inventory-full");
            return;
        }
        
        economyModule.withdraw(player, shopItem.buyPrice());
        player.getInventory().addItem(new ItemStack(shopItem.material(), shopItem.amount()));
        plugin.getMessageManager().send(player, "shop.bought",
            "amount", String.valueOf(shopItem.amount()),
            "item", shopItem.displayName(),
            "price", economyModule.format(shopItem.buyPrice())
        );
    }

    private void handleSell(Player player, ShopItem shopItem) {
        ItemStack toRemove = new ItemStack(shopItem.material(), shopItem.amount());
        
        if (!player.getInventory().containsAtLeast(toRemove, shopItem.amount())) {
            plugin.getMessageManager().send(player, "shop.not-enough-items");
            return;
        }
        
        player.getInventory().removeItem(toRemove);
        economyModule.deposit(player, shopItem.sellPrice());
        plugin.getMessageManager().send(player, "shop.sold",
            "amount", String.valueOf(shopItem.amount()),
            "item", shopItem.displayName(),
            "price", economyModule.format(shopItem.sellPrice())
        );
    }

    private record ShopItem(String id, Material material, String displayName, double buyPrice, double sellPrice, int amount) {}
}
