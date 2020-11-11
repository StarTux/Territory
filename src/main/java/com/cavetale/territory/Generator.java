package com.cavetale.territory;

import com.winthier.decorator.DecoratorPostWorldEvent;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@RequiredArgsConstructor
public final class Generator implements Listener {
    private final TerritoryPlugin plugin;
    private Map<String, ZoneWorld> worlds = new HashMap<>();

    public Generator enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        return this;
    }

    @EventHandler
    public void onDecoratorPostWorld(DecoratorPostWorldEvent event) {
        World world = event.getWorld();
        ZoneWorld zoneWorld = worlds.get(world.getName());
        if (zoneWorld == null) {
            event.setCancelled(true);
            zoneWorld = new ZoneWorld(world.getWorldFolder());
            worlds.put(world.getName(), zoneWorld);
            zoneWorld.loadBiomes();
            zoneWorld.loadStructures();
            zoneWorld.prepareFindZones();
            return;
        }
        if (!zoneWorld.allZonesDone) {
            event.setCancelled(true);
            long now = System.nanoTime();
            do {
                if (!zoneWorld.findZonesStep()) break;
            } while (System.nanoTime() - now < 50000000);
            return;
        }
        zoneWorld.mergeZones(100);
    }
}
