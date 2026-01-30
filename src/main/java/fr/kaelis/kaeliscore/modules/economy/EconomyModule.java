package fr.kaelis.kaeliscore.modules.economy;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import fr.kaelis.kaeliscore.player.KaelisPlayer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.logging.Level;

/**
 * Economy module for handling money, transactions, and shop
 */
public class EconomyModule extends AbstractModule {

    private String currencySymbol;
    private String currencyFormat;
    private DecimalFormat decimalFormat;
    private double maxBalance;
    private double minTransaction;

    public EconomyModule(KaelisCore plugin) {
        super(plugin, "Economy", "economy");
    }

    @Override
    public void onEnable() {
        loadConfig();
    }

    @Override
    public void onDisable() {
        // Nothing to clean up
    }

    private void loadConfig() {
        currencySymbol = getConfig().getString("currency.symbol", "💰");
        currencyFormat = getConfig().getString("currency.format", "#,##0.00");
        maxBalance = getConfig().getDouble("limits.max-balance", 1000000000);
        minTransaction = getConfig().getDouble("limits.min-transaction", 0.01);
        decimalFormat = new DecimalFormat(currencyFormat);
    }

    /**
     * Get a player's balance
     */
    public double getBalance(Player player) {
        return plugin.getPlayerDataManager().getPlayer(player).getBalance();
    }

    /**
     * Get a player's balance by UUID
     */
    public double getBalance(OfflinePlayer player) {
        if (player.isOnline()) {
            return getBalance(player.getPlayer());
        }
        // Load from database for offline players
        return plugin.getPlayerDataManager().getPlayer(player.getUniqueId()).getBalance();
    }

    /**
     * Set a player's balance
     */
    public void setBalance(Player player, double amount) {
        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(player);
        double oldBalance = kPlayer.getBalance();
        kPlayer.setBalance(Math.min(amount, maxBalance));
        
        logTransaction(player, "SET", amount - oldBalance, kPlayer.getBalance(), "Balance set by admin");
    }

    /**
     * Add money to a player
     */
    public void deposit(Player player, double amount) {
        if (amount < minTransaction) return;
        
        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(player);
        double newBalance = Math.min(kPlayer.getBalance() + amount, maxBalance);
        kPlayer.setBalance(newBalance);
        
        logTransaction(player, "DEPOSIT", amount, newBalance, "Deposit");
    }

    /**
     * Remove money from a player
     */
    public boolean withdraw(Player player, double amount) {
        if (amount < minTransaction) return false;
        
        KaelisPlayer kPlayer = plugin.getPlayerDataManager().getPlayer(player);
        if (!kPlayer.hasBalance(amount)) {
            return false;
        }
        
        kPlayer.removeBalance(amount);
        logTransaction(player, "WITHDRAW", -amount, kPlayer.getBalance(), "Withdrawal");
        return true;
    }

    /**
     * Transfer money between players
     */
    public boolean pay(Player from, Player to, double amount) {
        if (amount < minTransaction) return false;
        
        KaelisPlayer fromPlayer = plugin.getPlayerDataManager().getPlayer(from);
        KaelisPlayer toPlayer = plugin.getPlayerDataManager().getPlayer(to);
        
        if (!fromPlayer.hasBalance(amount)) {
            return false;
        }
        
        // Check if receiver would exceed max
        if (toPlayer.getBalance() + amount > maxBalance) {
            return false;
        }
        
        fromPlayer.removeBalance(amount);
        toPlayer.addBalance(amount);
        
        logTransaction(from, "PAY_OUT", -amount, fromPlayer.getBalance(), "Payment to " + to.getName());
        logTransaction(to, "PAY_IN", amount, toPlayer.getBalance(), "Payment from " + from.getName());
        
        return true;
    }

    /**
     * Check if player has enough money
     */
    public boolean has(Player player, double amount) {
        return plugin.getPlayerDataManager().getPlayer(player).hasBalance(amount);
    }

    /**
     * Format an amount with currency symbol
     */
    public String format(double amount) {
        return currencySymbol + " " + decimalFormat.format(amount);
    }

    /**
     * Format an amount without symbol
     */
    public String formatNumber(double amount) {
        return decimalFormat.format(amount);
    }

    /**
     * Log a transaction to the database
     */
    private void logTransaction(Player player, String type, double amount, double balanceAfter, String description) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try (Connection conn = plugin.getDatabaseManager().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO kc_transactions (uuid, type, amount, balance_after, description, timestamp) VALUES (?, ?, ?, ?, ?, ?)")) {
                
                stmt.setString(1, player.getUniqueId().toString());
                stmt.setString(2, type);
                stmt.setDouble(3, amount);
                stmt.setDouble(4, balanceAfter);
                stmt.setString(5, description);
                stmt.setLong(6, System.currentTimeMillis());
                stmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.WARNING, "Failed to log transaction", e);
            }
        });
    }

    // Getters
    public String getCurrencySymbol() {
        return currencySymbol;
    }

    public double getMaxBalance() {
        return maxBalance;
    }

    public double getMinTransaction() {
        return minTransaction;
    }
}
