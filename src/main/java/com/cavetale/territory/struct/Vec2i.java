package com.cavetale.territory.struct;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import lombok.Value;

@Value
public final class Vec2i {
    public static final Vec2i ZERO = new Vec2i(0, 0);
    public final int x;
    public final int y;

    public Vec2i add(int dx, int dy) {
        return new Vec2i(x + dx, y + dy);
    }

    public Vec2i add(Vec2i o) {
        return new Vec2i(x + o.x, y + o.y);
    }

    public List<Vec2i> getNeighbors() {
        return Arrays.asList(add(1, 0), add(0, 1), add(-1, 0), add(0, -1));
    }

    public int distanceSum(Vec2i other) {
        return Math.abs(other.x - x) + Math.abs(other.y - y);
    }

    public int maxDistance(Vec2i other) {
        return Math.max(Math.abs(other.x - x), Math.abs(other.y - y));
    }

    public int maxAbsCoord() {
        return Math.max(Math.abs(x), Math.abs(y));
    }

    public int minAbsCoord() {
        return Math.min(Math.abs(x), Math.abs(y));
    }

    public Vec2i nearest(Collection<Vec2i> others) {
        Vec2i result = null;
        int dist = Integer.MAX_VALUE;
        for (Vec2i other : others) {
            if (other == this) continue;
            if (result == null || distanceSum(other) < dist) {
                result = other;
                dist = distanceSum(other);
            }
        }
        return result;
    }

    public static Vec2i dimensionsOf(Collection<Vec2i> vecs) {
        Vec2i sample = vecs.iterator().next();
        int ax = sample.x;
        int bx = sample.x;
        int ay = sample.y;
        int by = sample.y;
        for (Vec2i vec : vecs) {
            if (vec.x < ax) ax = vec.x;
            if (vec.x > bx) bx = vec.x;
            if (vec.y < ay) ay = vec.y;
            if (vec.y > by) by = vec.y;
        }
        return new Vec2i(bx - ax + 1, by - ay + 1);
    }

    public static Vec2i maxX(Collection<Vec2i> vecs) {
        Vec2i result = null;
        for (Vec2i vec : vecs) {
            if (result == null || vec.x > result.x) result = vec;
        }
        return result;
    }

    public static Vec2i minX(Collection<Vec2i> vecs) {
        Vec2i result = null;
        for (Vec2i vec : vecs) {
            if (result == null || vec.x < result.x) result = vec;
        }
        return result;
    }

    public static Vec2i maxY(Collection<Vec2i> vecs) {
        Vec2i result = null;
        for (Vec2i vec : vecs) {
            if (result == null || vec.y > result.y) result = vec;
        }
        return result;
    }

    public static Vec2i minY(Collection<Vec2i> vecs) {
        Vec2i result = null;
        for (Vec2i vec : vecs) {
            if (result == null || vec.y < result.y) result = vec;
        }
        return result;
    }

    @Override
    public String toString() {
        return x + "," + y;
    }
}
