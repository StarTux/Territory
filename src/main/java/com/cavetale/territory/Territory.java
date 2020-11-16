package com.cavetale.territory;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public final class Territory {
    transient TerritoryWorld tworld;
    public final String name;
    public final String biome;
    public final Vec2i center;
    public final int level;
    public final List<Vec2i> chunks = new ArrayList<>();
    public final List<BoundingBox> customStructures = new ArrayList<>();

    public String getFileName() {
        return "zone." + center.x + "." + center.y + ".json";
    }

    public BoundingBox customStructureAt(int x, int y, int z) {
        for (BoundingBox bb : customStructures) {
            if (bb.contains(x, y, z)) return bb;
        }
        return null;
    }
}
