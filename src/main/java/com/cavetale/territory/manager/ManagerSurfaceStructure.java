package com.cavetale.territory.manager;

import com.cavetale.structure.cache.Structure;
import com.cavetale.territory.TerritoryStructureType;
import com.cavetale.territory.struct.SurfaceStructureTag;
import lombok.Getter;

@Getter
public final class ManagerSurfaceStructure implements ManagerStructure {
    private final TerritoryStructureType type;
    private final Structure structure;
    private final SurfaceStructureTag tag;

    protected ManagerSurfaceStructure(final TerritoryStructureType type, final Structure structure) {
        this.type = type;
        this.structure = structure;
        this.tag = structure.getJsonData(SurfaceStructureTag.class, () -> null);
        if (tag == null) {
            throw new IllegalStateException("Tag is null: " + structure);
        }
    }

    @Override
    public void enable() {
    }

    @Override
    public void disable() {
    }
}
