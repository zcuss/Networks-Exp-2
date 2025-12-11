package io.github.sefiraat.networks.slimefun.tools;

import io.github.sefiraat.networks.network.stackcaches.BlueprintInstance;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.StringUtils;
import io.github.sefiraat.networks.utils.Theme;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentCraftingBlueprintType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.attributes.DistinctiveItem;
import io.github.thebusybiscuit.slimefun4.implementation.items.blocks.UnplaceableBlock;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class CraftingBlueprint extends UnplaceableBlock implements DistinctiveItem {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacySection();
    private static final PlainTextComponentSerializer PLAIN = PlainTextComponentSerializer.plainText();

    public CraftingBlueprint(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    @ParametersAreNonnullByDefault
    public static void setBlueprint(ItemStack blueprint, ItemStack[] recipe, ItemStack output) {
        ItemMeta itemMeta = blueprint.getItemMeta();
        ItemMeta outputMeta = output.getItemMeta();

        DataTypeMethods.setCustom(itemMeta, Keys.BLUEPRINT_INSTANCE, PersistentCraftingBlueprintType.TYPE, new BlueprintInstance(recipe, output));

        List<Component> loreComponents = new ArrayList<>();
        loreComponents.add(LEGACY.deserialize(Theme.CLICK_INFO + "Assigned Recipe"));

        for (ItemStack item : recipe) {
            if (item == null) {
                loreComponents.add(LEGACY.deserialize(Theme.PASSIVE + "Nothing"));
                continue;
            }

            ItemMeta m = item.getItemMeta();
            if (m != null && m.hasDisplayName()) {
                Component name = m.displayName();
                String plain = PLAIN.serialize(name);
                loreComponents.add(LEGACY.deserialize(Theme.PASSIVE + plain));
            } else {
                loreComponents.add(LEGACY.deserialize(Theme.PASSIVE + StringUtils.toTitleCase(item.getType().name())));
            }
        }

        loreComponents.add(Component.empty());
        loreComponents.add(LEGACY.deserialize(Theme.CLICK_INFO + "Outputting"));

        if (outputMeta != null && outputMeta.hasDisplayName()) {
            Component name = outputMeta.displayName();
            String plain = PLAIN.serialize(name);
            loreComponents.add(LEGACY.deserialize(Theme.PASSIVE + plain));
        } else {
            loreComponents.add(LEGACY.deserialize(Theme.PASSIVE + StringUtils.toTitleCase(output.getType().name())));
        }

        Method setLoreMethod = null;
        for (Method m : itemMeta.getClass().getMethods()) {
            if (m.getName().equals("setLore") && m.getParameterCount() == 1 && List.class.isAssignableFrom(m.getParameterTypes()[0])) {
                setLoreMethod = m;
                break;
            }
        }

        try {
            if (setLoreMethod != null) {
                try {
                    setLoreMethod.invoke(itemMeta, loreComponents);
                } catch (IllegalArgumentException | InvocationTargetException ex) {
                    List<String> fallback = new ArrayList<>();
                    for (Component c : loreComponents) fallback.add(LEGACY.serialize(c));
                    setLoreMethod.invoke(itemMeta, fallback);
                }
            }
        } catch (IllegalAccessException | InvocationTargetException ignored) {}

        blueprint.setItemMeta(itemMeta);
    }

    @Override
    public boolean canStack(@Nonnull ItemMeta itemMetaOne, @Nonnull ItemMeta itemMetaTwo) {
        return itemMetaOne.getPersistentDataContainer().equals(itemMetaTwo.getPersistentDataContainer());
    }
}
