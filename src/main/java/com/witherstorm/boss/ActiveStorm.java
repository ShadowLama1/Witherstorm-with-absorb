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

/**
 * A single active Witherstorm fight. Encapsulates the wither entity, its phase
 * state, attack timers, block consumption, and the boss bar. One instance per
 * world; owned by {@link BossManager}.
 */
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

        AttributeInstance maxHealth = wither.getAttribute(Attribute.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(cfg.maxHealth);
        }
        wither.setHealth(cfg.maxHealth);

        AttributeInstance speed = wither.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speed != null) {
            speed.setBaseValue(0.0);
        }

        // Phase 1 spawn effects — dark storm clouds and command energy.
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

        // Ambient phase 1 particles — dark storm clouds swirling around the Wither.
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
        // Passive block absorption in all phases
        if (cfg.phase1AbsorbEnabled) {
            if (--absorbTimer <= 0) {
                absorbTimer = cfg.phase1AbsorbInterval;
                absorbBlocks();
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
        } else if (phase == 2 && ratio <= cfg.phase3Threshold) {
            phase = 3;
            broadcast(cfg.phase3Message);
            world.playSound(wither.getLocation(), Sound.ENTITY_WITHER_SPAWN, 2.0f, 0.3f);
        }
    }

    private void updateBossBar() {
        double max = wither.getAttribute(Attribute.MAX_HEALTH) != null
                ? wither.getAttribute(Attribute.MAX_HEALTH).getValue()
                : cfg.maxHealth;
        float progress = (float) Math.max(0.0, Math.min(1.0, wither.getHealth() / max));
        bossBar.progress(progress);
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
        // The storm hovers; keep it from descending by counteracting gravity.
        wither.setVelocity(new Vector(0, 0.05, 0));
    }

    private void tractorBeam() {
        Location eye = wither.getEyeLocation();
        for (Player p : world.getPlayers()) {
            if (p.getGameMode().name().equals("SPECTATOR")) continue;
            if (p.getLocation().distanceSquared(eye) > cfg.tractorBeamRange * cfg.tractorBeamRange) continue;

            Vector pull = eye.toVector().subtract(p.getLocation().toVector()).normalize()
                    .multiply(cfg.tractorBeamForce);
            p.setVelocity(p.getVelocity().add(pull));
            p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION,
                    cfg.tractorBeamLevitationTicks, 0, false, true, true));

            // Visual: particles along the beam.
            drawBeam(p.getLocation(), eye);
        }
    }

    private void drawBeam(Location from, Location to) {
        Vector dir = to.toVector().subtract(from.toVector());
        double dist = dir.length();
        if (dist < 0.1) return;
        dir.normalize();
        for (double d = 0; d < dist; d += 1.0) {
            Location point = from.clone().add(dir.clone().multiply(d));
            world.spawnParticle(Particle.PORTAL, point, 4, 0.2, 0.2, 0.2, 0.01);
        }
    }

    private void tentacleSlam() {
        Location center = wither.getLocation();
        world.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
        world.playSound(center, Sound.ENTITY_WITHER_SHOOT, 1.2f, 0.6f);

        for (Player p : world.getPlayers()) {
            if (p.getGameMode().name().equals("SPECTATOR")) continue;
            if (p.getLocation().distanceSquared(center) > cfg.tentacleSlamRadius * cfg.tentacleSlamRadius) continue;
            p.damage(cfg.tentacleSlamDamage, wither);
            Vector knock = p.getLocation().toVector().subtract(center.toVector()).normalize()
                    .multiply(cfg.tentacleSlamKnockback).setY(0.6);
            p.setVelocity(knock);
        }
    }

    private void skullBarrage() {
        Location eye = wither.getEyeLocation();
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (int i = 0; i < cfg.skullBarrageCount; i++) {
            Player target = nearestPlayer(60);
            if (target == null) break;
            Vector dir = target.getEyeLocation().toVector().subtract(eye.toVector()).normalize();
            // Spread for a barrage feel.
            dir.add(new Vector(rng.nextDouble(-0.15, 0.15), rng.nextDouble(-0.1, 0.1), rng.nextDouble(-0.15, 0.15)));
            WitherSkull skull = world.spawn(eye, WitherSkull.class);
            skull.setShooter(wither);
            skull.setDirection(dir);
            skull.setVelocity(dir.multiply(1.2));
        }
    }

    private void summonMinions() {
        // Cull dead minions from the tracked list.
        minions.removeIf(id -> Bukkit.getEntity(id) == null || ((org.bukkit.entity.LivingEntity) Bukkit.getEntity(id)).isDead());
        if (minions.size() >= cfg.summonMinionsMax) return;

        for (int i = 0; i < cfg.summonMinionsCount; i++) {
            Location spawn = wither.getLocation().clone().add(
                    ThreadLocalRandom.current().nextDouble(-4, 4),
                    0,
                    ThreadLocalRandom.current().nextDouble(-4, 4));
            var skeleton = world.spawn(spawn, org.bukkit.entity.WitherSkeleton.class);
            skeleton.setTarget(nearestPlayer(40));
            minions.add(skeleton.getUniqueId());
        }
        world.playSound(wither.getLocation(), Sound.ENTITY_WITHER_SKELETON_AMBIENT, 1.0f, 0.7f);
    }

    private void absorbBlocksPhase1() {
        if (wither == null || dead) return;
        Location center = wither.getLocation();
        int radius = cfg.phase1AbsorbRadius;
        int rate = cfg.phase1AbsorbRate;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        int absorbed = 0;
        outer:
        for (int r = 1; r <= radius && absorbed < rate; r++) {
            for (int i = 0; i < rate * 2 && absorbed < rate; i++) {
                int dx = rng.nextInt(-r, r + 1);
                int dz = rng.nextInt(-r, r + 1);
                int dy = rng.nextInt(-3, 4);
                Block b = world.getBlockAt(center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                if (b.getType().isAir() || b.isLiquid()) continue;
                if (b.getType() == Material.BEDROCK) continue;
                if (b.getType().getHardness() > 50.0) continue;
                Material type = b.getType();
                b.setType(Material.AIR);

                Location blockLoc = b.getLocation().add(0.5, 0.5, 0.5);
                FallingBlock fb = world.spawn(blockLoc, FallingBlock.class);
                fb.setBlockData(type.createBlockData());
                fb.setGravity(false);
                fb.setDropItem(false);
                fb.setHurtEntities(false);
                floatingBlocks.add(fb.getUniqueId());

                world.spawnParticle(Particle.BLOCK, blockLoc, 6, 0.2, 0.2, 0.2, type.createBlockData());
                absorbed++;
                if (absorbed >= rate) break outer;
            }
        }

        if (absorbed > 0) {
            blocksAbsorbed += absorbed;
            world.playSound(center, Sound.ENTITY_ENDER_EYE_DEATH, 0.4f, 0.7f);
        }
    }

    private void updateFloatingBlocks() {
        if (wither == null || floatingBlocks.isEmpty()) return;
        Location target = wither.getLocation().add(0, 1, 0);

        floatingBlocks.removeIf(uuid -> {
            if (wither == null || dead) return true;
            if (uuid == null) return true;
            org.bukkit.entity.Entity ent = Bukkit.getEntity(uuid);
            if (ent == null || ent.isDead() || !ent.isValid()) return true;

            Location loc = ent.getLocation();
            Vector dir = target.toVector().subtract(loc.toVector());
            double dist = dir.length();
            if (dist < 1.5) {
                // Block reached the storm — absorb it.
                ent.remove();
                world.spawnParticle(Particle.SQUID_INK, ent.getLocation(), 4, 0.3, 0.3, 0.3, 0.01);
                if (cfg.growOnConsume) {
                    AttributeInstance maxHealth = wither.getAttribute(Attribute.MAX_HEALTH);
                    if (maxHealth != null) {
                        double newMax = Math.min(cfg.growthCap, maxHealth.getValue() + cfg.growthPerBlock);
                        maxHealth.setBaseValue(newMax);
                        wither.setHealth(Math.min(newMax, wither.getHealth() + cfg.growthPerBlock));
                    }
                }
                return true;
            }

            dir.normalize().multiply(0.35 + (dist * 0.02));
            ent.setVelocity(dir);
            return false;
        });
    }

    private void consumeBlocks() {
        if (world == null) return;
        Location center = wither.getLocation();
        int radius = cfg.consumptionRadius;
        int rate = cfg.consumptionRate;
        ThreadLocalRandom rng = ThreadLocalRandom.current();

        int consumed = 0;
        // Spiral outward from the storm, consuming a limited number per tick.
        outer:
        for (int r = 1; r <= radius && consumed < rate; r++) {
            for (int i = 0; i < rate && consumed < rate; i++) {
                int dx = rng.nextInt(-r, r + 1);
                int dz = rng.nextInt(-r, r + 1);
                int dy = rng.nextInt(-2, 3);
                Block b = world.getBlockAt(center.getBlockX() + dx, center.getBlockY() + dy, center.getBlockZ() + dz);
                if (b.getType().isAir() || b.isLiquid()) continue;
                if (b.getType() == Material.BEDROCK) continue;
                Material type = b.getType();
                b.setType(Material.AIR);
                world.spawnParticle(Particle.BLOCK, b.getLocation().add(0.5, 0.5, 0.5),
                        10, 0.3, 0.3, 0.3, type.createBlockData());
                consumed++;
                if (consumed >= rate) break outer;
            }
        }

        if (consumed > 0 && cfg.growOnConsume) {
            AttributeInstance maxHealth = wither.getAttribute(Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                double newMax = Math.min(cfg.growthCap, maxHealth.getValue() + consumed * cfg.growthPerBlock);
                maxHealth.setBaseValue(newMax);
                // Heal by the same amount so growth is felt immediately.
                wither.setHealth(Math.min(newMax, wither.getHealth() + consumed * cfg.growthPerBlock));
            }
        }
    }

    private Player nearestPlayer(double range) {
        Player nearest = null;
        double best = range * range;
        for (Player p : world.getPlayers()) {
            if (p.getGameMode().name().equals("SPECTATOR")) continue;
            double d = p.getLocation().distanceSquared(wither.getLocation());
            if (d < best) {
                best = d;
                nearest = p;
            }
        }
        return nearest;
    }

    private void showBossBarToNearby() {
        for (Player p : world.getPlayers()) {
            p.showBossBar(bossBar);
        }
    }

    private void hideBossBar() {
        for (Player p : world.getPlayers()) {
            p.hideBossBar(bossBar);
        }
    }

    private void broadcast(String message) {
        var mm = plugin.miniMessage();
        for (Player p : world.getPlayers()) {
            p.sendMessage(mm.deserialize(message));
        }
    }

    public void onDamage(EntityDamageEvent event) {
        if (dead || wither == null || !event.getEntity().equals(wither)) return;
        // Phase 1: armored — reduce incoming damage to force the phase transition.
        if (phase == 1) {
            event.setDamage(event.getDamage() * 0.5);
        }
        // Visual feedback.
        world.spawnParticle(Particle.DRAGON_BREATH, wither.getEyeLocation(), 5, 0.5, 0.5, 0.5, 0.02);
    }

    public void onTarget(EntityTargetEvent event) {
        // The storm picks its own targets; don't let vanilla override.
        if (event.getTarget() == null) {
            Player target = nearestPlayer(60);
            if (target != null) event.setTarget(target);
        }
    }

    public void onDeath(EntityDeathEvent event) {
        if (dead || wither == null || !event.getEntity().equals(wither)) return;
        dead = true;
        event.setDroppedExp(cfg.dropExperience);
        event.getDrops().clear();

        // Standard configured drops.
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        for (WitherstormConfig.DropEntry drop : cfg.drops) {
            if (rng.nextDouble() < drop.chance()) {
                Material mat = Material.matchMaterial(drop.material());
                if (mat != null) {
                    event.getDrops().add(new ItemStack(mat, drop.amount()));
                }
            }
        }

        // Custom lore drop: Command Block Fragment.
        if (cfg.commandBlockFragmentEnabled) {
            event.getDrops().add(DropFactory.commandBlockFragment(plugin));
        }

        broadcast(cfg.deathBroadcast);
        world.playSound(wither.getLocation(), Sound.ENTITY_WITHER_DEATH, 2.0f, 0.5f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, wither.getLocation(), 3, 1, 1, 1, 0);

        hideBossBar();
        plugin.bossManager().removeStorm(world);
    }

    public Wither wither() {
        return wither;
    }

    public boolean matches(org.bukkit.entity.Entity entity) {
        return wither != null && wither.equals(entity);
    }

    public void cleanup() {
        dead = true;
        hideBossBar();
        if (wither != null && wither.isValid()) {
            wither.remove();
        }
        for (UUID id : minions) {
            org.bukkit.entity.Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        minions.clear();
        for (UUID id : floatingBlocks) {
            org.bukkit.entity.Entity e = Bukkit.getEntity(id);
            if (e != null) e.remove();
        }
        floatingBlocks.clear();
    }
}
