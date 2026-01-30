package fr.kaelis.kaeliscore.commands.quests;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.commands.BaseCommand;
import fr.kaelis.kaeliscore.gui.AbstractGui;
import fr.kaelis.kaeliscore.gui.GuiItem;
import fr.kaelis.kaeliscore.modules.quests.QuestsModule;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Quest command
 */
public class QuestCommand extends BaseCommand {

    public QuestCommand(KaelisCore plugin) {
        super(plugin);
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = getPlayer(sender);
        QuestsModule quests = plugin.getModuleManager().getModule(QuestsModule.class);

        // Open quests GUI
        plugin.getGuiManager().open(player, new QuestsGui(plugin, quests));
    }

    @Override
    protected String getPermission() {
        return "kaeliscore.quests.view";
    }

    /**
     * Inner class for Quests GUI
     */
    private static class QuestsGui extends AbstractGui {
        private final QuestsModule questsModule;

        public QuestsGui(KaelisCore plugin, QuestsModule questsModule) {
            super(plugin, 4, plugin.getMessageManager().get("quests.gui-title"));
            this.questsModule = questsModule;
        }

        @Override
        protected void build(Player player) {
            fillBorder(Material.GRAY_STAINED_GLASS_PANE);

            questsModule.getPlayerProgress(player.getUniqueId()).thenAccept(progress -> {
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    int slot = 10;

                    for (QuestsModule.Quest quest : questsModule.getAllQuests()) {
                        if (slot > 25) break;
                        if (slot == 17 || slot == 18) slot = 19; // Skip border

                        QuestsModule.QuestProgress qp = progress.get(quest.id().toLowerCase());
                        int currentProgress = qp != null ? qp.progress() : 0;
                        boolean completed = qp != null && qp.completed();

                        List<Component> lore = new ArrayList<>();
                        lore.add(plugin.getMessageManager().parse("<gray>" + quest.description()));
                        lore.add(Component.empty());
                        lore.add(plugin.getMessageManager().parse("<yellow>Progress: <white>" + currentProgress + "/" + quest.amount()));
                        
                        // Progress bar
                        int progressPercent = (int) ((double) currentProgress / quest.amount() * 10);
                        StringBuilder progressBar = new StringBuilder("<green>");
                        for (int i = 0; i < 10; i++) {
                            if (i < progressPercent) {
                                progressBar.append("█");
                            } else {
                                progressBar.append("<gray>█");
                            }
                        }
                        lore.add(plugin.getMessageManager().parse(progressBar.toString()));
                        
                        lore.add(Component.empty());
                        if (completed) {
                            lore.add(plugin.getMessageManager().parse("<green>✓ Completed!"));
                        } else {
                            lore.add(plugin.getMessageManager().parse("<yellow>Rewards:"));
                            if (quest.moneyReward() > 0) {
                                lore.add(plugin.getMessageManager().parse("<gray>- 💰 " + quest.moneyReward()));
                            }
                            if (quest.xpReward() > 0) {
                                lore.add(plugin.getMessageManager().parse("<gray>- ✨ " + quest.xpReward() + " XP"));
                            }
                        }

                        Material icon = completed ? Material.LIME_DYE : quest.icon();
                        ItemStack item = createItem(icon,
                            plugin.getMessageManager().parse((completed ? "<green>" : "<gold>") + quest.name()), lore);

                        setItem(slot, new GuiItem(item));
                        inventory.setItem(slot, item);

                        slot++;
                    }
                });
            });
        }
    }
}
