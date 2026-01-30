package fr.kaelis.kaeliscore.modules.antigrief;

import fr.kaelis.kaeliscore.KaelisCore;
import fr.kaelis.kaeliscore.modules.AbstractModule;
import fr.kaelis.kaeliscore.modules.claims.Claim;
import fr.kaelis.kaeliscore.modules.claims.ClaimsModule;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Anti-grief module for protection
 */
public class AntiGriefModule extends AbstractModule implements Listener {

    private boolean protectClaims;
    private boolean preventExplosions;
    private boolean preventFireSpread;
    private boolean preventEndermanGrief;
    private boolean preventCreeperDamage;
    private boolean preventWitherDamage;
    private Set<Material> protectedBlocks;

    public AntiGriefModule(KaelisCore plugin) {
        super(plugin, "AntiGrief", "antigrief");
    }

    @Override
    public void onEnable() {
        loadConfig();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void onDisable() {
        // Nothing to clean up
    }

    private void loadConfig() {
        protectClaims = getConfig().getBoolean("protect-claims", true);
        preventExplosions = getConfig().getBoolean("prevent-explosions", true);
        preventFireSpread = getConfig().getBoolean("prevent-fire-spread", true);
        preventEndermanGrief = getConfig().getBoolean("prevent-enderman-grief", true);
        preventCreeperDamage = getConfig().getBoolean("prevent-creeper-damage", true);
        preventWitherDamage = getConfig().getBoolean("prevent-wither-damage", true);

        protectedBlocks = new HashSet<>();
        List<String> blockList = getConfig().getStringList("protected-blocks");
        for (String block : blockList) {
            Material material = Material.matchMaterial(block);
            if (material != null) {
                protectedBlocks.add(material);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        if (!protectClaims) return;
        
        Player player = event.getPlayer();
        if (player.hasPermission("kaeliscore.antigrief.bypass")) return;

        ClaimsModule claims = plugin.getModuleManager().getModule(ClaimsModule.class);
        if (claims != null && !claims.canBuild(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            plugin.getMessageManager().send(player, "antigrief.cannot-break");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (!protectClaims) return;
        
        Player player = event.getPlayer();
        if (player.hasPermission("kaeliscore.antigrief.bypass")) return;

        ClaimsModule claims = plugin.getModuleManager().getModule(ClaimsModule.class);
        if (claims != null && !claims.canBuild(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            plugin.getMessageManager().send(player, "antigrief.cannot-place");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (!protectClaims) return;
        if (event.getClickedBlock() == null) return;
        
        Player player = event.getPlayer();
        if (player.hasPermission("kaeliscore.antigrief.bypass")) return;

        // Check for container/interactable blocks
        Material type = event.getClickedBlock().getType();
        if (isInteractable(type)) {
            ClaimsModule claims = plugin.getModuleManager().getModule(ClaimsModule.class);
            if (claims != null && !claims.canInteract(player, event.getClickedBlock().getLocation())) {
                event.setCancelled(true);
                plugin.getMessageManager().send(player, "antigrief.cannot-interact");
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        if (!protectClaims) return;
        
        Player player = event.getPlayer();
        if (player.hasPermission("kaeliscore.antigrief.bypass")) return;

        ClaimsModule claims = plugin.getModuleManager().getModule(ClaimsModule.class);
        if (claims != null && !claims.canBuild(player, event.getBlock().getLocation())) {
            event.setCancelled(true);
            plugin.getMessageManager().send(player, "antigrief.cannot-place");
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        if (!preventExplosions) return;

        // Check entity type
        EntityType type = event.getEntityType();
        boolean shouldCancel = false;

        if (type == EntityType.CREEPER && preventCreeperDamage) {
            shouldCancel = true;
        } else if (type == EntityType.WITHER || type == EntityType.WITHER_SKULL) {
            if (preventWitherDamage) shouldCancel = true;
        } else if (type == EntityType.TNT || type == EntityType.TNT_MINECART) {
            // Check if in claim
            ClaimsModule claims = plugin.getModuleManager().getModule(ClaimsModule.class);
            if (claims != null) {
                Claim claim = claims.getClaimAt(event.getLocation());
                if (claim != null && !claim.getFlag("explosions")) {
                    shouldCancel = true;
                }
            }
        }

        if (shouldCancel) {
            event.setCancelled(true);
        } else {
            // Remove protected blocks from explosion
            event.blockList().removeIf(block -> {
                ClaimsModule claims = plugin.getModuleManager().getModule(ClaimsModule.class);
                if (claims != null) {
                    Claim claim = claims.getClaimAt(block.getLocation());
                    if (claim != null && !claim.getFlag("explosions")) {
                        return true;
                    }
                }
                return protectedBlocks.contains(block.getType());
            });
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!preventExplosions) return;

        event.blockList().removeIf(block -> {
            ClaimsModule claims = plugin.getModuleManager().getModule(ClaimsModule.class);
            if (claims != null) {
                Claim claim = claims.getClaimAt(block.getLocation());
                if (claim != null && !claim.getFlag("explosions")) {
                    return true;
                }
            }
            return protectedBlocks.contains(block.getType());
        });
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        if (!preventFireSpread) return;

        ClaimsModule claims = plugin.getModuleManager().getModule(ClaimsModule.class);
        if (claims != null) {
            Claim claim = claims.getClaimAt(event.getBlock().getLocation());
            if (claim != null && !claim.getFlag("fire-spread")) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        // Check if enderman grief should be prevented
        if (event.getEntityType() == EntityType.ENDERMAN && preventEndermanGrief) {
            // Will be handled in EntityChangeBlockEvent instead
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPvP(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        ClaimsModule claims = plugin.getModuleManager().getModule(ClaimsModule.class);
        if (claims != null) {
            Claim claim = claims.getClaimAt(victim.getLocation());
            if (claim != null && !claim.getFlag("pvp")) {
                event.setCancelled(true);
                plugin.getMessageManager().send(attacker, "antigrief.pvp-disabled");
            }
        }
    }

    private boolean isInteractable(Material material) {
        return material.name().contains("CHEST") ||
               material.name().contains("DOOR") ||
               material.name().contains("GATE") ||
               material.name().contains("BUTTON") ||
               material.name().contains("LEVER") ||
               material.name().contains("TRAPDOOR") ||
               material == Material.FURNACE ||
               material == Material.BLAST_FURNACE ||
               material == Material.SMOKER ||
               material == Material.BREWING_STAND ||
               material == Material.ANVIL ||
               material == Material.ENCHANTING_TABLE ||
               material == Material.BEACON ||
               material == Material.HOPPER ||
               material == Material.DISPENSER ||
               material == Material.DROPPER;
    }
}
