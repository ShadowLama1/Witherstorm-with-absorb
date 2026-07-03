package com.witherstorm.boss;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the lifecycle of active Witherstorm fights. At most one storm per world
 * is permitted to keep the fight legible and avoid runaway block consumption.
 */
public final class BossManager {

    private final WitherstormPlugin plugin;
    private final ConcurrentHashMap<UUID, ActiveStorm> activeStorms = new ConcurrentHashMap<>();
    private BukkitTask tickTask;

    public BossManager(WitherstormPlugin plugin) {
        this.plugin = plugin;
        startTickLoop();
    }

    public void handleSummonCommand(CommandSender sender) {
        if (!sender.hasPermission("witherstorm.admin")) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>You lack permission."));
            return;
        }
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>Only players can summon the Witherstorm."));
            return;
        }
        World world = player.getWorld();
        if (!plugin.config().worldAllowed(world.getName())) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>Witherstorm cannot be summoned in this world."));
            return;
        }
        if (activeStorms.containsKey(world.getUID())) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>A Witherstorm already rages in this world."));
            return;
        }
        Location loc = player.getLocation();
        summon(loc, player);
    }

    public void summon(Location location, Player summoner) {
        World world = location.getWorld();
        if (world == null) return;
        if (activeStorms.containsKey(world.getUID())) return;

        ActiveStorm storm = new ActiveStorm(plugin, location, summoner.getUniqueId());
        activeStorms.put(world.getUID(), storm);
        storm.begin();

        var mm = plugin.miniMessage();
        String broadcast = plugin.config().summonBroadcast;
        world.getPlayers().forEach(p -> p.sendMessage(mm.deserialize(broadcast)));
    }

    public void handleDespawnCommand(CommandSender sender) {
        if (!sender.hasPermission("witherstorm.admin")) {
            sender.sendMessage(plugin.miniMessage().deserialize("<red>You lack permission."));
            return;
        }
        if (activeStorms.isEmpty()) {
            sender.sendMessage(plugin.miniMessage().deserialize("<gray>No active Witherstorm to despawn."));
            return;
        }
        despawnAll();
        sender.sendMessage(plugin.miniMessage().deserialize("<green>All Witherstorms despawned."));
    }

    public void despawnAll() {
        for (ActiveStorm storm : activeStorms.values()) {
            storm.cleanup();
        }
        activeStorms.clear();
    }

    public boolean hasActiveStorm(World world) {
        return activeStorms.containsKey(world.getUID());
    }

    public ActiveStorm getStorm(World world) {
        return activeStorms.get(world.getUID());
    }

    public void removeStorm(World world) {
        ActiveStorm storm = activeStorms.remove(world.getUID());
        if (storm != null) storm.cleanup();
    }

    private void startTickLoop() {
        tickTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (ActiveStorm storm : activeStorms.values()) {
                storm.tick();
            }
        }, 20L, 20L);
    }

    public void shutdown() {
        if (tickTask != null) tickTask.cancel();
        despawnAll();
    }
}
