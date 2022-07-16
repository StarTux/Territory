package com.cavetale.territory.manager;

import com.cavetale.territory.Territory;
import com.cavetale.territory.TerritoryPlugin;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
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
    private TerritoryCommand territoryCommand;

    public Manager enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        territoryCommand = new TerritoryCommand(plugin).enable();
        Gson gson = new Gson();
        for (String worldName : plugin.getConfig().getStringList("Manager.Worlds")) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                plugin.getLogger().warning("World not found: " + worldName);
                continue;
            }
            TerritoryWorld tworld = new TerritoryWorld(worldName);
            worlds.put(worldName, tworld);
            File folder = new File(world.getWorldFolder(), "cavetale.zones");
            for (File file : folder.listFiles()) {
                String name = file.getName();
                if (!name.startsWith("zone.") || !name.endsWith(".json")) continue;
                Territory territory;
                try (FileReader fr = new FileReader(file)) {
                    territory = gson.fromJson(fr, Territory.class);
                } catch (Exception e) {
                    plugin.getLogger().severe("Parsing file " + file);
                    e.printStackTrace();
                    continue;
                }
                if (territory == null) {
                    plugin.getLogger().severe("File yields null: " + file);
                    continue;
                }
                tworld.addTerritory(territory);
            }
            plugin.getLogger().info(tworld.worldName + ": " + tworld.territoryList.size() + " territories");
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            enter(player);
        }
        return this;
    }

    public void disable() {
        sessions.clear();
    }

    void enter(Player player) {
        sessions.put(player.getUniqueId(), new Session(player));
    }

    void exit(Player player) {
        sessions.remove(player.getUniqueId());
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        enter(event.getPlayer());
    }

    @EventHandler
    void onPlayerQuit(PlayerQuitEvent event) {
        exit(event.getPlayer());
    }

    public Session sessionOf(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), u -> new Session(player));
    }

    public TerritoryWorld getWorld(String worldName) {
        return worlds.get(worldName);
    }
}
