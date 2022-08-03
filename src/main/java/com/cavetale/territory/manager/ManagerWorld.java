package com.cavetale.territory.manager;

import com.cavetale.structure.cache.Structure;
import com.cavetale.territory.generator.structure.GeneratorStructureType;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

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
    }

    protected void onStructureLoad(GeneratorStructureType type, Structure structure) {
        ManagerStructure managerStructure = new ManagerStructure(type, structure);
        structureMap.put(structure.getId(), managerStructure);
        managerStructure.enable();
    }

    protected void onStructureUnload(GeneratorStructureType type, Structure structure) {
        ManagerStructure managerStructure = structureMap.remove(structure.getId());
        if (managerStructure != null) managerStructure.disable();
    }
}
