package com.cavetale.territory;

/**
 * A named bounding box read from the structures.txt file.
 */
public final class BoundingBox {
    String name;
    Vec3i min;
    Vec3i max;

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
}
