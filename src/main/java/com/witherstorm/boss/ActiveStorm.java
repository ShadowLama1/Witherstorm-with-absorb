package com.witherstorm.boss;

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wither;
import org.bukkit.entity.WitherSkull;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class ActiveStorm {

    private final WitherstormPlugin plugin;
    private final WitherstormConfig cfg;
    private final World world;
    private final Location origin;
    private final UUID summonerId;
    private final BossBar bossBar;

    private Wither wither;
    private int phase = 1;
    private int tractorTimer;
    private int slamTimer;
    private int barrageTimer;
    private int minionTimer;
    private int absorbTimer;
    private final List<UUID> minions = new ArrayList<>();
    private final List<UUID> floatingBlocks = new ArrayList<>();
    private int blocksAbsorbed = 0;
    private boolean dead = false;

    public ActiveStorm(WitherstormPlugin plugin, Location origin, UUID summonerId) {
        this.plugin = plugin;
        this.cfg = plugin.config();
        this.world = origin.getWorld();
        this.origin = origin;
        this.summonerId = summonerId;
        this.bossBar = BossBar.bossBar(
                Component.text("Witherstorm"),
                1.0f,
                BossBar.Color.valueOf(readBarColor()),
                BossBar.Overlay.valueOf(readBarStyle())
        );
        this.tractorTimer = cfg.tractorBeamInterval;
        this.slamTimer = cfg.tentacleSlamInterval;
        this.barrageTimer = cfg.skullBarrageInterval;
        this.minionTimer = cfg.summonMinionsInterval;
    }

    private String readBarColor() {
        try {
            return plugin.getConfig().getString("boss.boss-bar-color", "PURPLE");
        } catch (Exception e) {
            return "PURPLE";
        }
    }

    private String readBarStyle() {
        try {
            return plugin.getConfig().getString("boss.boss-bar-style", "NOTCHED_20");
        } catch (Exception e) {
            return "NOTCHED_20";
        }
    }

    public void begin() {
        if (world == null) return;
        this.wither = world.spawn(origin.clone().add(0, 3, 0), Wither.class);
        wither.customName(plugin.miniMessage().deserialize("<dark_purple><bold>Witherstorm"));
        wither.setCustomNameVisible(true);
        wither.setGlowing(true);
        wither.setInvulnerable(false);
        wither.setInvulnerabilityTicks(0);
        wither.setAI(true);
        wither.setPersistent(true);
        wither.setPortalCooldown(0);
        wither.setSilent(false);

        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "meg model " + wither.getUniqueId() + " summon witherstorm_phase1");

        AttributeInstance maxHealth = wither.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(cfg.maxHealth);
        }
        wither.setHealth(cfg.maxHealth);

        AttributeInstance speed = wither.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.0);
        }

        world.playSound(wither.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION, wither.getLocation(), 4, 2, 2, 2, 0);
        world.spawnParticle(Particle.LARGE_SMOKE, wither.getLocation(), 30, 3, 3, 3, 0.1);

        showBossBarToNearby();
    }

    public void tick() {
        if (dead || wither == null || !wither.isValid()) {
            return;
        }
        updatePhase();
        updateBossBar();
        keepLevitated();

        if (phase == 1) {
            Location loc = wither.getLocation();
            world.spawnParticle(Particle.LARGE_SMOKE, loc.clone().add(0, 1, 0), 3, 1.5, 1.5, 1.5, 0.02);
            world.spawnParticle(Particle.SQUID_INK, loc.clone().add(0, 0.5, 0), 2, 1.0, 1.0, 1.0, 0.01);
            if (ThreadLocalRandom.current().nextInt(3) == 0) {
                world.spawnParticle(Particle.REVERSE_PORTAL, loc.clone().add(0, 1.5, 0), 1, 0.8, 0.8, 0.8, 0.1);
            }
        }

        if (cfg.tractorBeamEnabled && --tractorTimer <= 0) {
            tractorTimer = cfg.tractorBeamInterval;
            tractorBeam();
        }
        if (cfg.tentacleSlamEnabled && --slamTimer <= 0) {
            slamTimer = cfg.tentacleSlamInterval;
            tentacleSlam();
        }
        if (phase >= 3 && cfg.skullBarrageEnabled && --barrageTimer <= 0) {
            barrageTimer = cfg.skullBarrageInterval;
            skullBarrage();
        }
        if (phase >= 2 && cfg.summonMinionsEnabled && --minionTimer <= 0) {
            minionTimer = cfg.summonMinionsInterval;
            summonMinions();
        }
        if (phase >= 2) {
            consumeBlocks();
        }
        if (phase == 1 && cfg.phase1AbsorbEnabled) {
            if (--absorbTimer <= 0) {
                absorbTimer = cfg.phase1AbsorbInterval;
                absorbBlocksPhase1();
            }
        }
        updateFloatingBlocks();
    }
    private void updatePhase() {
        double health = wither.getHealth();
        double max = wither.getAttribute(Attribute.MAX_HEALTH) != null
                ? wither.getAttribute(Attribute.MAX_HEALTH).getValue()
                : cfg.maxHealth;
        double ratio = health / max;

        if (phase == 1 && (ratio <= cfg.phase2Threshold || blocksAbsorbed >= cfg.phase1EvolveBlocks)) {
            phase = 2;
            broadcast(cfg.phase2Message);
            world.playSound(wither.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.5f, 0.5f);
            world.spawnParticle(Particle.EXPLOSION, wither.getLocation(), 8, 3, 3, 3, 0);
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "meg model " + wither.getUniqueId() + " change witherstorm_phase2");
        } else if (phase == 2 && ratio <= cfg.phase3Threshold) {
            phase = 3;
            broadcast(cfg.phase3Message);
            world.playSound(wither.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
            
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "meg model " + wither.getUniqueId() + " change witherstorm_phase3");
            if (wither.getAttribute(Attribute.GENERIC_SCALE) != null) {
                wither.getAttribute(Attribute.GENERIC_SCALE).setBaseValue(5.0);
            }
        }
    }

    private void updateBossBar() {
        double max = wither.getAttribute(Attribute.MAX_HEALTH) != null
                ? wither.getAttribute(Attribute.MAX_HEALTH).getValue()
                : cfg.maxHealth;
        float progress = (float) Math.max(0.0, Math.min(1.0, wither.getHealth() / max));
        bossBar.progress(progress);
        
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distance(wither.getLocation()) <= 60) {
                p.showBossBar(bossBar);
            } else {
                p.hideBossBar(bossBar);
            }
        }

        if (phase == 1 && cfg.phase1AbsorbEnabled) {
            double absorbProgress = Math.min(1.0, (double) blocksAbsorbed / cfg.phase1EvolveBlocks);
            bossBar.name(plugin.miniMessage().deserialize(
                    "<dark_purple><bold>Witherstorm</bold> <gray>Phase " + phase
                            + " <dark_gray>" + (int) wither.getHealth() + " HP"
                            + " <blue>Absorbed: " + blocksAbsorbed + "/" + cfg.phase1EvolveBlocks
                            + " <dark_blue>(" + (int) (absorbProgress * 100) + "%)"));
        } else {
            bossBar.name(plugin.miniMessage().deserialize(
                    "<dark_purple>Witherstorm <gray>Phase " + phase + " <dark_gray>" + (int) wither.getHealth() + " HP"));
        }
    }

    private void keepLevitated() {
        wither.setVelocity(new Vector(0, 0.05, 0));
    }

    private void showBossBarToNearby() {
        for (Player p : world.getPlayers()) {
            if (p.getLocation().distance(wither.getLocation()) <= 60) {
                p.showBossBar(bossBar);
            }
        }
    }

    private void tractorBeam() {
        for (Entity e : wither.getNearbyEntities(25, 25, 25)) {
            if (e instanceof Player p) {
                Vector v = wither.getLocation().toVector().subtract(p.getLocation().toVector()).normalize();
                v.setY(0.4);
                p.setVelocity(v.multiply(0.6));
                p.sendMessage(plugin.miniMessage().deserialize("<red>Du wirst vom Traktorstrahl erfasst!"));
            }
        }
    }

    private void tentacleSlam() {
        Location targetLoc = wither.getLocation().add(ThreadLocalRandom.current().nextInt(-15, 15), -5, ThreadLocalRandom.current().nextInt(-15, 15));
        world.spawnParticle(Particle.EXPLOSION, targetLoc, 2, 1, 1, 1, 0);
        world.playSound(targetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.8f);
        for (Entity e : world.getNearbyEntities(targetLoc, 4, 4, 4)) {
            if (e instanceof Player p) {
                p.damage(6.0, wither);
                p.setVelocity(new Vector(0, 0.5, 0));
            }
        }
    }

    private void skullBarrage() {
        for (int i = 0; i < 3; i++) {
            wither.launchProjectile(WitherSkull.class);
        }
    }

    private void summonMinions() {
        if (minions.size() >= 8) return;
        Location spawnLoc = wither.getLocation().add(ThreadLocalRandom.current().nextInt(-5, 5), -3, ThreadLocalRandom.current().nextInt(-5, 5));
        Entity minion = world.spawnEntity(spawnLoc, EntityType.WITHER_SKELETON);
        minions.add(minion.getUniqueId());
    }

    private void consumeBlocks() {
        Location loc = wither.getLocation().add(ThreadLocalRandom.current().nextInt(-10, 10), ThreadLocalRandom.current().nextInt(-10, -2), ThreadLocalRandom.current().nextInt(-10, 10));
        Block b = loc.getBlock();
        if (b.getType() != Material.AIR && b.getType() != Material.BEDROCK) {
            FallingBlock fb = world.spawnFallingBlock(b.getLocation(), b.getBlockData());
            fb.setDropItem(false);
            fb.setVelocity(wither.getLocation().toVector().subtract(fb.getLocation().toVector()).normalize().multiply(0.4));
            floatingBlocks.add(fb.getUniqueId());
            b.setType(Material.AIR);
            blocksAbsorbed++;
        }
    }

    private void absorbBlocksPhase1() {
        consumeBlocks();
    }

    private void updateFloatingBlocks() {
        floatingBlocks.removeIf(uuid -> {
            Entity e = Bukkit.getEntity(uuid);
            if (e == null || !e.isValid()) return true;
            if (e.getLocation().distance(wither.getLocation()) <= 3) {
                e.remove();
                return true;
            }
            return false;
        });
    }

    private void broadcast(String msg) {
        if (msg == null || msg.isEmpty()) return;
        Bukkit.broadcast(plugin.miniMessage().deserialize(msg));
    }

    public void remove() {
        dead = true;
        if (wither != null && wither.isValid()) {
            wither.remove();
        }
        for (UUID uuid : minions) {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null) e.remove();
        }
        for (UUID uuid : floatingBlocks) {
            Entity e = Bukkit.getEntity(uuid);
            if (e != null) e.remove();
        }
        bossBar.removeAll();
    }

    public void handleDamage(EntityDamageEvent event) {}

    public void handleDamageByEntity(EntityDamageByEntityEvent event) {}

    public void handleTarget(EntityTargetEvent event) {}

    public void handleDeath(EntityDeathEvent event) {
        dead = true;
        bossBar.removeAll();
    }
}
