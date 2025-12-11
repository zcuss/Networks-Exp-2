package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.network.stackcaches.QuantumCache;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.Keys;
import io.github.sefiraat.networks.utils.Theme;
import io.github.sefiraat.networks.utils.datatypes.DataTypeMethods;
import io.github.sefiraat.networks.utils.datatypes.PersistentQuantumStorageType;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockBreakHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.utils.SlimefunUtils;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NetworkQuantumWorkbench extends SlimefunItem {

    private static final int[] BACKGROUND_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 13, 14, 15, 16, 17, 18, 22, 24, 26, 27, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int[] RECIPE_SLOTS = {
            10, 11, 12, 19, 20, 21, 28, 29, 30
    };
    private static final int CRAFT_SLOT = 23;
    private static final int OUTPUT_SLOT = 25;

    private static final ItemStack CRAFT_BUTTON_STACK = ItemCreator.create(
            Material.CRAFTING_TABLE,
            Theme.CLICK_INFO + "Click to entangle"
    );

    private static final Map<ItemStack[], ItemStack> RECIPES = new HashMap<>();

    public static final RecipeType TYPE = new RecipeType(
            Keys.newKey("quantum-workbench"),
            Theme.themedItemStack(
                    Material.BRAIN_CORAL_BLOCK,
                    Theme.MACHINE,
                    "Quantum Workbench",
                    "Crafted using the Quantum Workbench."
            ),
            NetworkQuantumWorkbench::addRecipe
    );

    @ParametersAreNonnullByDefault
    public NetworkQuantumWorkbench(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe);
    }

    public static void addRecipe(ItemStack[] input, ItemStack output) {
        RECIPES.put(input, output);
    }

    @Override
    public void preRegister() {
        addItemHandler(getBlockBreakHandler());
    }

    @Override
    public void postRegister() {
        new BlockMenuPreset(this.getId(), this.getItemName()) {
            @Override
            public void init() {
                drawBackground(BACKGROUND_SLOTS);
                addItem(CRAFT_SLOT, CRAFT_BUTTON_STACK, (p, slot, item, action) -> false);
            }

            @Override
            public boolean canOpen(@Nonnull Block block, @Nonnull Player player) {
                return BlockStorage.check(block).canUse(player, false)
                        && Slimefun.getProtectionManager().hasPermission(player, block.getLocation(), Interaction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                if (flow == ItemTransportFlow.WITHDRAW) {
                    return new int[]{OUTPUT_SLOT};
                }
                return new int[0];
            }

            @Override
            public void newInstance(@Nonnull BlockMenu menu, @Nonnull Block b) {
                menu.addMenuClickHandler(CRAFT_SLOT, (p, slot, item, action) -> {
                    craft(menu);
                    return false;
                });
            }
        };
    }

    public void craft(@Nonnull BlockMenu menu) {
        // Ambil item di output
        final ItemStack itemInOutput = menu.getItemInSlot(OUTPUT_SLOT);

        // Ambil input dari slot resep
        final ItemStack[] inputs = new ItemStack[RECIPE_SLOTS.length];
        int i = 0;
        for (int recipeSlot : RECIPE_SLOTS) {
            inputs[i] = menu.getItemInSlot(recipeSlot);
            i++;
        }

        // Cari resep yang cocok
        ItemStack crafted = null;
        for (Map.Entry<ItemStack[], ItemStack> entry : RECIPES.entrySet()) {
            if (testRecipe(inputs, entry.getKey())) {
                crafted = entry.getValue().clone();
                break;
            }
        }

        if (crafted == null) {
            return; // tidak ada resep cocok
        }

        // Transfer metadata Quantum jika perlu (sama seperti logika sebelumnya)
        final ItemStack coreItem = inputs[4]; // core berada di tengah resep
        final SlimefunItem oldQuantum = SlimefunItem.getByItem(coreItem);

        if (oldQuantum instanceof NetworkQuantumStorage) {
            final ItemMeta oldMeta = coreItem.getItemMeta();
            final ItemMeta newMeta = crafted.getItemMeta();
            final NetworkQuantumStorage newQuantum = (NetworkQuantumStorage) SlimefunItem.getByItem(crafted);
            final QuantumCache oldCache = DataTypeMethods.getCustom(oldMeta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE);

            if (oldCache != null) {
                final QuantumCache newCache = new QuantumCache(
                        oldCache.getItemStack().clone(),
                        oldCache.getAmount(),
                        newQuantum.getMaxAmount(),
                        oldCache.isVoidExcess()
                );
                DataTypeMethods.setCustom(newMeta, Keys.QUANTUM_STORAGE_INSTANCE, PersistentQuantumStorageType.TYPE, newCache);
                newCache.addMetaLore(newMeta);
                crafted.setItemMeta(newMeta);
            }
        }

        // Jumlah yang dihasilkan per satu kali craft (biasanya 1)
        int perCraftAmount = Math.max(1, crafted.getAmount());

        // Pastikan semua slot input punya minimal 1 item (kita hanya craft 1x)
        for (int recipeSlot : RECIPE_SLOTS) {
            ItemStack s = menu.getItemInSlot(recipeSlot);
            if (s == null || s.getAmount() <= 0) {
                return; // ada slot kosong / tidak cukup bahan -> batal
            }
        }

        // Batas keras per-slot output
        final int HARD_MAX = 64;

        // Cek apakah output boleh diisi / ditumpuk
        if (itemInOutput == null) {
            // kosong: pastikan kita bisa menaruh perCraftAmount sampai HARD_MAX
            if (perCraftAmount > HARD_MAX) {
                return; // hasil craft melebihi batas per-slot
            }
        } else {
            // ada item di output: hanya boleh ditumpuk jika sama
            if (!SlimefunUtils.isItemSimilar(itemInOutput, crafted, true, false, false)) {
                return; // beda item -> batal
            }
            int effectiveMaxStack = Math.min(HARD_MAX, itemInOutput.getMaxStackSize());
            if (itemInOutput.getAmount() + perCraftAmount > effectiveMaxStack) {
                return; // tidak ada ruang cukup untuk satu craft -> batal
            }
        }

        // Siapkan ItemStack untuk dimasukkan (hanya 1x craft)
        ItemStack toPush = crafted.clone();
        toPush.setAmount(perCraftAmount);

        // Masukkan ke output (menu.pushItem biasanya akan menambah/menggabungkan stack)
        menu.pushItem(toPush, OUTPUT_SLOT);

        // Konsumsi bahan: 1 unit per slot (satu kali craft)
        for (int recipeSlot : RECIPE_SLOTS) {
            if (menu.getItemInSlot(recipeSlot) != null) {
                menu.consumeItem(recipeSlot, 1, true);
            }
        }
    }

    private boolean testRecipe(ItemStack[] input, ItemStack[] recipe) {
        for (int test = 0; test < recipe.length; test++) {
            if (!SlimefunUtils.isItemSimilar(input[test], recipe[test], true, false, false)) {
                return false;
            }
        }
        return true;
    }

    private BlockBreakHandler getBlockBreakHandler() {
        return new BlockBreakHandler(false, false) {
            @Override
            public void onPlayerBreak(BlockBreakEvent event, ItemStack itemStack, List<ItemStack> drops) {
                BlockMenu menu = BlockStorage.getInventory(event.getBlock());
                menu.dropItems(menu.getLocation(), RECIPE_SLOTS);
                menu.dropItems(menu.getLocation(), OUTPUT_SLOT);
            }
        };
    }
}
