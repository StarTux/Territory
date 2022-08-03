package com.cavetale.territory.manager;

import com.cavetale.structure.cache.Structure;
import com.cavetale.structure.event.StructureLoadEvent;
import com.cavetale.structure.event.StructureUnloadEvent;
import com.cavetale.territory.TerritoryPlugin;
import com.cavetale.territory.generator.structure.GeneratorStructureType;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class Manager implements Listener {
    private final TerritoryPlugin plugin;
    private Map<String, ManagerWorld> worlds = new HashMap<>();
    private Map<UUID, Session> sessions = new HashMap<>();

    public Manager enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (String worldName : plugin.getConfig().getStringList("Manager.Worlds")) {
            ManagerWorld managerWorld = new ManagerWorld(worldName);
            worlds.put(worldName, managerWorld);
            managerWorld.enable();
            plugin.getLogger().info("[Manager] " + worldName + ": "
                                    + managerWorld.territoryWorld.getTerritories().size() + " territories");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            sessions.put(player.getUniqueId(), new Session(player));
        }
        return this;
    }

    public void disable() {
        sessions.clear();
    }

    @EventHandler
    private void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessions.put(player.getUniqueId(), new Session(player));
    }

    @EventHandler
    private void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    private void onStructureLoad(StructureLoadEvent event) {
        Structure structure = event.getStructure();
        GeneratorStructureType type = GeneratorStructureType.of(structure.getKey());
        if (type == null) return;
        ManagerWorld managerWorld = worlds.get(structure.getWorld());
        if (managerWorld == null) return;
        managerWorld.onStructureLoad(type, structure);
    }

    @EventHandler
    private void onStructureUnload(StructureUnloadEvent event) {
        Structure structure = event.getStructure();
        GeneratorStructureType type = GeneratorStructureType.of(structure.getKey());
        if (type == null) return;
        ManagerWorld managerWorld = worlds.get(structure.getWorld());
        if (managerWorld == null) return;
        managerWorld.onStructureUnload(type, structure);
    }

    public ManagerWorld getWorld(String worldName) {
        return worlds.get(worldName);
    }
}
