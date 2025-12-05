package io.github.sefiraat.networks.utils;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Utility pembuat ItemStack sederhana.
 * Menggunakan @SuppressWarnings("deprecation") untuk sementara
 * agar tidak terganggu oleh API deprecated (displayName / lore).
 *
 * Jika ingin migrasi "bersih", saya bisa ubah ke Adventure Components
 * (ItemMeta::displayName(Component) & ItemMeta::lore(List<Component>))
 * dan mengkonversi warna legacy via LegacyComponentSerializer.
 */
@SuppressWarnings("deprecation")
public class ItemCreator {
    public static ItemStack create(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.color(name));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.color(name));
        meta.setLore(Utils.colorLore(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(ItemStack item, String name, String... lore) {
        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.color(name));
        meta.setLore(Utils.colorLore(lore));
        item.setItemMeta(meta);
        return item;
    }

    public static ItemStack create(ItemStack item, String name) {
        item = item.clone();
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(Utils.color(name));
        item.setItemMeta(meta);
        return item;
    }
}
