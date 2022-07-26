package com.cavetale.territory.manager;

import com.cavetale.territory.TerritoryPlugin;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

@RequiredArgsConstructor
public final class Manager implements Listener {
    private final TerritoryPlugin plugin;
    private Map<String, TerritoryWorld> worlds = new HashMap<>();
    private Map<UUID, Session> sessions = new HashMap<>();

    public Manager enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (String worldName : plugin.getConfig().getStringList("Manager.Worlds")) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World not found: " + worldName);
                continue;
            }
            TerritoryWorld territoryWorld = new TerritoryWorld(worldName);
            worlds.put(worldName, territoryWorld);
            territoryWorld.load();
            plugin.getLogger().info("[Manager] " + territoryWorld.worldName + ": "
                                    + territoryWorld.getTerritories().size() + " territories");
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
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        sessions.put(player.getUniqueId(), new Session(player));
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        sessions.remove(event.getPlayer().getUniqueId());
    }

    public Session sessionOf(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), u -> new Session(player));
    }

    public TerritoryWorld getWorld(String worldName) {
        return worlds.get(worldName);
    }
}
