package com.cavetale.territory.manager;

import com.cavetale.territory.Territory;
import com.cavetale.territory.util.Vec2i;
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

    public void addTerritory(Territory territory) {
        territoryList.add(territory);
        for (Vec2i chunk : territory.chunks) {
            territoryMap.put(chunk, territory);
        }
    }

    public List<Territory> getTerritories() {
        return territoryList;
    }

    public Territory getTerritoryAtChunk(int x, int y) {
        return territoryMap.get(new Vec2i(x, y));
    }
}
