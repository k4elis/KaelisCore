package fr.kaelis.kaeliscore.commands.kits;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.gui.AbstractGui;
import fr.kaelis.kaeliscore.gui.GuiItem;
import fr.kaelis.kaeliscore.modules.kits.KitsModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Kit command
 */
public class KitCommand extends BaseCommand {

    public KitCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        KitsModule kits = plugin.getModuleManager().getModule(KitsModule.class);

        if (args.length == 0) {
            // Open kits GUI
            plugin.getGuiManager().open(player, new KitsGui(plugin, kits));
            return;
        }

        String kitId = args[0];
        KitsModule.Kit kit = kits.getKit(kitId);
        
        if (kit == null) {
            plugin.getMessageManager().send(player, "kits.not-found");
            return;
        }

        kits.giveKit(player, kitId).thenAccept(result -> {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                switch (result) {
                    case SUCCESS -> plugin.getMessageManager().send(player, "kits.received", "kit", kit.displayName());
                    case NO_PERMISSION -> plugin.getMessageManager().send(player, "kits.no-permission");
                    case ON_COOLDOWN -> kits.getCooldownRemaining(player.getUniqueId(), kitId).thenAccept(cooldown -> {
                        plugin.getServer().getScheduler().runTask(plugin, () -> {
                            plugin.getMessageManager().send(player, "kits.cooldown",
                                "time", kits.formatCooldown(cooldown));
                        });
                    });
                    case INVENTORY_FULL -> plugin.getMessageManager().send(player, "kits.inventory-full");
                    case NOT_FOUND -> plugin.getMessageManager().send(player, "kits.not-found");
                }
            });
        });
    }

    @Override
    protected List<String> tabComplete(CommandSender sender, String[] args) {
        if (args.length == 1) {
            KitsModule kits = plugin.getModuleManager().getModule(KitsModule.class);
            return kits.getAllKits().stream()
                    .map(KitsModule.Kit::id)
                    .filter(id -> id.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return super.tabComplete(sender, args);
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.kits.use";
    }

    /**
     * Inner class for Kits GUI
     */
    private static class KitsGui extends AbstractGui {
        private final KitsModule kitsModule;

        public KitsGui(KaelisCore plugin, KitsModule kitsModule) {
            super(plugin, 3, plugin.getMessageManager().get("kits.gui-title"));
            this.kitsModule = kitsModule;
        }

        @Override
        protected void build(Player player) {
            fillBorder(Material.GRAY_STAINED_GLASS_PANE);

            List<KitsModule.Kit> availableKits = kitsModule.getAvailableKits(player);
            int slot = 10;

            for (KitsModule.Kit kit : availableKits) {
                if (slot > 16) break;

                kitsModule.getCooldownRemaining(player.getUniqueId(), kit.id()).thenAccept(cooldown -> {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        List<Component> lore = new ArrayList<>();
                        lore.add(plugin.getMessageManager().parse("<gray>" + kit.description()));
                        lore.add(Component.empty());
                        
                        if (cooldown > 0) {
                            lore.add(plugin.getMessageManager().parse("<red>Cooldown: " + kitsModule.formatCooldown(cooldown)));
                        } else {
                            lore.add(plugin.getMessageManager().parse("<green>Available!"));
                        }
                        lore.add(Component.empty());
                        lore.add(plugin.getMessageManager().parse("<yellow>Click to claim"));

                        ItemStack item = createItem(kit.icon(),
                            plugin.getMessageManager().parse("<gold>" + kit.displayName()), lore);

                        final int finalSlot = slot;
                        setItem(finalSlot, item, ctx -> {
                            ctx.close();
                            kitsModule.giveKit(player, kit.id()).thenAccept(result -> {
                                plugin.getServer().getScheduler().runTask(plugin, () -> {
                                    if (result == KitsModule.KitResult.SUCCESS) {
                                        plugin.getMessageManager().send(player, "kits.received", "kit", kit.displayName());
                                    }
                                });
                            });
                        });

                        inventory.setItem(finalSlot, item);
                    });
                });

                slot++;
            }
        }
    }
}
