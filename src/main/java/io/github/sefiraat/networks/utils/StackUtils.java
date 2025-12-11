package io.github.sefiraat.networks.utils;

import com.balugaq.netex.utils.Converter;
import io.github.sefiraat.networks.network.stackcaches.ItemStackCache;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.data.persistent.PersistentDataAPI;
import lombok.experimental.UtilityClass;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@UtilityClass
public class StackUtils {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();

    @Nonnull
    public static ItemStack getAsQuantity(@Nonnull ItemStack itemStack, int amount) {
        return Converter.getItem(itemStack, amount);
    }

    public static boolean itemsMatch(@Nullable ItemStack itemStack1, @Nullable ItemStack itemStack2) {
        return itemsMatch(new ItemStackCache(itemStack1), itemStack2, true);
    }

    public static boolean itemsMatch(@Nonnull ItemStackCache cache, @Nullable ItemStack itemStack, boolean checkLore) {
        if (cache.getItemStack() == null || itemStack == null) {
            return itemStack == null && cache.getItemStack() == null;
        }
        if (itemStack.getType() != cache.getItemType()) return false;
        if (!itemStack.hasItemMeta() || !cache.getItemStack().hasItemMeta()) {
            return itemStack.hasItemMeta() == cache.getItemStack().hasItemMeta();
        }
        final ItemMeta itemMeta = itemStack.getItemMeta();
        final ItemMeta cachedMeta = cache.getItemMeta();
        if (!itemMeta.getClass().equals(cachedMeta.getClass())) return false;
        if (canQuickEscapeMetaVariant(itemMeta, cachedMeta)) return false;
        if (hasDisplayName(itemMeta) != hasDisplayName(cachedMeta)) return false;
        final boolean hasCustomOne = itemMeta.hasCustomModelData();
        final boolean hasCustomTwo = cachedMeta.hasCustomModelData();
        if (hasCustomOne) {
            if (!hasCustomTwo || itemMeta.getCustomModelData() != cachedMeta.getCustomModelData()) return false;
        } else if (hasCustomTwo) {
            return false;
        }
        if (!itemMeta.getPersistentDataContainer().equals(cachedMeta.getPersistentDataContainer())) return false;
        if (!itemMeta.getEnchants().equals(cachedMeta.getEnchants())) return false;
        if (!itemMeta.getItemFlags().equals(cachedMeta.getItemFlags())) return false;
        if (checkLore) {
            Component[] a = toComponentArray(itemMeta);
            Component[] b = toComponentArray(cachedMeta);
            if (!Objects.deepEquals(a, b)) return false;
        }
        final Optional<String> optionalStackId1 = Slimefun.getItemDataService().getItemData(itemMeta);
        final Optional<String> optionalStackId2 = Slimefun.getItemDataService().getItemData(cachedMeta);
        if (optionalStackId1.isPresent() && optionalStackId2.isPresent()) {
            return optionalStackId1.get().equals(optionalStackId2.get());
        }
        Component d1 = getDisplayComponent(itemMeta);
        Component d2 = getDisplayComponent(cachedMeta);
        return d1 == null || Objects.equals(d1, d2);
    }

