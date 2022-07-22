package com.cavetale.territory.manager;

import com.cavetale.core.util.Json;
import com.cavetale.territory.BiomeGroup;
import com.cavetale.territory.struct.Territory;
import com.cavetale.territory.struct.Vec2i;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;

/**
 * World runtime for the Manager.
 */
@RequiredArgsConstructor
public final class TerritoryWorld {
    public static final String TERRITORY_FOLDER = "territories";
    public final String worldName;
    @Getter public final List<Territory> territories = new ArrayList<>();
    public final Map<Vec2i, int[]> regionMap = new HashMap<>();
    private final Territory nullTerritory = new Territory(0, 0, Vec2i.ZERO, "Nowhere", BiomeGroup.VOID.key, List.of());

    protected void load() {
        World world = Bukkit.getWorld(worldName);
        if (world == null) return;
        File folder = new File(world.getWorldFolder(), TERRITORY_FOLDER);
        for (File file : folder.listFiles()) {
            String name = file.getName();
            if (!name.startsWith("territory.") || !name.endsWith(".json")) continue;
            Territory territory = Json.load(file, Territory.class);
            if (territory == null) {
                throw new IllegalStateException("File yields null: " + file);
            }
            territories.add(territory);
        }
        territories.sort(Comparator.comparingInt(Territory::getId));
        for (int tindex = 0; tindex < territories.size(); tindex += 1) {
            Territory territory = territories.get(tindex);
            for (int i = 0; i < territory.getChunkCount(); i += 1) {
                final Vec2i chunkVec = territory.getChunk(i);
                final Vec2i regionVec = new Vec2i(chunkVec.x >> 5, chunkVec.y >> 5);
                regionMap.computeIfAbsent(regionVec, v -> new int[32 * 32])
                    [chunkVec.x + chunkVec.y * 32] = tindex;
            }
        }
    }

    public Territory at(Block block) {
        return getTerritoryAtChunk(block.getX() >> 4, block.getZ() >> 4);
    }

    public Territory at(Location location) {
        return getTerritoryAtChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
    }

    public Territory getTerritoryAtChunk(int x, int y) {
        int[] region = regionMap.get(new Vec2i(x, y));
        if (region == null) return nullTerritory;
        int tindex = region[x + y * 32];
        return tindex > 0
            ? territories.get(tindex)
            : nullTerritory;
    }
}
