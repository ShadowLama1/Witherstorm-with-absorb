package com.witherstorm.boss;

import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.List;

/**
 * Strongly-typed view over config.yml. All balance and lore values are read once
 * on load/reload so the runtime never touches raw config keys during a fight.
 */
public final class WitherstormConfig {

    public final double maxHealth;
    public final double phase2Threshold;
    public final double phase3Threshold;
    public final int consumptionRate;
    public final int consumptionRadius;
    public final boolean growOnConsume;
    public final double growthPerBlock;
    public final double growthCap;

    public final boolean phase1AbsorbEnabled;
    public final int phase1AbsorbInterval;
    public final int phase1AbsorbRate;
    public final int phase1AbsorbRadius;
    public final int phase1EvolveBlocks;
    public final double phase1AbsorbDamage;
    public final double phase1SizePerBlock;

    public final boolean tractorBeamEnabled;
    public final int tractorBeamInterval;
    public final double tractorBeamForce;
    public final double tractorBeamRange;
    public final int tractorBeamLevitationTicks;

    public final boolean tentacleSlamEnabled;
    public final int tentacleSlamInterval;
    public final double tentacleSlamDamage;
    public final double tentacleSlamRadius;
    public final double tentacleSlamKnockback;

    public final boolean skullBarrageEnabled;
    public final int skullBarrageInterval;
    public final int skullBarrageCount;
    public final double skullBarrageDamage;

    public final boolean summonMinionsEnabled;
    public final int summonMinionsInterval;
    public final int summonMinionsMax;
    public final int summonMinionsCount;

    public final int dropExperience;
    public final List<DropEntry> drops;
    public final boolean commandBlockFragmentEnabled;
    public final String commandBlockFragmentMaterial;
    public final int commandBlockFragmentModelData;
    public final String commandBlockFragmentName;
    public final List<String> commandBlockFragmentLore;

    public final String summonItemName;
    public final List<String> summonItemLore;
    public final String summonBroadcast;
    public final String deathBroadcast;
    public final String phase2Message;
    public final String phase3Message;

    public final List<String> allowedWorlds;
    public final String summonBlock;
    public final int protectRadius;