    public boolean canQuickEscapeMetaVariant(@Nonnull ItemMeta metaOne, @Nonnull ItemMeta metaTwo) {
        if (metaOne instanceof Damageable instanceOne && metaTwo instanceof Damageable instanceTwo) {
            if (instanceOne.getDamage() != instanceTwo.getDamage()) return true;
        }
        if (metaOne instanceof AxolotlBucketMeta instanceOne && metaTwo instanceof AxolotlBucketMeta instanceTwo) {
            if (instanceOne.hasVariant() != instanceTwo.hasVariant()) return true;
            if (!instanceOne.hasVariant() || !instanceTwo.hasVariant()) return true;
            if (instanceOne.getVariant() != instanceTwo.getVariant()) return true;
        }
        if (metaOne instanceof BannerMeta instanceOne && metaTwo instanceof BannerMeta instanceTwo) {
            if (!instanceOne.getPatterns().equals(instanceTwo.getPatterns())) return true;
        }
        if (metaOne instanceof BookMeta instanceOne && metaTwo instanceof BookMeta instanceTwo) {
            if (instanceOne.getPageCount() != instanceTwo.getPageCount()) return true;
            if (!Objects.equals(instanceOne.getAuthor(), instanceTwo.getAuthor())) return true;
            if (!Objects.equals(instanceOne.getTitle(), instanceTwo.getTitle())) return true;
            if (!Objects.equals(instanceOne.getGeneration(), instanceTwo.getGeneration())) return true;
        }
        if (metaOne instanceof BundleMeta instanceOne && metaTwo instanceof BundleMeta instanceTwo) {
            if (instanceOne.hasItems() != instanceTwo.hasItems()) return true;
            if (!instanceOne.getItems().equals(instanceTwo.getItems())) return true;
        }
        if (metaOne instanceof CompassMeta instanceOne && metaTwo instanceof CompassMeta instanceTwo) {
            if (instanceOne.isLodestoneTracked() != instanceTwo.isLodestoneTracked()) return true;
            if (!Objects.equals(instanceOne.getLodestone(), instanceTwo.getLodestone())) return true;
        }
        if (metaOne instanceof CrossbowMeta instanceOne && metaTwo instanceof CrossbowMeta instanceTwo) {
            if (instanceOne.hasChargedProjectiles() != instanceTwo.hasChargedProjectiles()) return true;
            if (!instanceOne.getChargedProjectiles().equals(instanceTwo.getChargedProjectiles())) return true;
        }
        if (metaOne instanceof EnchantmentStorageMeta instanceOne && metaTwo instanceof EnchantmentStorageMeta instanceTwo) {
            if (instanceOne.hasStoredEnchants() != instanceTwo.hasStoredEnchants()) return true;
            if (!instanceOne.getStoredEnchants().equals(instanceTwo.getStoredEnchants())) return true;
        }
        if (metaOne instanceof FireworkEffectMeta instanceOne && metaTwo instanceof FireworkEffectMeta instanceTwo) {
            if (!Objects.equals(instanceOne.getEffect(), instanceTwo.getEffect())) return true;
        }
        if (metaOne instanceof FireworkMeta instanceOne && metaTwo instanceof FireworkMeta instanceTwo) {
            if (instanceOne.getPower() != instanceTwo.getPower()) return true;
            if (!instanceOne.getEffects().equals(instanceTwo.getEffects())) return true;
        }
        if (metaOne instanceof LeatherArmorMeta instanceOne && metaTwo instanceof LeatherArmorMeta instanceTwo) {
            if (!instanceOne.getColor().equals(instanceTwo.getColor())) return true;
        }
        if (metaOne instanceof MapMeta instanceOne && metaTwo instanceof MapMeta instanceTwo) {
            Object locOne = invokeOrNull(instanceOne, "locationName");
            Object locTwo = invokeOrNull(instanceTwo, "locationName");
            if (!Objects.equals(locOne, locTwo)) {
                Object g1 = invokeOrNull(instanceOne, "getLocationName");
                Object g2 = invokeOrNull(instanceTwo, "getLocationName");
                if (!Objects.equals(g1, g2)) return true;
            }
            if (instanceOne.hasMapView() != instanceTwo.hasMapView()) return true;
            if (instanceOne.hasColor() != instanceTwo.hasColor()) return true;
            if (!Objects.equals(instanceOne.getMapView(), instanceTwo.getMapView())) return true;
            if (!Objects.equals(instanceOne.getColor(), instanceTwo.getColor())) return true;
        }
        if (metaOne instanceof PotionMeta instanceOne && metaTwo instanceof PotionMeta instanceTwo) {
            if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_20_5)) {
                if (instanceOne.getBasePotionType() != instanceTwo.getBasePotionType()) return true;
            } else {
                Object b1 = invokeOrNull(instanceOne, "basePotionData");
                Object b2 = invokeOrNull(instanceTwo, "basePotionData");
                if (!Objects.equals(b1, b2)) {
                    Object old1 = invokeOrNull(instanceOne, "getBasePotionData");
                    Object old2 = invokeOrNull(instanceTwo, "getBasePotionData");
                    if (!Objects.equals(old1, old2)) return true;
                }
            }
            if (instanceOne.hasCustomEffects() != instanceTwo.hasCustomEffects()) return true;
            if (instanceOne.hasColor() != instanceTwo.hasColor()) return true;
            if (!Objects.equals(instanceOne.getColor(), instanceTwo.getColor())) return true;
            if (!instanceOne.getCustomEffects().equals(instanceTwo.getCustomEffects())) return true;
        }
        if (metaOne instanceof SkullMeta instanceOne && metaTwo instanceof SkullMeta instanceTwo) {
            if (instanceOne.hasOwner() != instanceTwo.hasOwner()) return true;
            if (!Objects.equals(instanceOne.getOwningPlayer(), instanceTwo.getOwningPlayer())) return true;
        }
        if (metaOne instanceof SuspiciousStewMeta instanceOne && metaTwo instanceof SuspiciousStewMeta instanceTwo) {
            if (!Objects.equals(instanceOne.getCustomEffects(), instanceTwo.getCustomEffects())) return true;
        }
        if (metaOne instanceof TropicalFishBucketMeta instanceOne && metaTwo instanceof TropicalFishBucketMeta instanceTwo) {
            if (instanceOne.hasVariant() != instanceTwo.hasVariant()) return true;
            if (!instanceOne.getPattern().equals(instanceTwo.getPattern())) return true;
            if (!instanceOne.getBodyColor().equals(instanceTwo.getBodyColor())) return true;
            return !instanceOne.getPatternColor().equals(instanceTwo.getPatternColor());
        }
        return false;
    }

    @ParametersAreNonnullByDefault
    public static void putOnCooldown(ItemStack itemStack, int durationInSeconds) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            PersistentDataAPI.setLong(itemMeta, Keys.ON_COOLDOWN, System.currentTimeMillis() + (durationInSeconds * 1000L));
            itemStack.setItemMeta(itemMeta);
        }
    }

    @ParametersAreNonnullByDefault
    public static boolean isOnCooldown(ItemStack itemStack) {
        ItemMeta itemMeta = itemStack.getItemMeta();
        if (itemMeta != null) {
            long cooldownUntil = PersistentDataAPI.getLong(itemMeta, Keys.ON_COOLDOWN, 0);
            return System.currentTimeMillis() < cooldownUntil;
        }
        return false;
    }

    private static Component getDisplayComponent(ItemMeta meta) {
        try {
            return meta.displayName();
        } catch (NoSuchMethodError | AbstractMethodError e) {
            Object v = invokeOrNull(meta, "getDisplayName");
            if (v instanceof String) return LEGACY.deserialize((String) v);
            return null;
        }
    }

    private static Component[] toComponentArray(ItemMeta meta) {
        try {
            List<Component> lore = meta.lore();
            if (lore == null) return null;
            return lore.toArray(new Component[0]);
        } catch (NoSuchMethodError | AbstractMethodError e) {
            Object maybe = invokeOrNull(meta, "getLore");

            if (!(maybe instanceof List<?>)) return null;

            @SuppressWarnings("unchecked")
            List<String> legacy = (List<String>) maybe;

            // Tidak perlu cek legacy == null, karena instanceof sudah menjamin bukan null

            Component[] arr = new Component[legacy.size()];
            for (int i = 0; i < legacy.size(); i++) {
                arr[i] = LEGACY.deserialize(legacy.get(i));
            }
            return arr;
        }
    }

    private static boolean hasDisplayName(ItemMeta meta) {
        try {
            return meta.hasDisplayName();
        } catch (NoSuchMethodError | AbstractMethodError e) {
            Object v = invokeOrNull(meta, "hasDisplayName");
            if (v instanceof Boolean) return (Boolean) v;
            return false;
        }
    }

    private static Object invokeOrNull(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            return m.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }
}
