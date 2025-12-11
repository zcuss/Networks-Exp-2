package io.github.sefiraat.networks.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemCreator {

    private static Component toComponent(String text) {
        return LegacyComponentSerializer.legacySection().deserialize(Utils.color(text));
    }

    private static List<Component> toComponentLore(String... lore) {
        List<Component> list = new ArrayList<>();
        for (String line : lore) {
            list.add(LegacyComponentSerializer.legacySection().deserialize(Utils.color(line)));
        }
        return list;
    }

    public static ItemStack create(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(toComponent(name));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(toComponent(name));
        meta.lore(toComponentLore(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(ItemStack item, String name, String... lore) {
        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(toComponent(name));
        meta.lore(toComponentLore(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(ItemStack item, String name) {
        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        meta.displayName(toComponent(name));
        item.setItemMeta(meta);
        return item;
    }
}
