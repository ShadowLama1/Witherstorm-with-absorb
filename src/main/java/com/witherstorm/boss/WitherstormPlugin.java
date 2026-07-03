package com.witherstorm.boss;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

public final class WitherstormPlugin extends JavaPlugin {

    private static WitherstormPlugin instance;

    private WitherstormConfig config;
    private BossManager bossManager;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadConfigValues();

        this.bossManager = new BossManager(this);
        getServer().getPluginManager().registerEvents(new WitherstormListener(this), this);
        getServer().getPluginManager().registerEvents(new SummonItemListener(this), this);

        getLogger().info("Witherstorm plugin enabled. The storm waits in the dark.");
    }

    @Override
    public void onDisable() {
        if (bossManager != null) {
            bossManager.shutdown();
        }
        HandlerList.unregisterAll(this);
        getLogger().info("Witherstorm plugin disabled.");
    }

    public void loadConfigValues() {
        reloadConfig();
        this.config = new WitherstormConfig(getConfig());
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (args.length == 0) {
            sendHelp(sender, label);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "summon" -> bossManager.handleSummonCommand(sender);
            case "despawn" -> bossManager.handleDespawnCommand(sender);
            case "reload" -> {
                if (!sender.hasPermission("witherstorm.admin")) {
                    sender.sendMessage(miniMessage.deserialize("<red>You lack permission."));
                    return true;
                }
                loadConfigValues();
                sender.sendMessage(miniMessage.deserialize("<green>Witherstorm config reloaded."));
            }
            case "help" -> sendHelp(sender, label);
            default -> sendHelp(sender, label);
        }
        return true;
    }

    @Override
    public @NotNull List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (args.length == 1) {
            return List.of("summon", "despawn", "reload", "help");
        }
        return Collections.emptyList();
    }

    private void sendHelp(CommandSender sender, String label) {
        sender.sendMessage(miniMessage.deserialize("<dark_purple><bold>Witherstorm Commands</bold>"));
        sender.sendMessage(miniMessage.deserialize("<gray>/" + label + " summon <dark_gray>- Summon the Witherstorm at your location."));
        sender.sendMessage(miniMessage.deserialize("<gray>/" + label + " despawn <dark_gray>- Remove an active Witherstorm."));
        sender.sendMessage(miniMessage.deserialize("<gray>/" + label + " reload <dark_gray>- Reload config values."));
        sender.sendMessage(miniMessage.deserialize("<gray>/" + label + " help <dark_gray>- Show this help."));
    }

    public static WitherstormPlugin get() {
        return instance;
    }

    public WitherstormConfig config() {
        return config;
    }

    public BossManager bossManager() {
        return bossManager;
    }

    public MiniMessage miniMessage() {
        return miniMessage;
    }
}
