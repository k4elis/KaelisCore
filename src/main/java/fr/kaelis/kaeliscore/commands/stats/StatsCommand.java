package fr.kaelis.kaeliscore.commands.stats;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.gui.AbstractGui;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import fr.kaelis.kaeliscore.modules.stats.StatsModule;
import fr.kaelis.kaeliscore.player.KaelisPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Statistic;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;

/**
 * Stats command
 */
public class StatsCommand extends BaseCommand {

    public StatsCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        Player target = player;
        
        if (args.length > 0 && player.hasPermission("kaeliscore.stats.others")) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                plugin.getMessageManager().send(player, "general.player-not-found");
                return;
            }
        }

        // Open stats GUI
        plugin.getGuiManager().open(player, new StatsGui(plugin, target));
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.stats.view";
    }

    /**
     * Inner class for Stats GUI
     */
    private static class StatsGui extends AbstractGui {
        private final Player target;

        public StatsGui(KaelisCore plugin, Player target) {
            super(plugin, 4, plugin.getMessageManager().parse("<gradient:#6366f1:#8b5cf6>" + target.getName() + "'s Profile</gradient>"));
            this.target = target;
        }

        @Override
        protected void build(Player player) {
            fillBorder(Material.GRAY_STAINED_GLASS_PANE);

            KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(target);
            StatsModule stats = plugin.getModuleManager().getModule(StatsModule.class);
            EconomyModule economy = plugin.getModuleManager().getModule(EconomyModule.class);

            // Player head
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(target);
            meta.displayName(plugin.getMessageManager().parse("<gold>" + target.getName()));
            List<Component> headLore = new ArrayList<>();
            headLore.add(plugin.getMessageManager().parse("<gray>First join: <white>" + formatDate(kPlayer.getFirstJoin())));
            headLore.add(plugin.getMessageManager().parse("<gray>Play time: <white>" + kPlayer.getPlayTimeFormatted()));
            meta.lore(headLore);
            head.setItemMeta(meta);
            setItem(4, head, null);
            inventory.setItem(4, head);

            // Economy stats
            ItemStack economyItem = createItem(Material.GOLD_INGOT,
                plugin.getMessageManager().parse("<yellow>Economy"),
                List.of(
                    plugin.getMessageManager().parse("<gray>Balance: <white>" + economy.format(kPlayer.getBalance()))
                ));
            setItem(11, economyItem, null);
            inventory.setItem(11, economyItem);

            // Combat stats
            ItemStack combatItem = createItem(Material.DIAMOND_SWORD,
                plugin.getMessageManager().parse("<red>Combat"),
                List.of(
                    plugin.getMessageManager().parse("<gray>Kills: <white>" + (int) kPlayer.getStat(StatsModule.KILLS)),
                    plugin.getMessageManager().parse("<gray>Deaths: <white>" + (int) kPlayer.getStat(StatsModule.DEATHS)),
                    plugin.getMessageManager().parse("<gray>K/D: <white>" + String.format("%.2f", stats.getKDRatio(target))),
                    plugin.getMessageManager().parse("<gray>Mob kills: <white>" + (int) kPlayer.getStat(StatsModule.MOB_KILLS))
                ));
            setItem(12, combatItem, null);
            inventory.setItem(12, combatItem);

            // Mining stats
            ItemStack miningItem = createItem(Material.DIAMOND_PICKAXE,
                plugin.getMessageManager().parse("<aqua>Mining"),
                List.of(
                    plugin.getMessageManager().parse("<gray>Blocks broken: <white>" + target.getStatistic(Statistic.MINE_BLOCK)),
                    plugin.getMessageManager().parse("<gray>Blocks placed: <white>" + (int) kPlayer.getStat(StatsModule.BLOCKS_PLACED))
                ));
            setItem(13, miningItem, null);
            inventory.setItem(13, miningItem);

            // Exploration stats
            ItemStack explorationItem = createItem(Material.COMPASS,
                plugin.getMessageManager().parse("<green>Exploration"),
                List.of(
                    plugin.getMessageManager().parse("<gray>Distance walked: <white>" + String.format("%.0f", kPlayer.getStat(StatsModule.DISTANCE_WALKED)) + " blocks"),
                    plugin.getMessageManager().parse("<gray>Logins: <white>" + (int) kPlayer.getStat(StatsModule.LOGINS))
                ));
            setItem(14, explorationItem, null);
            inventory.setItem(14, explorationItem);

            // Fishing & trading
            ItemStack fishingItem = createItem(Material.FISHING_ROD,
                plugin.getMessageManager().parse("<blue>Fishing & Trading"),
                List.of(
                    plugin.getMessageManager().parse("<gray>Fish caught: <white>" + (int) kPlayer.getStat(StatsModule.FISH_CAUGHT)),
                    plugin.getMessageManager().parse("<gray>Villager trades: <white>" + (int) kPlayer.getStat(StatsModule.TRADES))
                ));
            setItem(15, fishingItem, null);
            inventory.setItem(15, fishingItem);
        }

        private String formatDate(long timestamp) {
            if (timestamp == 0) return "Never";
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
            return sdf.format(new java.util.Date(timestamp));
        }
    }
}
