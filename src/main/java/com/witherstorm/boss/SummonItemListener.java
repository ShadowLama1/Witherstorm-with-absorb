package com.witherstorm.boss;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

/**
 * Handles the Witherstorm Heart summon catalyst. Right-clicking soul sand
 * (or a similar configured block) with the heart consumes the heart and
 * summons the storm. Also gives ops a heart on first join for convenience.
 */
public final class SummonItemListener implements Listener {

    private final WitherstormPlugin plugin;

    public SummonItemListener(WitherstormPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (!event.getAction().name().equals("RIGHT_CLICK_BLOCK")) return;

        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!DropFactory.isWitherstormHeart(plugin, hand)) return;

        Block clicked = event.getClickedBlock();
        if (clicked == null) return;

        Material required = Material.matchMaterial(plugin.config().summonBlock);
        if (required == null) required = Material.SOUL_SAND;
        if (clicked.getType() != required) return;

        World world = clicked.getWorld();
        if (!plugin.config().worldAllowed(world.getName())) {
            player.sendMessage(plugin.miniMessage().deserialize("<red>The Witherstorm cannot be summoned here."));
            return;
        }
        if (plugin.bossManager().hasActiveStorm(world)) {
            player.sendMessage(plugin.miniMessage().deserialize("<red>A Witherstorm already rages in this world."));
            return;
        }

        event.setCancelled(true);
        if (!player.getGameMode().name().equals("CREATIVE")) {
            hand.setAmount(hand.getAmount() - 1);
        }

        Location spawn = clicked.getRelative(BlockFace.UP).getLocation();
        plugin.bossManager().summon(spawn, player);
    }

    // Prevent placing the heart as a normal block.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (DropFactory.isWitherstormHeart(plugin, event.getItemInHand())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (!player.hasPermission("witherstorm.admin")) return;
        // Only give a heart if the player has none already.
        for (ItemStack item : player.getInventory().getContents()) {
            if (DropFactory.isWitherstormHeart(plugin, item)) return;
        }
        player.getInventory().addItem(DropFactory.witherstormHeart(plugin));
        player.sendMessage(plugin.miniMessage().deserialize(
                "<dark_purple>You have been gifted a Witherstorm Heart. Place it on "
                        + plugin.config().summonBlock.toLowerCase().replace('_', ' ')
                        + " to summon the storm."));
    }
}
