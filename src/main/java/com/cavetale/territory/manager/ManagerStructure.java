package com.cavetale.territory.manager;

import com.cavetale.structure.cache.Structure;
import com.cavetale.territory.TerritoryStructureType;

public interface ManagerStructure {
    TerritoryStructureType getType();

    Structure getStructure();

    void enable();

    void disable();
}
