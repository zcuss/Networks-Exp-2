package io.github.sefiraat.networks.listeners;

import javax.annotation.Nonnull;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import io.github.sefiraat.networks.utils.NetworkUtils;
import org.bukkit.event.world.StructureGrowEvent;

public class SyncListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBlockBreak(@Nonnull BlockBreakEvent event) {
        NetworkUtils.clearNetwork(event.getBlock().getLocation());
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockPlace(@Nonnull BlockPlaceEvent event) {
        NetworkUtils.clearNetwork(event.getBlock().getLocation());
    }

    //Fixed a dupe
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onStructureGrow(@Nonnull StructureGrowEvent e) {
        for (BlockState state : e.getBlocks()) {
            Block block = state.getBlock();
            Location loc = block.getLocation();
            NetworkUtils.clearNetwork(loc);
        }
    }
}