package com.cavetale.territory.manager;

import com.cavetale.territory.BiomeGroup;
import com.cavetale.territory.Territory;
import com.cavetale.territory.TerritoryPlugin;
import com.cavetale.territory.bb.BoundingBox;
import com.destroystokyo.paper.Title;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
        Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
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
                    System.err.println("Parsing file " + file);
                    e.printStackTrace();
                    continue;
                }
                if (territory == null) {
                    System.err.println("File yields null: " + file);
                    continue;
                }
                tworld.addTerritory(territory);
            }
            try {
                tworld.structures.addAll(BoundingBox.fromStructuresFile(new File(world.getWorldFolder(), "structures.txt")));
            } catch (IOException ioe) {
                ioe.printStackTrace();
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

    void tick() {
        for (TerritoryWorld tworld : worlds.values()) {
            World world = Bukkit.getWorld(tworld.worldName);
            if (world == null) continue;
            tickWorld(world, tworld);
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            tickPlayer(player);
        }
    }

    public Session sessionOf(Player player) {
        return sessions.computeIfAbsent(player.getUniqueId(), u -> new Session(player));
    }

    void tickWorld(World world, TerritoryWorld tworld) {
        for (Territory territory : tworld.getTerritories()) {
            for (BoundingBox struct : territory.getCustomStructures()) {
                if (struct.isLoaded(world)) tickCustomStructure(world, tworld, territory, struct);
            }
        }
    }

    void tickCustomStructure(World world, TerritoryWorld tworld, Territory territory, BoundingBox structure) {
    }

    void tickPlayer(Player player) {
        Session session = sessionOf(player);
        TerritoryWorld tworld = getWorld(player.getWorld().getName());
        if (tworld == null) {
            session.resetTerritory();
            return;
        }
        Location location = player.getLocation();
        Territory oldTerritory = session.getTerritory();
        Territory territory = tworld.getTerritoryAtChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        if (!Objects.equals(oldTerritory, territory)) {
            session.setTerritory(territory);
            BiomeGroup biome = BiomeGroup.ofKey(territory.biome);
            if (biome == null) biome = BiomeGroup.VOID;
            ChatColor color = ChatColor.of(biome.color);
            String level = territory.level == 0
                ? "Beginner "
                : "Level " + territory.level + " ";
            Title title = Title.builder()
                .title(new ComponentBuilder().append(territory.name).color(color).create())
                .subtitle(new ComponentBuilder().append(level + biome.humanName).color(color).create())
                .fadeIn(10)
                .stay(20)
                .fadeOut(10)
                .build();
            player.sendTitle(title);
        }
    }

    public TerritoryWorld getWorld(String worldName) {
        return worlds.get(worldName);
    }
}