    public WitherstormConfig(FileConfiguration cfg) {
        this.maxHealth = cfg.getDouble("boss.max-health", 600.0);
        this.phase2Threshold = cfg.getDouble("boss.phase-2-threshold", 0.66);
        this.phase3Threshold = cfg.getDouble("boss.phase-3-threshold", 0.33);
        this.consumptionRate = cfg.getInt("boss.consumption-rate", 8);
        this.consumptionRadius = cfg.getInt("boss.consumption-radius", 14);
        this.growOnConsume = cfg.getBoolean("boss.grow-on-consume", true);
        this.growthPerBlock = cfg.getDouble("boss.growth-per-block", 1.0);
        this.growthCap = cfg.getDouble("boss.growth-cap", 1200.0);

        this.phase1AbsorbEnabled = cfg.getBoolean("phase1.absorb.enabled", true);
        this.phase1AbsorbInterval = cfg.getInt("phase1.absorb.interval", 5);
        this.phase1AbsorbRate = cfg.getInt("phase1.absorb.rate", 4);
        this.phase1AbsorbRadius = cfg.getInt("phase1.absorb.radius", 12);
        this.phase1EvolveBlocks = cfg.getInt("phase1.evolve-blocks", 200);
        this.phase1AbsorbDamage = cfg.getDouble("phase1.absorb.damage", 3.0);
        this.phase1SizePerBlock = cfg.getDouble("phase1.size-per-block", 0.003);

        this.tractorBeamEnabled = cfg.getBoolean("attacks.tractor-beam.enabled", true);
        this.tractorBeamInterval = cfg.getInt("attacks.tractor-beam.interval", 12);
        this.tractorBeamForce = cfg.getDouble("attacks.tractor-beam.force", 1.6);
        this.tractorBeamRange = cfg.getDouble("attacks.tractor-beam.range", 40);
        this.tractorBeamLevitationTicks = cfg.getInt("attacks.tractor-beam.levitation-ticks", 40);

        this.tentacleSlamEnabled = cfg.getBoolean("attacks.tentacle-slam.enabled", true);
        this.tentacleSlamInterval = cfg.getInt("attacks.tentacle-slam.interval", 18);
        this.tentacleSlamDamage = cfg.getDouble("attacks.tentacle-slam.damage", 8.0);
        this.tentacleSlamRadius = cfg.getDouble("attacks.tentacle-slam.radius", 8.0);
        this.tentacleSlamKnockback = cfg.getDouble("attacks.tentacle-slam.knockback", 1.4);

        this.skullBarrageEnabled = cfg.getBoolean("attacks.skull-barrage.enabled", true);
        this.skullBarrageInterval = cfg.getInt("attacks.skull-barrage.interval", 9);
        this.skullBarrageCount = cfg.getInt("attacks.skull-barrage.count", 3);
        this.skullBarrageDamage = cfg.getDouble("attacks.skull-barrage.damage", 6.0);

        this.summonMinionsEnabled = cfg.getBoolean("attacks.summon-minions.enabled", true);
        this.summonMinionsInterval = cfg.getInt("attacks.summon-minions.interval", 25);
        this.summonMinionsMax = cfg.getInt("attacks.summon-minions.max-minions", 6);
        this.summonMinionsCount = cfg.getInt("attacks.summon-minions.count", 2);

        this.dropExperience = cfg.getInt("drops.experience", 1500);
        this.drops = parseDrops(cfg.getStringList("drops.items"));
        this.commandBlockFragmentEnabled = cfg.getBoolean("drops.command-block-fragment.enabled", true);
        this.commandBlockFragmentMaterial = cfg.getString("drops.command-block-fragment.material", "STRUCTURE_BLOCK");
        this.commandBlockFragmentModelData = cfg.getInt("drops.command-block-fragment.custom-model-data", 26111);
        this.commandBlockFragmentName = cfg.getString("drops.command-block-fragment.name", "<gradient:#a855f7:#ec4899>Command Block Fragment</gradient>");
        this.commandBlockFragmentLore = cfg.getStringList("drops.command-block-fragment.lore");

        this.summonItemName = cfg.getString("lore.summon-item.name", "<gradient:#7c3aed:#db2777>Witherstorm Heart</gradient>");
        this.summonItemLore = cfg.getStringList("lore.summon-item.lore");
        this.summonBroadcast = cfg.getString("lore.summon-broadcast", "<dark_purple>The sky darkens...");
        this.deathBroadcast = cfg.getString("lore.death-broadcast", "<light_purple>The Witherstorm collapses...");
        this.phase2Message = cfg.getString("lore.phase-2-message", "<dark_purple>The Witherstorm shudders...");
        this.phase3Message = cfg.getString("lore.phase-3-message", "<dark_red>The Witherstorm roars!");

        this.allowedWorlds = cfg.getStringList("world.allowed-worlds");
        this.summonBlock = cfg.getString("world.summon-block", "SOUL_SAND");
        this.protectRadius = cfg.getInt("world.protect-radius", 32);
    }

    private static List<DropEntry> parseDrops(List<String> raw) {
        List<DropEntry> out = new ArrayList<>();
        for (String line : raw) {
            String[] parts = line.split(":");
            if (parts.length < 3) continue;
            try {
                out.add(new DropEntry(parts[0], Integer.parseInt(parts[1]), Double.parseDouble(parts[2])));
            } catch (NumberFormatException ignored) {
            }
        }
        return out;
    }

    public boolean worldAllowed(String worldName) {
        return allowedWorlds.isEmpty() || allowedWorlds.contains(worldName);
    }

    public record DropEntry(String material, int amount, double chance) {
    }
}
