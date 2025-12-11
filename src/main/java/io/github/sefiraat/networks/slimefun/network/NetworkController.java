package io.github.sefiraat.networks.slimefun.network;

import io.github.sefiraat.networks.NetworkStorage;
import io.github.sefiraat.networks.network.NetworkNode;
import io.github.sefiraat.networks.network.NetworkRoot;
import io.github.sefiraat.networks.network.NodeDefinition;
import io.github.sefiraat.networks.network.NodeType;
import io.github.thebusybiscuit.slimefun4.api.events.PlayerRightClickEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.items.settings.IntRangeSetting;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import lombok.Getter;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nonnull;
import java.util.*;

public class NetworkController extends NetworkObject {

    private static final String CRAYON = "crayon";
    private static final Map<Location, NetworkRoot> NETWORKS = new HashMap<>();
    @Getter
    private static final Set<Location> CRAYONS = new HashSet<>();
    protected final Map<Location, Boolean> firstTickMap = new HashMap<>();
    private final ItemSetting<Integer> maxNodes;

    public NetworkController(ItemGroup itemGroup, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, item, recipeType, recipe, NodeType.CONTROLLER);

        maxNodes = new IntRangeSetting(this, "max_nodes", 10, 2000, 5000);
        addItemSetting(maxNodes);

        addItemHandler(
                new BlockTicker() {
                    @Override
                    public boolean isSynchronized() {
                        return false;
                    }

                    @Override
                    public void tick(Block block, SlimefunItem item, Config data) {
                        if (!firstTickMap.containsKey(block.getLocation())) {
                            onFirstTick(block, data);
                            firstTickMap.put(block.getLocation(), true);
                        }

                        addToRegistry(block);
                        NetworkRoot networkRoot = new NetworkRoot(block.getLocation(), NodeType.CONTROLLER, maxNodes.getValue());
                        networkRoot.addAllChildren();

                        NodeDefinition definition = NetworkStorage.getAllNetworkObjects().get(block.getLocation());
                        if (definition != null) {
                            definition.setNode(networkRoot);
                        }

                        boolean crayon = CRAYONS.contains(block.getLocation());
                        if (crayon) {
                            networkRoot.setDisplayParticles(true);
                        }

                        NETWORKS.put(block.getLocation(), networkRoot);
                    }
                }
        );
    }

    public static Map<Location, NetworkRoot> getNetworks() {
        return NETWORKS;
    }

    public static void addCrayon(@Nonnull Location location) {
        BlockStorage.addBlockInfo(location, CRAYON, String.valueOf(true));
        CRAYONS.add(location);
    }

    public static void removeCrayon(@Nonnull Location location) {
        BlockStorage.addBlockInfo(location, CRAYON, null);
        CRAYONS.remove(location);
    }

    public static boolean hasCrayon(@Nonnull Location location) {
        return CRAYONS.contains(location);
    }

    public static void wipeNetwork(@Nonnull Location location) {
        NetworkRoot networkRoot = NETWORKS.remove(location);
        if (networkRoot != null) {
            for (NetworkNode node : networkRoot.getChildrenNodes()) {
                NetworkStorage.removeNode(node.getNodePosition());
            }
        }
    }

    @Override
    protected void prePlace(@Nonnull PlayerRightClickEvent event) {
        Optional<Block> blockOptional = event.getClickedBlock();

        if (blockOptional.isEmpty()) {
            return;
        }

        Block block = blockOptional.get();
        Block target = block.getRelative(event.getClickedFace());

        for (BlockFace checkFace : CHECK_FACES) {
            Block checkBlock = target.getRelative(checkFace);
            SlimefunItem sfItem = BlockStorage.check(checkBlock);

            // If another controller is nearby -> block placement
            if (sfItem instanceof NetworkController) {
                event.getPlayer().sendMessage("§cthis network already has a controller");
                event.cancel();
                return;
            }

            // If the location is already part of an active network -> block placement
            NodeDefinition def = NetworkStorage.getAllNetworkObjects().get(checkBlock.getLocation());
            if (def != null && def.getNode() != null) {
                event.getPlayer().sendMessage("§cthis network already has a controller");
                event.cancel();
                return;
            }
        }
    }


    private void onFirstTick(@Nonnull Block block, @Nonnull Config data) {
        final String crayon = data.getString(CRAYON);
        if (Boolean.parseBoolean(crayon)) {
            CRAYONS.add(block.getLocation());
        }
    }
}
