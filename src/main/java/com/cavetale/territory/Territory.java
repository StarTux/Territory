package com.cavetale.territory;

import com.cavetale.territory.bb.BoundingBox;
import com.cavetale.territory.util.Vec2i;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

/**
 * One contingent area within a world, comprised ideally of one
 * BiomeGroup, sometimes several.
 *
 * Each territory is stored in a file named after the chunk that's
 * considered its center. Example:
 * zone.-1.17.json
 */
@Data
public final class Territory {
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
