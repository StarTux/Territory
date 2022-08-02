package com.cavetale.territory.generator.structure;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.structure.cache.Structure;
import java.util.List;
import org.bukkit.World;
import org.bukkit.block.Block;

public interface GeneratorStructure {
    World getOriginWorld();

    boolean isValid();

    String getName();

    Vec3i getAnchor();

    Cuboid getBoundingBox();

    List<Cuboid> getMarkers(String markerName);

    /**
     * Create the world bounding box required to place this structure
     * in a world.
     * @param the anchor which will correspond with this anchor
     * @return the bounding box
     */
    default Cuboid createTargetBoundingBox(Block base) {
        Cuboid boundingBox = getBoundingBox();
        Vec3i anchor = getAnchor();
        Vec3i min = boundingBox.getMin();
        return boundingBox.shift((base.getX() - min.x) - (anchor.x - min.x),
                                 (base.getY() - min.y) - (anchor.y - min.y),
                                 (base.getZ() - min.z) - (anchor.z - min.z));
    }

    boolean canPlace(Block anchorBlock);

    PlacementResult canPlace(World targetWorld, Cuboid targetBoundingBox);

    Structure place(World targetWorld, Cuboid worldBoundingBox, Vec2i chunkVector);
}
