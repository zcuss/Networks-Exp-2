package io.github.sefiraat.networks.network.stackcaches;

import io.github.sefiraat.networks.utils.Theme;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class QuantumCache extends ItemStackCache {

    @Nullable
    private final ItemMeta storedItemMeta;
    private final boolean supportsCustomMaxAmount = false;
    @Getter
    @Setter
    private int limit;
    @Setter
    @Getter
    private int amount;
    @Getter
    @Setter
    private boolean voidExcess;

    public QuantumCache(@Nullable ItemStack storedItem, int amount, int limit, boolean voidExcess) {
        super(storedItem);
        this.storedItemMeta = storedItem == null ? null : storedItem.getItemMeta();
        this.amount = amount;
        this.limit = limit;
        this.voidExcess = voidExcess;
    }

    @Nullable
    public ItemMeta getStoredItemMeta() {
        return this.storedItemMeta;
    }

    public boolean supportsCustomMaxAmount() {
        return this.supportsCustomMaxAmount;
    }

    public int increaseAmount(int amount) {
        long total = (long) this.amount + (long) amount;
        if (total > this.limit) {
            this.amount = this.limit;
            if (!this.voidExcess) {
                return (int) (total - this.limit);
            }
        } else {
            this.amount = this.amount + amount;
        }
        return 0;
    }

    public void reduceAmount(int amount) {
        this.amount = this.amount - amount;
    }

    @Nullable
    public ItemStack withdrawItem(int amount) {
        if (this.getItemStack() == null) {
            return null;
        }
        final ItemStack clone = this.getItemStack().clone();
        clone.setAmount(Math.min(this.amount, amount));
        reduceAmount(clone.getAmount());
        return clone;
    }

    @Nullable
    public ItemStack withdrawItem() {
        if (this.getItemStack() == null) {
            return null;
        }
        return withdrawItem(this.getItemStack().getMaxStackSize());
    }

    private String getReadableDisplayName(ItemMeta meta) {
        if (meta == null) return "";
        // Jika ada display name (Component), serialize ke teks legacy untuk menampilkan
        if (meta.hasDisplayName()) {
            Component comp = meta.displayName();
            return LegacyComponentSerializer.legacySection().serialize(comp);
        }
        return "";
    }

    public void addMetaLore(ItemMeta itemMeta) {
        final List<Component> loreComponents = itemMeta.lore() != null ? new ArrayList<>(itemMeta.lore()) : new ArrayList<>();
        // kosongkan baris pemisah (sama seperti sebelumnya)
        loreComponents.add(Component.text(""));

        // Nama yang ditampilkan: prioritas ke ItemMeta displayName (Component) jika ada, fallback ke material name
        String holdingName = this.getItemMeta() != null
        ? getReadableDisplayName(this.getItemMeta())
        : (this.getItemStack() != null ? this.getItemStack().getType().name() : "");


        loreComponents.add(Component.text(Theme.CLICK_INFO + "Holding: " + holdingName));
        loreComponents.add(Component.text(Theme.CLICK_INFO + "Amount: " + this.getAmount()));
        if (this.supportsCustomMaxAmount) {
            loreComponents.add(Component.text(Theme.CLICK_INFO + "Current capacity limit: " + Theme.ERROR + this.getLimit()));
        }

        itemMeta.lore(loreComponents);
    }

    public void updateMetaLore(ItemMeta itemMeta) {
        final List<Component> loreComponents = itemMeta.lore() != null ? new ArrayList<>(itemMeta.lore()) : new ArrayList<>();
        if (loreComponents.isEmpty()) {
            // nothing to update
            itemMeta.lore(loreComponents);
            return;
        }

        // Hitung index yang benar berdasarkan apakah capacity line ada atau tidak.
        // Jika supportsCustomMaxAmount == true, expected tail is: ... , <Holding>, <Amount>, <Capacity>
        // Jika false, expected tail is: ... , <Holding>, <Amount>
        int size = loreComponents.size();
        if (this.supportsCustomMaxAmount) {
            // butuh minimal 3 lines after possible prefix; jika tidak, jangan NPE
            if (size < 3) {
                // fallback: append entries if structure tidak sesuai
                addMetaLore(itemMeta);
                return;
            }
            int holdingIndex = size - 3;
            int amountIndex = size - 2;
            int capacityIndex = size - 1;

            String holdingName = (this.getItemMeta() != null && this.getItemMeta().hasDisplayName()
                    ? LegacyComponentSerializer.legacySection().serialize(this.getItemMeta().displayName())
                    : (this.getItemStack() != null ? this.getItemStack().getType().name() : "")
            );

            loreComponents.set(holdingIndex, Component.text(Theme.CLICK_INFO + "Holding: " + holdingName));
            loreComponents.set(amountIndex, Component.text(Theme.CLICK_INFO + "Amount: " + this.getAmount()));
            loreComponents.set(capacityIndex, Component.text(Theme.CLICK_INFO + "Current capacity limit: " + Theme.ERROR + this.getLimit()));
        } else {
            // supportsCustomMaxAmount == false
            if (size < 2) {
                // struktur tak terduga -> buat ulang
                addMetaLore(itemMeta);
                return;
            }
            int holdingIndex = size - 2;
            int amountIndex = size - 1;

            String holdingName = (this.getItemMeta() != null && this.getItemMeta().hasDisplayName()
                    ? LegacyComponentSerializer.legacySection().serialize(this.getItemMeta().displayName())
                    : (this.getItemStack() != null ? this.getItemStack().getType().name() : "")
            );

            loreComponents.set(holdingIndex, Component.text(Theme.CLICK_INFO + "Holding: " + holdingName));
            loreComponents.set(amountIndex, Component.text(Theme.CLICK_INFO + "Amount: " + this.getAmount()));
        }

        itemMeta.lore(loreComponents);
    }
}
