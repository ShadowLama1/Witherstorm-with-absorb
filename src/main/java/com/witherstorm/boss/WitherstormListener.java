package com.witherstorm.boss;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Wither;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.EntityTeleportEvent;

public final class WitherstormListener implements Listener {

    private final WitherstormPlugin plugin;

    public WitherstormListener(WitherstormPlugin plugin) {
        this.plugin = plugin;
    }

    private ActiveStorm stormFor(Entity entity) {
        if (!(entity instanceof Wither)) return null;
        if (entity.getWorld() == null) return null;
        ActiveStorm storm = plugin.bossManager().getStorm(entity.getWorld());
        if (storm == null) return null;
        return storm.matches(entity) ? storm : null;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        ActiveStorm storm = stormFor(event.getEntity());
        if (storm != null) storm.onDamage(event);
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        ActiveStorm storm = stormFor(event.getEntity());
        if (storm != null) storm.onDamage(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTarget(EntityTargetEvent event) {
        ActiveStorm storm = stormFor(event.getEntity());
        if (storm != null) storm.onTarget(event);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onTargetLiving(EntityTargetLivingEntityEvent event) {
        ActiveStorm storm = stormFor(event.getEntity());
        if (storm != null) storm.onTarget(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onDeath(EntityDeathEvent event) {
        ActiveStorm storm = stormFor(event.getEntity());
        if (storm != null) storm.onDeath(event);
    }

    // Keep the storm from teleporting away (ender pearls / chorus fruit on it).
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onTeleport(EntityTeleportEvent event) {
        if (stormFor(event.getEntity()) != null) {
            event.setCancelled(true);
        }
    }
}
