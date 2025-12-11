package io.github.sefiraat.networks.network.stackcaches;

import io.github.sefiraat.networks.utils.Theme;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

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

    public void addMetaLore(ItemMeta itemMeta) {
        // Ambil lore sebagai Component-list modern API
        List<Component> lore = itemMeta.lore() != null ? new ArrayList<>(itemMeta.lore()) : new ArrayList<>();

        // tambahkan baris sesuai format lama (menggunakan Theme.*). Kita menyimpan sebagai Component
        lore.add(Component.empty());

        String holdingName = (this.getItemMeta() != null && this.getItemMeta().displayName() != null)
                ? PlainTextComponentSerializer.plainText().serialize(this.getItemMeta().displayName())
                : (this.getItemStack() != null ? this.getItemStack().getType().name() : "UNKNOWN");

        lore.add(LegacyComponentSerializer.legacySection().deserialize(
                Theme.CLICK_INFO + "Holding: " + holdingName
        ));
        lore.add(LegacyComponentSerializer.legacySection().deserialize(
                Theme.CLICK_INFO + "Amount: " + this.getAmount()
        ));
        if (this.supportsCustomMaxAmount) {
            lore.add(LegacyComponentSerializer.legacySection().deserialize(
                    Theme.CLICK_INFO + "Current capacity limit: " + Theme.ERROR + this.getLimit()
            ));
        }

        // tulis kembali lore sebagai Component-list
        itemMeta.lore(lore);
    }

    public void updateMetaLore(ItemMeta itemMeta) {
        List<Component> lore = itemMeta.lore() != null ? new ArrayList<>(itemMeta.lore()) : new ArrayList<>();
        final int loreIndexModifier = this.supportsCustomMaxAmount ? 1 : 0;

        // pastikan ukuran lore cukup untuk meng-set index dari belakang seperti logika lama
        // (lore.size() - 2) dan (lore.size() - 1) harus ada
        while (lore.size() < 2 + loreIndexModifier) {
            lore.add(Component.empty());
        }

        String holdingName = (this.getItemMeta() != null && this.getItemMeta().displayName() != null)
                ? PlainTextComponentSerializer.plainText().serialize(this.getItemMeta().displayName())
                : (this.getItemStack() != null ? this.getItemStack().getType().name() : "UNKNOWN");

        // set baris-barus terakhir sesuai posisi di implementasi lama
        lore.set(lore.size() - 2, LegacyComponentSerializer.legacySection().deserialize(
                Theme.CLICK_INFO + "Holding: " + holdingName
        ));
        lore.set(lore.size() - 1, LegacyComponentSerializer.legacySection().deserialize(
                Theme.CLICK_INFO + "Amount: " + this.getAmount()
        ));
        if (this.supportsCustomMaxAmount) {
            lore.set(lore.size() - loreIndexModifier, LegacyComponentSerializer.legacySection().deserialize(
                    Theme.CLICK_INFO + "Current capacity limit: " + Theme.ERROR + this.getLimit()
            ));
        }

        itemMeta.lore(lore);
    }
}
