package com.cavetale.territory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

/**
 * World runtime for the Manager.
 */
@RequiredArgsConstructor
public final class TerritoryWorld {
    public final String worldName;
    public final List<Territory> territoryList = new ArrayList<>();
    public final Map<Vec2i, Territory> territoryMap = new HashMap<>();
    public final List<BoundingBox> structures = new ArrayList<>();

    public void addTerritory(Territory territory) {
        territoryList.add(territory);
        for (Vec2i chunk : territory.chunks) {
            territoryMap.put(chunk, territory);
        }
    }

    public Territory getTerritoryAtChunk(int x, int y) {
        return territoryMap.get(new Vec2i(x, y));
    }

    public BoundingBox getStructureAt(int x, int y, int z) {
        for (BoundingBox bb : structures) {
            if (bb.contains(x, y, z)) return bb;
        }
        return null;
    }
}
