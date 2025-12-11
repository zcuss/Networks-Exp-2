package io.github.sefiraat.networks.network.stackcaches;

import io.github.sefiraat.networks.utils.Theme;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class CardInstance extends ItemStackCache {

    private final int limit;
    private int amount;

    public CardInstance(@Nullable ItemStack itemStack, int amount, int limit) {
        super(itemStack);
        this.amount = amount;
        this.limit = limit;
    }

    public int getAmount() {
        return this.amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public int getLimit() {
        return this.limit;
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

    public void increaseAmount(int amount) {
        long total = (long) this.amount + (long) amount;
        if (total > this.limit) {
            this.amount = this.limit;
        } else {
            this.amount = this.amount + amount;
        }
    }

    public void reduceAmount(int amount) {
        this.amount = this.amount - amount;
    }

    public void updateLore(@Nonnull ItemMeta itemMeta) {
        // Ambil lore sebagai List<Component> (modern API)
        List<Component> lore = itemMeta.lore();
        if (lore == null) {
            lore = new ArrayList<>();
        } else {
            // copy to mutable list to be safe
            lore = new ArrayList<>(lore);
        }

        // Pastikan indeks 10 ada
        while (lore.size() <= 10) {
            lore.add(Component.empty());
        }

        // Set line ke-10 (index 10) menggunakan legacy serializer untuk interpretasi &-codes
        lore.set(10, LegacyComponentSerializer.legacySection().deserialize(getLoreLine()));

        // Tulis ulang lore
        itemMeta.lore(lore);
    }

    public String getLoreLine() {
        if (this.getItemStack() == null) {
            return Theme.WARNING + "Empty";
        }
        ItemMeta itemMeta = this.getItemMeta();
        String name;
        if (itemMeta != null && itemMeta.hasDisplayName()) {
            // displayName() mengembalikan Component di modern API — konversi ke plain text
            Component comp = itemMeta.displayName();
            if (comp != null) {
                name = PlainTextComponentSerializer.plainText().serialize(comp);
            } else if (this.getItemType() != null) {
                name = this.getItemType().name();
            } else {
                name = "Unknown/Error";
            }
        } else if (this.getItemType() != null) {
            name = this.getItemType().name();
        } else {
            name = "Unknown/Error";
        }
        // Menghasilkan string dengan kode warna legacy (&-style) — akan di-deserialize saat di-set ke lore
        return Theme.CLICK_INFO + name + ": " + Theme.PASSIVE + this.amount;
    }
}
