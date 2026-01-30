package fr.kaelis.kaeliscore;

import fr.kaelis.kaeliscore.commands.CommandManager;
import fr.kaelis.kaeliscore.config.ConfigManager;
import fr.kaelis.kaeliscore.config.MessageManager;
import fr.kaelis.kaeliscore.database.DatabaseManager;
import fr.kaelis.kaeliscore.gui.GuiManager;
import fr.kaelis.kaeliscore.integrations.discord.DiscordBot;
import fr.kaelis.kaeliscore.integrations.placeholderapi.KaelisPlaceholders;
import fr.kaelis.kaeliscore.integrations.vault.VaultHook;
import fr.kaelis.kaeliscore.listeners.ListenerManager;
import fr.kaelis.kaeliscore.modules.ModuleManager;
import fr.kaelis.kaeliscore.player.PlayerDataManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * KaelisCore - Plugin survie complet pour serveurs Minecraft
 * Compatible avec Paper, Purpur, Leaf et autres forks depuis 1.21.1
 *
 * @author Kaelis
 */
public final class KaelisCore extends JavaPlugin {

    private static KaelisCore instance;
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    // Managers
    private ConfigManager configManager;
    private MessageManager messageManager;
    private DatabaseManager databaseManager;
    private PlayerDataManager playerDataManager;
    private ModuleManager moduleManager;
    private CommandManager commandManager;
    private ListenerManager listenerManager;
    private GuiManager guiManager;

    // Integrations
    private VaultHook vaultHook;
    private DiscordBot discordBot;

    @Override
    public void onEnable() {
        instance = this;
        
        // ASCII Art Banner
        printBanner();
        
        // Initialize configuration
        getLogger().info("Loading configuration...");
        this.configManager = new ConfigManager(this);
        this.messageManager = new MessageManager(this);
        
        // Initialize database
        getLogger().info("Connecting to database...");
        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Failed to connect to database! Disabling plugin...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        
        // Initialize player data manager
        getLogger().info("Initializing player data manager...");
        this.playerDataManager = new PlayerDataManager(this);
        
        // Initialize GUI manager
        getLogger().info("Initializing GUI system...");
        this.guiManager = new GuiManager(this);
        
        // Initialize modules
        getLogger().info("Loading modules...");
        this.moduleManager = new ModuleManager(this);
        this.moduleManager.loadModules();
        
        // Initialize commands
        getLogger().info("Registering commands...");
        this.commandManager = new CommandManager(this);
        this.commandManager.registerCommands();
        
        // Initialize listeners
        getLogger().info("Registering listeners...");
        this.listenerManager = new ListenerManager(this);
        this.listenerManager.registerListeners();
        
        // Setup integrations
        setupIntegrations();
        
        getLogger().info("KaelisCore has been enabled successfully!");
        getLogger().info("Server: " + Bukkit.getName() + " " + Bukkit.getVersion());
    }

    @Override
    public void onDisable() {
        getLogger().info("Disabling KaelisCore...");
        
        // Save all player data
        if (playerDataManager != null) {
            playerDataManager.saveAll();
        }
        
        // Disable modules
        if (moduleManager != null) {
            moduleManager.disableModules();
        }
        
        // Disconnect Discord bot
        if (discordBot != null) {
            discordBot.shutdown();
        }
        
        // Close database connection
        if (databaseManager != null) {
            databaseManager.disconnect();
        }
        
        getLogger().info("KaelisCore has been disabled.");
    }

    private void printBanner() {
        getLogger().info("");
        getLogger().info("  _  __          _ _       ____               ");
        getLogger().info(" | |/ /__ _  ___| (_)___  / ___|___  _ __ ___ ");
        getLogger().info(" | ' // _` |/ _ \\ | / __|| |   / _ \\| '__/ _ \\");
        getLogger().info(" | . \\ (_| |  __/ | \\__ \\| |__| (_) | | |  __/");
        getLogger().info(" |_|\\_\\__,_|\\___|_|_|___/ \\____\\___/|_|  \\___|");
        getLogger().info("");
        getLogger().info(" Version: " + getDescription().getVersion());
        getLogger().info(" Author: Kaelis");
        getLogger().info("");
    }

    private void setupIntegrations() {
        // Vault
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            getLogger().info("Hooking into Vault...");
            this.vaultHook = new VaultHook(this);
            vaultHook.setup();
        } else {
            getLogger().warning("Vault not found! Some features may not work properly.");
        }
        
        // PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            getLogger().info("Registering PlaceholderAPI placeholders...");
            new KaelisPlaceholders(this).register();
        }
        
        // Discord
        if (configManager.getConfig().getBoolean("discord.enabled", false)) {
            getLogger().info("Starting Discord bot...");
            try {
                this.discordBot = new DiscordBot(this);
                discordBot.start();
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to start Discord bot: " + e.getMessage(), e);
            }
        }
    }

    // Static getters
    public static KaelisCore getInstance() {
        return instance;
    }

    public static MiniMessage getMiniMessage() {
        return MINI_MESSAGE;
    }

    public static Component colorize(String message) {
        return MINI_MESSAGE.deserialize(message);
    }

    // Manager getters
    public ConfigManager getConfigManager() {
        return configManager;
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public ListenerManager getListenerManager() {
        return listenerManager;
    }

    public GuiManager getGuiManager() {
        return guiManager;
    }

    // Integration getters
    public VaultHook getVaultHook() {
        return vaultHook;
    }

    public DiscordBot getDiscordBot() {
        return discordBot;
    }
}
