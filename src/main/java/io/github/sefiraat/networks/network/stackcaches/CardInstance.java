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
        List<String> lore = safeGetLore(itemMeta);
        // pastikan ukuran lore cukup sebelum set
        while (lore.size() <= 10) {
            lore.add("");
        }
        lore.set(10, getLoreLine());
        safeSetLore(itemMeta, lore);
    }

    public String getLoreLine() {
        if (this.getItemStack() == null) {
            return Theme.WARNING + "Empty";
        }
        ItemMeta itemMeta = this.getItemMeta();
        String name;
        if (itemMeta != null && hasDisplayName(itemMeta)) {
            name = getDisplayNamePlain(itemMeta);
        } else if (this.getItemType() != null) {
            name = this.getItemType().name();
        } else {
            name = "Unknown/Error";
        }
        return Theme.CLICK_INFO + name + ": " + Theme.PASSIVE + this.amount;
    }

    // -----------------------
    // Helper compatibility API
    // -----------------------

    /**
     * Ambil display name sebagai plain text.
     * - Jika runtime menyediakan ItemMeta.displayName() (Adventure Component), gunakan it.
     * - Jika tidak, fallback ke deprecated getDisplayName() (dipanggil melalui helper disuppress).
     */
    private static String getDisplayNamePlain(@Nonnull ItemMeta meta) {
        try {
            // Modern API: ItemMeta#displayName() -> Component
            Component comp = meta.displayName();
            if (comp != null) {
                return PlainTextComponentSerializer.plainText().serialize(comp);
            }
        } catch (NoSuchMethodError | AbstractMethodError ignored) {
            // fallback ke deprecated method di bawah
        } catch (Throwable ignored) {
            // unexpected, fallback
        }

        // fallback (deprecated) — suppressed in helper
        return legacyGetDisplayNameAndStrip(meta);
    }

    /**
     * Periksa keberadaan display name secara kompatibel.
     */
    private static boolean hasDisplayName(@Nonnull ItemMeta meta) {
        try {
            // Modern API
            Component comp = meta.displayName();
            return comp != null && !PlainTextComponentSerializer.plainText().serialize(comp).isBlank();
        } catch (NoSuchMethodError | AbstractMethodError ignored) {
            // fallback
        } catch (Throwable ignored) {
            // ignore
        }
        return legacyHasDisplayName(meta);
    }

    /**
     * Dapatkan lore, fallback ke list kosong bila null.
     * Panggilan getLore() deprecated dibungkus di sini untuk mencegah peringatan di tempat lain.
     */
    @Nonnull
    private static List<String> safeGetLore(@Nonnull ItemMeta meta) {
        try {
            // Modern runtimes masih mengembalikan List<String> untuk lore, tapi tetap aman.
            List<String> lore = legacyGetLore(meta);
            return lore == null ? new ArrayList<>() : new ArrayList<>(lore);
        } catch (Throwable t) {
            // kalau ada masalah, kembalikan list kosong
            return new ArrayList<>();
        }
    }

    /**
     * Set lore (bungkus panggilan deprecated setLore di sini untuk menekan peringatan).
     */
    private static void safeSetLore(@Nonnull ItemMeta meta, @Nonnull List<String> lore) {
        try {
            legacySetLore(meta, lore);
        } catch (Throwable ignored) {
            // ignore failures
        }
    }

    // -----------------------
    // Deprecated-call helpers (suppress warnings here only)
    // -----------------------

    private static boolean legacyHasDisplayName(@Nonnull ItemMeta meta) {
        return meta.hasDisplayName();
    }

    @SuppressWarnings("deprecation")
    private static String legacyGetDisplayNameAndStrip(@Nonnull ItemMeta meta) {
        try {
            String raw = meta.getDisplayName();
            if (raw == null) return "";
            // Jika ada format warna legacy, strip menggunakan LegacyComponentSerializer -> plain text
            Component comp = LegacyComponentSerializer.legacySection().deserialize(raw);
            return PlainTextComponentSerializer.plainText().serialize(comp);
        } catch (Throwable t) {
            // ultimate fallback
            try {
                String raw = meta.getDisplayName();
                return raw == null ? "" : raw.replaceAll("(?i)§[0-9A-FK-OR]", "");
            } catch (Throwable ignored) {
                return "";
            }
        }
    }

    @SuppressWarnings("deprecation")
    private static List<String> legacyGetLore(@Nonnull ItemMeta meta) {
        return meta.getLore();
    }

    @SuppressWarnings("deprecation")
    private static void legacySetLore(@Nonnull ItemMeta meta, @Nonnull List<String> lore) {
        meta.setLore(lore);
    }
}
