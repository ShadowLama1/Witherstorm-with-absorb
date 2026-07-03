package com.witherstorm.boss;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;

/**
 * Builds the custom items used by the plugin: the Witherstorm Heart (summon
 * catalyst) and the Command Block Fragment (mythic drop). Both carry lore and
 * a persistent key so the listeners can identify them reliably.
 */
public final class DropFactory {

    private static final String HEART_KEY = "witherstorm_heart";
    private static final String FRAGMENT_KEY = "witherstorm_fragment";

    public static ItemStack witherstormHeart(WitherstormPlugin plugin) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        MiniMessage mm = plugin.miniMessage();
        meta.displayName(mm.deserialize(plugin.config().summonItemName));
        List<Component> lore = plugin.config().summonItemLore.stream()
                .map(mm::deserialize)
                .toList();
        meta.lore(lore);

        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);

        NamespacedKey key = new NamespacedKey(plugin, HEART_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isWitherstormHeart(WitherstormPlugin plugin, ItemStack item) {
        if (item == null || item.getType() != Material.NETHER_STAR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey key = new NamespacedKey(plugin, HEART_KEY);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    public static ItemStack commandBlockFragment(WitherstormPlugin plugin) {
        WitherstormConfig cfg = plugin.config();
        Material mat = Material.matchMaterial(cfg.commandBlockFragmentMaterial);
        if (mat == null) mat = Material.STRUCTURE_BLOCK;
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        MiniMessage mm = plugin.miniMessage();
        meta.displayName(mm.deserialize(cfg.commandBlockFragmentName));
        List<Component> lore = cfg.commandBlockFragmentLore.stream()
                .map(mm::deserialize)
                .toList();
        meta.lore(lore);

        meta.setCustomModelData(cfg.commandBlockFragmentModelData);
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_DESTROYS);

        NamespacedKey key = new NamespacedKey(plugin, FRAGMENT_KEY);
        meta.getPersistentDataContainer().set(key, PersistentDataType.BYTE, (byte) 1);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isCommandBlockFragment(WitherstormPlugin plugin, ItemStack item) {
        if (item == null) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        NamespacedKey key = new NamespacedKey(plugin, FRAGMENT_KEY);
        return meta.getPersistentDataContainer().has(key, PersistentDataType.BYTE);
    }

    private DropFactory() {
    }
}
