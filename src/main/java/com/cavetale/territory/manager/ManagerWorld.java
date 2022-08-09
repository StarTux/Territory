package com.cavetale.territory.manager;

import com.cavetale.structure.cache.Structure;
import com.cavetale.territory.TerritoryStructureType;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import static com.cavetale.structure.StructurePlugin.structureCache;

@Getter
public final class ManagerWorld {
    protected final String worldName;
    protected final TerritoryWorld territoryWorld;
    protected final Map<Integer, ManagerStructure> structureMap = new HashMap<>();

    public ManagerWorld(final String worldName) {
        this.worldName = worldName;
        this.territoryWorld = new TerritoryWorld(worldName);
    }

    protected void enable() {
        territoryWorld.load();
        for (Structure structure : structureCache().allLoaded(worldName)) {
            TerritoryStructureType type = TerritoryStructureType.of(structure.getKey());
            if (type == null) continue;
            onStructureLoad(type, structure);
        }
    }

    protected void onStructureLoad(TerritoryStructureType type, Structure structure) {
        ManagerStructure managerStructure = switch (type) {
        case MOB_CAMP -> new ManagerSurfaceStructure(type, structure);
        };
        structureMap.put(structure.getId(), managerStructure);
        managerStructure.enable();
    }

    protected void onStructureUnload(TerritoryStructureType type, Structure structure) {
        ManagerStructure managerStructure = structureMap.remove(structure.getId());
        if (managerStructure != null) managerStructure.disable();
    }
}
