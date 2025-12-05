package io.github.sefiraat.networks.slimefun.network;

// GANTI seluruh blok import di file dengan yang ini
import dev.sefiraat.sefilib.misc.ParticleUtils;
import dev.sefiraat.sefilib.world.LocationUtils;
import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.Networks;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.sefiraat.networks.utils.ItemCreator;
import io.github.sefiraat.networks.utils.Theme;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.libraries.dough.blocks.BlockPosition;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun; // <- pastikan ada
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;
import org.bukkit.OfflinePlayer;
import org.bukkit.Bukkit;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;


/**
 * NetworkControlX rewritten to avoid direct NMS / CraftBukkit usage.
 */
public class NetworkControlX extends NetworkDirectional {

    public static final ItemStack TEMPLATE_BACKGROUND_STACK = ItemCreator.create(
            Material.BLUE_STAINED_GLASS_PANE,
            Theme.PASSIVE + "Cut items matching template.",
            Theme.PASSIVE + "Leaving blank will cut anything"
    );
    private static final int[] BACKGROUND_SLOTS = new int[] {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 15, 17, 18, 20, 22, 23, 24, 26, 27, 28, 30, 31, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44
    };
    private static final int[] TEMPLATE_BACKGROUND = new int[] {16};
    private static final int TEMPLATE_SLOT = 25;
    private static final int NORTH_SLOT = 11;
    private static final int SOUTH_SLOT = 29;
    private static final int EAST_SLOT = 21;
    private static final int WEST_SLOT = 19;
    private static final int UP_SLOT = 14;
    private static final int DOWN_SLOT = 32;
    private static final int REQUIRED_POWER = 100;
    private static final Particle.DustOptions DUST_OPTIONS = new Particle.DustOptions(Color.GRAY, 1);

    private final Set<BlockPosition> blockCache = new HashSet<>();

    public NetworkControlX(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.CUTTER);
    }

    @Override
    protected void onTick(@Nullable BlockMenu blockMenu, @Nonnull Block block) {
        super.onTick(blockMenu, block);
        if (blockMenu != null) {
            tryBreakBlock(blockMenu);
        }
    }

    @Override
    protected void onUniqueTick() {
        this.blockCache.clear();
    }

    private void tryBreakBlock(@Nonnull BlockMenu blockMenu) {
        final NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(blockMenu.getLocation());

        if (definition == null || definition.getNode() == null) {
            return;
        }

        if (definition.getNode().getRoot().getRootPower() < REQUIRED_POWER) {
            return;
        }

        final BlockFace direction = getCurrentDirection(blockMenu);
        if (direction == BlockFace.SELF) return;

        final Block targetBlock = blockMenu.getBlock().getRelative(direction);
        final BlockPosition targetPosition = new BlockPosition(targetBlock);

        if (this.blockCache.contains(targetPosition)) {
            return;
        }

        final Material material = targetBlock.getType();

        // skip unbreakable / air
        try {
            // getHardness() mungkin tidak ada di beberapa runtime -> bisa lempar NoSuchMethodError
            if (material.getHardness() < 0 || material.isAir()) {
                return;
            }
        } catch (NoSuchMethodError nsme) {
            // Jika getHardness() tidak tersedia, tetap lakukan check isAir() jika mungkin.
            try {
                if (material.isAir()) return;
            } catch (Throwable ignored) {
                // Jika isAir() juga bermasalah, lanjutkan (anggap breakable)
            }
        } catch (Throwable ignored) {
            // General fallback: jika isAir() tersedia dan true -> skip; jika tidak, anggap breakable.
            try {
                if (material.isAir()) return;
            } catch (Throwable ignored2) {
                // nothing we can do here; assume breakable
            }
        }

        final SlimefunItem slimefunItem = BlockStorage.check(targetBlock);
        if (slimefunItem != null) return;

        final ItemStack templateStack = blockMenu.getItemInSlot(TEMPLATE_SLOT);
        boolean hasTemplate = templateStack != null && !templateStack.getType().isAir();

        if (hasTemplate) {
            if (targetBlock.getType() != templateStack.getType() || SlimefunItem.getByItem(templateStack) != null) {
                return;
            }
        }

        // Owner check (may be null -> handle safely)
        String ownerStr = BlockStorage.getLocationInfo(blockMenu.getLocation(), OWNER_KEY);
        if (ownerStr != null && !ownerStr.isBlank()) {
            try {
                UUID uuid = UUID.fromString(ownerStr);
                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                if (!Slimefun.getProtectionManager().hasPermission(offlinePlayer, targetBlock, Interaction.BREAK_BLOCK)) {
                    return;
                }
            } catch (IllegalArgumentException ignored) {
                // invalid UUID -> ignore owner check
            }
        }

        // Excluded block types
        if (isSensitiveContainer(material)) {
            return;
        }

        final ItemStack resultStack = new ItemStack(material, 1);

        // add item to root (this handles storing amounts etc.)
        definition.getNode().getRoot().addItemStack0(blockMenu.getBlock().getLocation(), resultStack);

        // If resultStack amount becomes zero, we need to actually remove the block from world.
        if (resultStack.getAmount() == 0) {
            this.blockCache.add(targetPosition);

            // run on main thread
            Bukkit.getScheduler().runTask(Networks.getInstance(), () -> {
                try {
                    // Remove block without causing neighbor physics
                    targetBlock.setType(Material.AIR, false);

                    // Display particle effect
                    ParticleUtils.displayParticleRandomly(
                            LocationUtils.centre(targetBlock.getLocation()),
                            1,
                            5,
                            DUST_OPTIONS
                    );

                    // Deduct power for successful cut
                    definition.getNode().getRoot().removeRootPower(REQUIRED_POWER);

                    // If lighting/visuals are problematic in your test, you can optionally force a chunk refresh:
                    // try { targetBlock.getWorld().refreshChunk(targetBlock.getX() >> 4, targetBlock.getZ() >> 4); } catch (Throwable ignored) {}

                } catch (Throwable t) {
                    Bukkit.getLogger().finer("Failed to remove block at " + targetBlock.getLocation() + ": " + t.getMessage());
                }
            });
        }
    }

    private static boolean isSensitiveContainer(Material m) {
        if (m == Material.CHEST
                || m == Material.TRAPPED_CHEST
                || m == Material.ENDER_CHEST
                || m == Material.BARREL
                || m == Material.SHULKER_BOX
                || m.name().endsWith("_SHULKER_BOX")
                || m.name().endsWith("_SHELF")
                || m == Material.FURNACE
                || m == Material.BLAST_FURNACE
                || m == Material.SMOKER
                || m == Material.HOPPER
                || m == Material.DROPPER
                || m == Material.DISPENSER
                || m == Material.BREWING_STAND
        ) {
            return true;
        }
        return false;
    }

    @Nonnull
    @Override
    protected int[] getBackgroundSlots() {
        return BACKGROUND_SLOTS;
    }

    @Nullable
    @Override
    protected int[] getOtherBackgroundSlots() {
        return TEMPLATE_BACKGROUND;
    }

    @Nullable
    @Override
    protected ItemStack getOtherBackgroundStack() {
        return TEMPLATE_BACKGROUND_STACK;
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
    public int[] getItemSlots() {
        return new int[]{TEMPLATE_SLOT};
    }

    @Override
    protected Particle.DustOptions getDustOptions() {
        return DUST_OPTIONS;
    }
}
