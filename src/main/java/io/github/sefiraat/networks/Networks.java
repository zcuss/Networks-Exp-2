package io.github.sefiraat.networks;

import com.balugaq.netex.utils.Converter;
import io.github.sefiraat.networks.commands.NetworksMain;
import io.github.sefiraat.networks.integrations.HudCallbacks;
import io.github.sefiraat.networks.integrations.NetheoPlants;
import io.github.sefiraat.networks.managers.ListenerManager;
import io.github.sefiraat.networks.managers.SupportedPluginManager;
import io.github.sefiraat.networks.slimefun.NetworkSlimefunItems;
import io.github.sefiraat.networks.slimefun.NetworksSlimefunItemStacks;
import io.github.sefiraat.networks.slimefun.network.NetworkController;
import io.github.sefiraat.networks.utils.NetworkUtils;
import io.github.thebusybiscuit.slimefun4.api.SlimefunAddon;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.libraries.dough.updater.BlobBuildUpdater;
import lombok.Getter;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Networks extends JavaPlugin implements SlimefunAddon {
    @Getter
    private static final Set<Location> controllersSet = new HashSet<>();

    private static Networks instance;

    private final String username;
    private final String repo;

    private ListenerManager listenerManager;
    private SupportedPluginManager supportedPluginManager;

    public Networks() {
        this.username = "Sefiraat";
        this.repo = "Networks";
    }

    @Nonnull
    public static PluginManager getPluginManager() {
        return Networks.getInstance().getServer().getPluginManager();
    }

    public static Networks getInstance() {
        return Networks.instance;
    }

    public static SupportedPluginManager getSupportedPluginManager() {
        return Networks.getInstance().supportedPluginManager;
    }

    public static ListenerManager getListenerManager() {
        return Networks.getInstance().listenerManager;
    }

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("########################################");
        getLogger().info("         Networks - By Sefiraat         ");
        getLogger().info("           Changed by mmmjjkx           ");
        getLogger().info("########################################");

        saveDefaultConfig();
        tryUpdate();

        this.supportedPluginManager = new SupportedPluginManager();

        setupSlimefun();

        this.listenerManager = new ListenerManager();
        this.getCommand("networks").setExecutor(new NetworksMain());

        // Fix dupe bug which break the network controller data without player interaction
        Bukkit.getScheduler().runTaskTimer(
                this,
                () -> {
                    Set<Location> wrongs = new HashSet<>();
                    Set<Location> controllers = new HashSet<>(
                            NetworkController.getNetworks().keySet());
                    for (Location controller : controllers) {
                        if (!(BlockStorage.check(controller) instanceof NetworkController)) {
                            wrongs.add(controller);
                        }
                    }

                    for (Location wrong : wrongs) {
                        NetworkUtils.clearNetwork(wrong);
                    }
                },
                5, Slimefun.getTickerTask().getTickRate()
        );

        // Asynchronous scanner that schedules synchronous world updates
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
            // Copy locations to avoid concurrent modification and to create stable snapshot
            Set<Location> snapshot = new HashSet<>(controllersSet);

            for (Location c : snapshot) {
                if (BlockStorage.check(c) instanceof NetworkController) {
                    // Clone location to capture its coordinates for later synchronous task
                    final Location loc = c.clone();

                    // Schedule a synchronous task to manipulate the world safely
                    Bukkit.getScheduler().runTask(this, () -> {
                        World world = loc.getWorld();
                        if (world == null) {
                            // Nothing to do if world unloaded / null
                            BlockStorage.clearBlockInfo(loc);
                            NetworkUtils.clearNetwork(loc);
                            return;
                        }

                        // Drop the network controller item at the center of the block
                        Location dropLoc = loc.clone().add(0.5, 0.5, 0.5);
                        world.dropItemNaturally(dropLoc, Converter.getItem(NetworksSlimefunItemStacks.NETWORK_CONTROLLER));

                        // Set the block to AIR using Bukkit API
                        world.getBlockAt(loc).setType(Material.AIR, false);

                        // Force an update so clients notice the change
                        world.getBlockAt(loc).getState().update(true);

                        // Clear stored block info and network data
                        BlockStorage.clearBlockInfo(loc);
                        NetworkUtils.clearNetwork(loc);
                    });
                }
            }

            // Clear the controllers set after scheduling tasks (clones were used)
            controllersSet.clear();
        }, 5, 10);

        setupMetrics();
    }

    public void tryUpdate() {
        if (getConfig().getBoolean("auto-update") && getPluginMeta().getVersion().startsWith("Dev")) {
            new BlobBuildUpdater(this, getFile(), "Networks", "Dev").start();
        }
    }

    public void setupSlimefun() {
        NetworkSlimefunItems.setup();
        if (supportedPluginManager.isNetheopoiesis()) {
            try {
                NetheoPlants.setup();
            } catch (NoClassDefFoundError e) {
                getLogger().severe("Netheopoiesis must be updated to meet Networks' requirements.");
            }
        }
        if (supportedPluginManager.isSlimeHud()) {
            try {
                HudCallbacks.setup();
            } catch (NoClassDefFoundError e) {
                getLogger().severe("SlimeHUD must be updated to meet Networks' requirements.");
            }
        }
    }

    public void setupMetrics() {
        final Metrics metrics = new Metrics(this, 13644);

        AdvancedPie networksChart = new AdvancedPie("networks", () -> {
            Map<String, Integer> networksMap = new HashMap<>();
            networksMap.put("Number of networks", NetworkController.getNetworks().size());
            return networksMap;
        });

        metrics.addCustomChart(networksChart);
    }

    @Nonnull
    @Override
    public JavaPlugin getJavaPlugin() {
        return this;
    }

    @Nullable
    @Override
    public String getBugTrackerURL() {
        return MessageFormat.format("https://github.com/{0}/{1}/issues/", this.username, this.repo);
    }
}
