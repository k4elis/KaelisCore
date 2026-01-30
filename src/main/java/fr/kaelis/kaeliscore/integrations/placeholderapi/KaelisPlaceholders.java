package fr.kaelis.kaeliscore.integrations.placeholderapi;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import fr.kaelis.kaeliscore.modules.stats.StatsModule;
import fr.kaelis.kaeliscore.player.KaelisPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * PlaceholderAPI expansion for KaelisCore
 */
public class KaelisPlaceholders extends PlaceholderExpansion {

    private final KaelisCore plugin;

    public KaelisPlaceholders(KaelisCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "kaeliscore";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Kaelis";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return null;

        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(player.getUniqueId());
        EconomyModule economy = plugin.getModuleManager().getModule(EconomyModule.class);
        StatsModule stats = plugin.getModuleManager().getModule(StatsModule.class);

        // Economy placeholders
        if (params.equalsIgnoreCase("balance")) {
            return economy != null ? economy.formatNumber(kPlayer.getBalance()) : String.valueOf(kPlayer.getBalance());
        }
        if (params.equalsIgnoreCase("balance_formatted")) {
            return economy != null ? economy.format(kPlayer.getBalance()) : String.valueOf(kPlayer.getBalance());
        }
        if (params.equalsIgnoreCase("balance_raw")) {
            return String.valueOf(kPlayer.getBalance());
        }

        // Time placeholders
        if (params.equalsIgnoreCase("playtime")) {
            return kPlayer.getPlayTimeFormatted();
        }
        if (params.equalsIgnoreCase("playtime_hours")) {
            return String.valueOf(kPlayer.getPlayTime() / 3600000);
        }
        if (params.equalsIgnoreCase("playtime_minutes")) {
            return String.valueOf(kPlayer.getPlayTime() / 60000);
        }

        // Stats placeholders
        if (params.equalsIgnoreCase("kills")) {
            return String.valueOf((int) kPlayer.getStat(StatsModule.KILLS));
        }
        if (params.equalsIgnoreCase("deaths")) {
            return String.valueOf((int) kPlayer.getStat(StatsModule.DEATHS));
        }
        if (params.equalsIgnoreCase("kdr")) {
            if (player.isOnline()) {
                return String.format("%.2f", stats.getKDRatio(player.getPlayer()));
            }
            double kills = kPlayer.getStat(StatsModule.KILLS);
            double deaths = kPlayer.getStat(StatsModule.DEATHS);
            return String.format("%.2f", deaths == 0 ? kills : kills / deaths);
        }
        if (params.equalsIgnoreCase("mob_kills")) {
            return String.valueOf((int) kPlayer.getStat(StatsModule.MOB_KILLS));
        }
        if (params.equalsIgnoreCase("blocks_broken")) {
            return String.valueOf((int) kPlayer.getStat(StatsModule.BLOCKS_BROKEN));
        }
        if (params.equalsIgnoreCase("blocks_placed")) {
            return String.valueOf((int) kPlayer.getStat(StatsModule.BLOCKS_PLACED));
        }

        // Custom stat placeholder
        if (params.startsWith("stat_")) {
            String statKey = params.substring(5);
            return String.valueOf(kPlayer.getStat(statKey));
        }

        return null;
    }
}
