package com.cavetale.territory.bb;

import com.cavetale.structure.cache.Cuboid;
import com.cavetale.territory.util.Vec2i;
import com.cavetale.territory.util.Vec3i;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.World;

/**
 * A named bounding box with additional data in world coordinates.
 * Read from the structures.txt file.
 */
@Data @RequiredArgsConstructor
public final class BoundingBox {
    public final String name;
    public final Vec3i min;
    public final Vec3i max;
    public final Vec2i chunk;
    private List<BoundingBox> children;
    private List<Position> positions;

    public int width() {
        return max.x - min.x + 1;
    }

    public int length() {
        return max.z - min.z + 1;
    }

    public boolean contains(int x, int y, int z) {
        return x >= min.x && y >= min.y && z >= min.z
            && x <= max.x && y <= max.y && z <= max.z;
    }

    public boolean overlaps(BoundingBox other) {
        return min.x <= other.max.x && max.x >= other.min.x
            && min.z <= other.max.z && max.z >= other.min.z
            && min.y <= other.max.y && max.y >= other.min.y;
    }

    public boolean overlapsArea(BoundingBox other) {
        return min.x <= other.max.x && max.x >= other.min.x
            && min.z <= other.max.z && max.z >= other.min.z;
    }

    public boolean overlapsAny(Collection<BoundingBox> others) {
        for (BoundingBox other : others) {
            if (overlaps(other)) return true;
        }
        return false;
    }

    public boolean overlapsAnyArea(Collection<BoundingBox> others) {
        for (BoundingBox other : others) {
            if (overlapsArea(other)) return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return name + ":" + min + "-" + max;
    }

    public void addChild(BoundingBox bb) {
        if (children == null) children = new ArrayList<>();
        children.add(bb);
    }

    public void addPosition(Position position) {
        if (positions == null) positions = new ArrayList<>();
        positions.add(position);
    }

    public Position getPositionAt(int x, int y, int z) {
        if (positions == null) return null;
        for (Position position : positions) {
            if (position.isAt(x, y, z)) return position;
        }
        return null;
    }

    /**
     * Check if ALL containing chunks are loaded.
     */
    public boolean isLoaded(World world) {
        final int ax = min.x >> 4;
        final int ay = min.z >> 4;
        final int bx = max.x >> 4;
        final int by = max.z >> 4;
        for (int y = ay; y <= by; y += 1) {
            for (int x = ax; x <= bx; x += 1) {
                if (!world.isChunkLoaded(x, y)) return false;
            }
        }
        return true;
    }

    public Cuboid toStructureCuboid() {
        return new Cuboid(min.x, min.y, min.z, max.x, max.y, max.z);
    }
}
