package io.github.sefiraat.networks.slimefun.network;

import com.bgsoftware.wildchests.api.WildChestsAPI;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.listeners.BlockStateRefreshListener;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.thebusybiscuit.slimefun4.api.MinecraftVersion;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

public class NetworkVanillaGrabber extends NetworkDirectional {

    private static final int[] BACKGROUND_SLOTS = new int[]{
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 15, 16, 17, 18, 20, 22, 23, 24, 26, 27, 28, 30, 31, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int OUTPUT_SLOT = 25;
    private static final int NORTH_SLOT = 11;
    private static final int SOUTH_SLOT = 29;
    private static final int EAST_SLOT = 21;
    private static final int WEST_SLOT = 19;
    private static final int UP_SLOT = 14;
    private static final int DOWN_SLOT = 32;

    public NetworkVanillaGrabber(ItemGroup itemGroup,
                                 SlimefunItemStack item,
                                 RecipeType recipeType,
                                 ItemStack[] recipe
    ) {
        super(itemGroup, item, recipeType, recipe, NodeType.PUSHER);
        this.getSlotsToDrop().add(OUTPUT_SLOT);
    }

    @Override
    protected void onTick(@Nullable BlockMenu blockMenu, @Nonnull Block block) {
        super.onTick(blockMenu, block);
        if (blockMenu != null) {
            tryGrabItem(blockMenu);
        }
    }

    private void tryGrabItem(@Nonnull BlockMenu blockMenu) {

        final ItemStack itemInSlot = blockMenu.getItemInSlot(OUTPUT_SLOT);

        if (itemInSlot != null && itemInSlot.getType() != Material.AIR) {
            return;
        }

        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            return;
        }

        final BlockFace direction = getCurrentDirection(blockMenu);
        final Block block = blockMenu.getBlock();
        final Block targetBlock = block.getRelative(direction);

        // Owner might be null/invalid — handle safely
        String ownerInfo = BlockStorage.getLocationInfo(block.getLocation(), OWNER_KEY);
        if (ownerInfo == null || ownerInfo.isBlank()) {
            return;
        }

        final UUID uuid;
        try {
            uuid = UUID.fromString(ownerInfo);
        } catch (IllegalArgumentException e) {
            return;
        }

        final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);

        if (!Slimefun.getProtectionManager().hasPermission(offlinePlayer, targetBlock, Interaction.INTERACT_BLOCK)) {
            return;
        }

        final BlockState blockState = BlockStateRefreshListener.getState(targetBlock);

        if (!(blockState instanceof InventoryHolder holder)) {
            return;
        }

        boolean wildChests = Networks.getSupportedPluginManager().isWildChests();
        boolean isChest = wildChests && WildChestsAPI.getChest(targetBlock.getLocation()) != null;

        sendDebugMessage(block.getLocation(), "WildChests detected: " + wildChests);
        sendDebugMessage(block.getLocation(), "Block detected as chest: " + isChest);

        if (wildChests && isChest) {
            sendDebugMessage(block.getLocation(), "WildChest test failed, escaping");
            return;
        }

        sendDebugMessage(block.getLocation(), "WildChest test passed.");
        final Inventory inventory = holder.getInventory();

        if (inventory instanceof FurnaceInventory furnaceInventory) {
            final ItemStack furnaceInventoryResult = furnaceInventory.getResult();
            final ItemStack furnaceInventoryFuel = furnaceInventory.getFuel();
            grabItem(blockMenu, furnaceInventoryResult);

            if (furnaceInventoryFuel != null && furnaceInventoryFuel.getType() == Material.BUCKET) {
                grabItem(blockMenu, furnaceInventoryFuel);
            }

        } else if (inventory instanceof BrewerInventory brewerInventory) {
            for (int i = 0; i < 3; i++) {
                final ItemStack stack = brewerInventory.getContents()[i];
                if (stack != null && stack.getType() != Material.AIR) { // safe-null check
                    final PotionMeta potionMeta = (PotionMeta) stack.getItemMeta();
                    if (potionMeta == null) continue;

                    if (Slimefun.getMinecraftVersion().isAtLeast(MinecraftVersion.MINECRAFT_1_20_5)) {
                        // 1.20.5 及以上: gunakan modern API
                        if (potionMeta.getBasePotionType() == PotionType.WATER) {
                            grabItem(blockMenu, stack);
                        }
                    } else {
                        // 1.20.5 以下: pakai helper yang menampung panggilan deprecated
                        if (!isBasePotionWaterCompat(potionMeta)) {
                            grabItem(blockMenu, stack);
                            break;
                        }
                    }
                }
            }
        } else {
            for (ItemStack stack : inventory.getContents()) {
                if (grabItem(blockMenu, stack)) {
                    return;
                }
            }
        }
    }

    private boolean grabItem(@Nonnull BlockMenu blockMenu, @Nullable ItemStack stack) {
        if (stack != null && stack.getType() != Material.AIR) {
            blockMenu.replaceExistingItem(OUTPUT_SLOT, stack.clone());
            stack.setAmount(0);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("deprecation")
    private static boolean isBasePotionWaterCompat(@Nonnull PotionMeta meta) {
        try {
            PotionData pd = meta.getBasePotionData();
            return pd != null && pd.getType() == PotionType.WATER;
        } catch (Throwable ignored) {
            // Jika API tidak tersedia atau berubah, anggap bukan water agar logic tetap aman
            return false;
        }
    }

    @Nonnull
    @Override
    protected int[] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }

    @Override
    public int getNorthSlot() {
        return NORTH_SLOT;
    }

    @Override
    public int getSouthSlot() {
        return SOUTH_SLOT;
    }

    @Override
    public int getEastSlot() {
        return EAST_SLOT;
    }

    @Override
    public int getWestSlot() {
        return WEST_SLOT;
    }

    @Override
    public int getUpSlot() {
        return UP_SLOT;
    }

    @Override
    public int getDownSlot() {
        return DOWN_SLOT;
    }

    @Override
    public boolean runSync() {
        return true;
    }

    @Override
    public int[] getOutputSlots() {
        return new int[]{OUTPUT_SLOT};
    }

    @Override
    protected Particle.DustOptions getDustOptions() {
        return new Particle.DustOptions(Color.MAROON, 1);
    }
}
