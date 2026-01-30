package fr.kaelis.kaeliscore.integrations.vault;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.economy.EconomyModule;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;

import java.util.ArrayList;
import java.util.List;

/**
 * Vault integration for economy and permissions
 */
public class VaultHook {

    private final KaelisCore plugin;
    private Economy economy;
    private Permission permission;
    private Chat chat;

    public VaultHook(KaelisCore plugin) {
        this.plugin = plugin;
    }

    public void setup() {
        // Register our economy provider
        Bukkit.getServicesManager().register(Economy.class, new KaelisEconomy(), plugin, ServicePriority.Highest);
        plugin.getLogger().info("Registered KaelisCore economy with Vault");

        // Get permission provider
        RegisteredServiceProvider<Permission> permRsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (permRsp != null) {
            permission = permRsp.getProvider();
        }

        // Get chat provider
        RegisteredServiceProvider<Chat> chatRsp = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (chatRsp != null) {
            chat = chatRsp.getProvider();
        }
    }

    public Economy getEconomy() {
        return economy;
    }

    public Permission getPermission() {
        return permission;
    }

    public Chat getChat() {
        return chat;
    }

    /**
     * Vault Economy implementation using KaelisCore
     */
    private class KaelisEconomy implements Economy {

        @Override
        public boolean isEnabled() {
            return plugin.isEnabled();
        }

        @Override
        public String getName() {
            return "KaelisCore";
        }

        @Override
        public boolean hasBankSupport() {
            return false;
        }

        @Override
        public int fractionalDigits() {
            return 2;
        }

        @Override
        public String format(double amount) {
            EconomyModule eco = plugin.getModuleManager().getModule(EconomyModule.class);
            return eco != null ? eco.format(amount) : String.format("%.2f", amount);
        }

        @Override
        public String currencyNamePlural() {
            return "coins";
        }

        @Override
        public String currencyNameSingular() {
            return "coin";
        }

        @Override
        public boolean hasAccount(OfflinePlayer player) {
            return true;
        }

        @Override
        public boolean hasAccount(String playerName) {
            return true;
        }

        @Override
        public boolean hasAccount(OfflinePlayer player, String worldName) {
            return true;
        }

        @Override
        public boolean hasAccount(String playerName, String worldName) {
            return true;
        }

        @Override
        public double getBalance(OfflinePlayer player) {
            EconomyModule eco = plugin.getModuleManager().getModule(EconomyModule.class);
            return eco != null ? eco.getBalance(player) : 0;
        }

        @Override
        public double getBalance(String playerName) {
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            return getBalance(player);
        }

        @Override
        public double getBalance(OfflinePlayer player, String world) {
            return getBalance(player);
        }

        @Override
        public double getBalance(String playerName, String world) {
            return getBalance(playerName);
        }

        @Override
        public boolean has(OfflinePlayer player, double amount) {
            return getBalance(player) >= amount;
        }

        @Override
        public boolean has(String playerName, double amount) {
            return getBalance(playerName) >= amount;
        }

        @Override
        public boolean has(OfflinePlayer player, String worldName, double amount) {
            return has(player, amount);
        }

        @Override
        public boolean has(String playerName, String worldName, double amount) {
            return has(playerName, amount);
        }

        @Override
        public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
            if (player.isOnline()) {
                EconomyModule eco = plugin.getModuleManager().getModule(EconomyModule.class);
                if (eco != null && eco.withdraw(player.getPlayer(), amount)) {
                    return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
                }
            }
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Player not online or insufficient funds");
        }

        @Override
        public EconomyResponse withdrawPlayer(String playerName, double amount) {
            return withdrawPlayer(Bukkit.getOfflinePlayer(playerName), amount);
        }

        @Override
        public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
            return withdrawPlayer(player, amount);
        }

        @Override
        public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
            return withdrawPlayer(playerName, amount);
        }

        @Override
        public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
            if (player.isOnline()) {
                EconomyModule eco = plugin.getModuleManager().getModule(EconomyModule.class);
                if (eco != null) {
                    eco.deposit(player.getPlayer(), amount);
                    return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
                }
            }
            return new EconomyResponse(0, getBalance(player), EconomyResponse.ResponseType.FAILURE, "Player not online");
        }

        @Override
        public EconomyResponse depositPlayer(String playerName, double amount) {
            return depositPlayer(Bukkit.getOfflinePlayer(playerName), amount);
        }

        @Override
        public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
            return depositPlayer(player, amount);
        }

        @Override
        public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
            return depositPlayer(playerName, amount);
        }

        // Bank methods - not supported
        @Override
        public EconomyResponse createBank(String name, OfflinePlayer player) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse createBank(String name, String player) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse deleteBank(String name) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse bankBalance(String name) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse bankHas(String name, double amount) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse bankWithdraw(String name, double amount) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse bankDeposit(String name, double amount) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse isBankOwner(String name, String playerName) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse isBankMember(String name, OfflinePlayer player) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public EconomyResponse isBankMember(String name, String playerName) {
            return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED, "Banks not supported");
        }

        @Override
        public List<String> getBanks() {
            return new ArrayList<>();
        }

        @Override
        public boolean createPlayerAccount(OfflinePlayer player) {
            return true;
        }

        @Override
        public boolean createPlayerAccount(String playerName) {
            return true;
        }

        @Override
        public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
            return true;
        }

        @Override
        public boolean createPlayerAccount(String playerName, String worldName) {
            return true;
        }
    }
}
