package com.cavetale.territory.manager;

import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.util.Json;
import com.cavetale.territory.BiomeGroup;
import com.cavetale.territory.struct.Territory;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * World runtime for the Manager.  The generator also utilizes this in
 * its later stages.
 *
 * This loads the territories from their files and preprocesses them
 * for quick lookup.
 */
@RequiredArgsConstructor
public final class TerritoryWorld {
    public static final String TERRITORY_FOLDER = "territory";
    public final String worldName;
    @Getter private final List<Territory> territories = new ArrayList<>();
    private final Map<Vec2i, int[]> regionMap = new HashMap<>();
    private final Territory nullTerritory = new Territory(0, 0, Vec2i.ZERO, "Nowhere", BiomeGroup.VOID, List.of());

    public void load() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        File folder = new File(world.getWorldFolder(), TERRITORY_FOLDER);
        if (folder.isDirectory()) {
            for (File file : folder.listFiles()) {
                String name = file.getName();
                if (!name.startsWith("territory.") || !name.endsWith(".json")) continue;
                Territory territory = Json.load(file, Territory.class);
                if (territory == null) {
                    throw new IllegalStateException("File yields null: " + file);
                }
                territories.add(territory);
            }
        }
        territories.sort(Comparator.comparingInt(Territory::getId));
        for (int tindex = 0; tindex < territories.size(); tindex += 1) {
            Territory territory = territories.get(tindex);
            for (int i = 0; i < territory.getChunkCount(); i += 1) {
                final Vec2i chunkVec = territory.getChunk(i);
                final Vec2i regionVec = new Vec2i(chunkVec.x >> 5, chunkVec.z >> 5);
                int chunkX = chunkVec.x & 31;
                int chunkZ = chunkVec.z & 31;
                regionMap.computeIfAbsent(regionVec, v -> new int[32 * 32])
                    [chunkX + chunkZ * 32] = tindex + 1;
            }
        }
    }

    public Territory at(Block block) {
        return getTerritoryAtChunk(block.getX() >> 4, block.getZ() >> 4);
    }

    public Territory at(Location location) {
        return getTerritoryAtChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public Territory at(Chunk chunk) {
        return getTerritoryAtChunk(chunk.getX(), chunk.getZ());
    }

    public Territory getTerritoryAtChunk(int x, int y) {
        int[] region = regionMap.get(new Vec2i(x >> 5, y >> 5));
        if (region == null) return nullTerritory;
        int chunkX = x & 31;
        int chunkZ = y & 31;
        int tindex = region[chunkX + chunkZ * 32];
        return tindex > 0
            ? territories.get(tindex - 1)
            : nullTerritory;
    }
}
