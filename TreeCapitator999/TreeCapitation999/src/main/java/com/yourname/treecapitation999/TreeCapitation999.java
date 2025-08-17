package com.yourname.treecapitation999;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.ChatColor;

import java.util.HashSet;
import java.util.Set;

public class TreeCapitation999 extends JavaPlugin implements Listener {

    private boolean breakLeaves;
    private boolean requireAxe;
    private boolean dropAsOne;
    private boolean autoReplant;
    private int maxTreeBlocks;
    private Set<Material> woodMaterials;
    private Set<Material> leafMaterials;

    @Override
    public void onEnable() {
        // Initialize all wood types
        woodMaterials = new HashSet<>();
        woodMaterials.add(Material.OAK_LOG);
        woodMaterials.add(Material.SPRUCE_LOG);
        woodMaterials.add(Material.BIRCH_LOG);
        woodMaterials.add(Material.JUNGLE_LOG);
        woodMaterials.add(Material.ACACIA_LOG);
        woodMaterials.add(Material.DARK_OAK_LOG);
        woodMaterials.add(Material.MANGROVE_LOG);
        woodMaterials.add(Material.CHERRY_LOG);
        woodMaterials.add(Material.CRIMSON_STEM);
        woodMaterials.add(Material.WARPED_STEM);
        
        // Initialize all leaf types
        leafMaterials = new HashSet<>();
        leafMaterials.add(Material.OAK_LEAVES);
        leafMaterials.add(Material.SPRUCE_LEAVES);
        leafMaterials.add(Material.BIRCH_LEAVES);
        leafMaterials.add(Material.JUNGLE_LEAVES);
        leafMaterials.add(Material.ACACIA_LEAVES);
        leafMaterials.add(Material.DARK_OAK_LEAVES);
        leafMaterials.add(Material.MANGROVE_LEAVES);
        leafMaterials.add(Material.CHERRY_LEAVES);
        leafMaterials.add(Material.AZALEA_LEAVES);
        leafMaterials.add(Material.FLOWERING_AZALEA_LEAVES);

        // Load configuration
        saveDefaultConfig();
        breakLeaves = getConfig().getBoolean("break-leaves", true);
        requireAxe = getConfig().getBoolean("require-axe", true);
        dropAsOne = getConfig().getBoolean("drop-as-one", true);
        autoReplant = getConfig().getBoolean("auto-replant", true);
        maxTreeBlocks = getConfig().getInt("max-tree-blocks", 120);
        
        int maxBreakDistance = getConfig().getInt("max-break-distance", 50);
        
        if (maxBreakDistance > 100) {
            getLogger().warning("Max break distance is set very high (" + maxBreakDistance + "). This may impact performance.");
        }

        Bukkit.getPluginManager().registerEvents(this, this);
        getLogger().info("TreeCapitation999 has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("TreeCapitation999 has been disabled!");
    }

    @EventHandler
    public void onTreeBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        
        // Check if the broken block is a log
        if (woodMaterials.contains(block.getType())) {
            // Check if the player is using an axe if required
            if (requireAxe && !event.getPlayer().getInventory().getItemInMainHand().getType().name().endsWith("_AXE")) {
                return;
            }

            // Find all connected logs
            Set<Block> treeBlocks = new HashSet<>();
            findConnectedLogs(block, treeBlocks, getConfig().getInt("max-break-distance", 50));
            
            // Check tree size limit
            if (treeBlocks.size() > maxTreeBlocks) {
                event.getPlayer().sendMessage(ChatColor.RED + "This tree is too large to break!");
                return;
            }

            // Drop items as one if enabled
            if (dropAsOne) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType(), treeBlocks.size()));
                for (Block log : treeBlocks) {
                    log.setType(Material.AIR);
                }
            } else {
                for (Block log : treeBlocks) {
                    log.setType(Material.AIR);
                    log.getWorld().dropItemNaturally(log.getLocation(), new ItemStack(log.getType()));
                }
            }

            // Break leaves if enabled
            if (breakLeaves) {
                Set<Block> leaves = findNearbyLeaves(treeBlocks, getConfig().getInt("leaf-search-radius", 5));
                for (Block leaf : leaves) {
                    leaf.setType(Material.AIR);
                }
            }

            // Handle tool durability
            ItemStack tool = event.getPlayer().getInventory().getItemInMainHand();
            if (tool.getItemMeta() instanceof Damageable) {
                Damageable dmg = (Damageable) tool.getItemMeta();
                dmg.setDamage(dmg.getDamage() + treeBlocks.size());
                tool.setItemMeta((ItemMeta) dmg);
            }

            // Auto-replant if enabled
            if (autoReplant) {
                block.getWorld().getBlockAt(block.getLocation()).setType(saplingFor(block.getType()));
            }

            // Cancel the original event to prevent double drops
            event.setCancelled(true);
        }
    }

    private void findConnectedLogs(Block block, Set<Block> foundBlocks, int remainingDepth) {
        if (remainingDepth <= 0 || !woodMaterials.contains(block.getType()) || foundBlocks.contains(block)) {
            return;
        }
        
        foundBlocks.add(block);
        
        // Check all adjacent blocks
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
                    if (x != 0 || y != 0 || z != 0) {
                        Block relative = block.getRelative(x, y, z);
                        findConnectedLogs(relative, foundBlocks, remainingDepth - 1);
                    }
                }
            }
        }
    }

    private Set<Block> findNearbyLeaves(Set<Block> logs, int radius) {
        Set<Block> leaves = new HashSet<>();
        for (Block log : logs) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block block = log.getRelative(x, y, z);
                        if (leafMaterials.contains(block.getType())) {
                            leaves.add(block);
                        }
                    }
                }
            }
        }
        return leaves;
    }

    private Material saplingFor(Material logType) {
        switch (logType) {
            case OAK_LOG: return Material.OAK_SAPLING;
            case SPRUCE_LOG: return Material.SPRUCE_SAPLING;
            case BIRCH_LOG: return Material.BIRCH_SAPLING;
            case JUNGLE_LOG: return Material.JUNGLE_SAPLING;
            case ACACIA_LOG: return Material.ACACIA_SAPLING;
            case DARK_OAK_LOG: return Material.DARK_OAK_SAPLING;
            case MANGROVE_LOG: return Material.MANGROVE_PROPAGULE;
            case CHERRY_LOG: return Material.CHERRY_SAPLING;
            case CRIMSON_STEM: return Material.CRIMSON_FUNGUS;
            case WARPED_STEM: return Material.WARPED_FUNGUS;
            default: return Material.OAK_SAPLING; // Default to oak if unknown
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("treecapitation")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                reloadConfig();
                breakLeaves = getConfig().getBoolean("break-leaves", true);
                requireAxe = getConfig().getBoolean("require-axe", true);
                dropAsOne = getConfig().getBoolean("drop-as-one", true);
                autoReplant = getConfig().getBoolean("auto-replant", true);
                maxTreeBlocks = getConfig().getInt("max-tree-blocks", 120);
                sender.sendMessage(ChatColor.GREEN + "TreeCapitation config reloaded.");
                return true;
            }
        }
        return false;
    }
}
