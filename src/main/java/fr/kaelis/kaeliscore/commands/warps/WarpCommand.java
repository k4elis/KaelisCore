package fr.kaelis.kaeliscore.commands.warps;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.gui.AbstractGui;
import fr.kaelis.kaeliscore.gui.GuiItem;
import fr.kaelis.kaeliscore.modules.warps.WarpsModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Warp command to teleport to warps
 */
public class WarpCommand extends BaseCommand {

    public WarpCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        WarpsModule warps = plugin.getModuleManager().getModule(WarpsModule.class);

        if (args.length == 0) {
            // Open warps GUI
            plugin.getGuiManager().open(player, new WarpsGui(plugin, warps));
            return;
        }

        String warpName = args[0];
        warps.teleportToWarp(player, warpName);
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            WarpsModule warps = plugin.getModuleManager().getModule(WarpsModule.class);
            if (sender instanceof Player player) {
                return warps.getAvailableWarps(player).stream()
                        .map(WarpsModule.Warp::name)
                        .filter(name -> name.toLowerCase().startsWith(args[0].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }
        return super.tabComplete(sender, args);
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.warps.use";
    }

    /**
     * Warps GUI
     */
    private static class WarpsGui extends AbstractGui {
        private final WarpsModule warpsModule;

        public WarpsGui(KaelisCore plugin, WarpsModule warpsModule) {
            super(plugin, 4, plugin.getMessageManager().get("warps.gui-title"));
            this.warpsModule = warpsModule;
        }

        @Override
        protected void build(Player player) {
            fillBorder(Material.GRAY_STAINED_GLASS_PANE);

            List<WarpsModule.Warp> available = warpsModule.getAvailableWarps(player);
            int slot = 10;

            for (WarpsModule.Warp warp : available) {
                if (slot > 25) break;
                if (slot == 17 || slot == 18) slot = 19;

                List<Component> lore = new ArrayList<>();
                lore.add(plugin.getMessageManager().parse("<gray>World: <white>" + warp.location().getWorld().getName()));
                lore.add(plugin.getMessageManager().parse("<gray>Position: <white>" + 
                    String.format("%.0f, %.0f, %.0f", warp.location().getX(), warp.location().getY(), warp.location().getZ())));
                if (warp.category() != null && !warp.category().isEmpty()) {
                    lore.add(plugin.getMessageManager().parse("<gray>Category: <yellow>" + warp.category()));
                }
                lore.add(Component.empty());
                lore.add(plugin.getMessageManager().parse("<yellow>Click to teleport"));

                Material icon = getMaterialForCategory(warp.category());
                ItemStack item = createItem(icon,
                    plugin.getMessageManager().parse("<green>" + warp.name()), lore);

                final String warpName = warp.name();
                setItem(slot, item, ctx -> {
                    ctx.close();
                    warpsModule.teleportToWarp(player, warpName);
                });
                inventory.setItem(slot, item);

                slot++;
            }
        }

        private Material getMaterialForCategory(String category) {
            if (category == null) return Material.ENDER_PEARL;
            return switch (category.toLowerCase()) {
                case "shop", "shops" -> Material.EMERALD;
                case "pvp", "arena" -> Material.DIAMOND_SWORD;
                case "spawn", "hub" -> Material.NETHER_STAR;
                case "farm", "farms" -> Material.WHEAT;
                case "mine", "mines" -> Material.DIAMOND_PICKAXE;
                default -> Material.ENDER_PEARL;
            };
        }
    }
}
