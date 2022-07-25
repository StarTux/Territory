package com.cavetale.territory.util;

import com.cavetale.core.struct.Vec2i;
import java.util.List;

/**
 * Utility class with vector operations.
 */
public final class Vectors {
    public static List<Vec2i> neighbors(Vec2i vec) {
        return List.of(vec.add(1, 0), vec.add(0, 1), vec.add(-1, 0), vec.add(0, -1));
    }

    public static Vec2i dimensions(Iterable<Vec2i> vecs) {
        Vec2i sample = vecs.iterator().next();
        int ax = sample.x;
        int bx = sample.x;
        int az = sample.z;
        int bz = sample.z;
        for (Vec2i vec : vecs) {
            if (vec.x < ax) ax = vec.x;
            if (vec.x > bx) bx = vec.x;
            if (vec.z < az) az = vec.z;
            if (vec.z > bz) bz = vec.z;
        }
        return new Vec2i(bx - ax + 1, bz - az + 1);
    }

    public static Vec2i minX(Iterable<Vec2i> vecs) {
        Vec2i result = null;
        for (Vec2i vec : vecs) {
            if (result == null || vec.x < result.x) result = vec;
        }
        return result;
    }

    public static Vec2i minZ(Iterable<Vec2i> vecs) {
        Vec2i result = null;
        for (Vec2i vec : vecs) {
            if (result == null || vec.z < result.z) result = vec;
        }
        return result;
    }

    public static int minAbsCoord(Vec2i vec) {
        return Math.min(Math.abs(vec.x), Math.abs(vec.z));
    }

    public static int distanceSum(Vec2i a, Vec2i b) {
        return Math.abs(a.x - b.x) + Math.abs(a.z - b.z);
    }

    public static Vec2i nearest(Vec2i pivot, Iterable<Vec2i> others) {
        Vec2i result = null;
        int dist = Integer.MAX_VALUE;
        for (Vec2i other : others) {
            if (pivot == other) continue;
            if (result == null || distanceSum(pivot, other) < dist) {
                result = other;
                dist = distanceSum(pivot, other);
            }
        }
        return result;
    }

    private Vectors() { }
}
