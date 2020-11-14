package com.cavetale.territory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Zone abstraction for the Generator.
 */
@RequiredArgsConstructor @Getter
public final class Zone {
    final BiomeGroup biome;
    final Set<Vec2i> chunks = new HashSet<>();
    Map<Vec2i, BoundingBox> customStructures;
    private Vec2i center;
    boolean essential = false; // deprecated?
    boolean structuresDone = false;
    int level;
    List<Zone> neighbors;
    String name;

    public Territory getTerritory() {
        Territory t = new Territory(name, biome.key, getCenter(), level);
        t.chunks.addAll(chunks);
        t.customStructures.addAll(customStructures.values());
        return t;
    }

    public boolean isBorder(Vec2i vec) {
        return chunks.contains(vec)
            && (!chunks.contains(vec.relative(0, 1))
                || !chunks.contains(vec.relative(1, 0))
                || !chunks.contains(vec.relative(0, -1))
                || !chunks.contains(vec.relative(-1, 0)));
    }

    public void putIn(Map<Vec2i, Zone> map) {
        for (Vec2i vec : chunks) {
            map.put(vec, this);
        }
    }

    public void removeFrom(Map<Vec2i, Zone> map) {
        for (Vec2i vec : chunks) {
            map.remove(vec);
        }
    }

    public int size() {
        return chunks.size();
    }

    public List<Vec2i> getBorderChunks() {
        return chunks.stream()
            .filter(this::isBorder)
            .collect(Collectors.toList());
    }

    public void addChunk(Vec2i vec) {
        center = null;
        chunks.add(vec);
    }

    public boolean removeChunk(Vec2i vec) {
        center = null;
        return chunks.remove(vec);
    }

    public void addAll(Zone other) {
        center = null;
        chunks.addAll(other.chunks);
    }

    public void addAllChunks(Collection<Vec2i> cs) {
        center = null;
        chunks.addAll(cs);
    }

    public boolean containsChunk(Vec2i vec) {
        return chunks.contains(vec);
    }

    public Vec2i computeCenter() {
        // Average of min and max
        Vec2i sample = chunks.iterator().next();
        int ax = sample.x;
        int bx = sample.x;
        int ay = sample.y;
        int by = sample.y;
        for (Vec2i chunk : chunks) {
            if (chunk.x < ax) ax = chunk.x;
            if (chunk.x > bx) bx = chunk.x;
            if (chunk.y < ay) ay = chunk.y;
            if (chunk.y > by) by = chunk.y;
        }
        // Nearest of Median
        center = new Vec2i((ax + bx) / 2, (ay + by) / 2).nearest(chunks);
        return center;
    }

    public Vec2i getCenter() {
        return center != null ? center : computeCenter();
    }

    public void computeNeighbors(Map<Vec2i, Zone> zones) {
        neighbors = new ArrayList<>();
        for (Vec2i vec : getBorderChunks()) {
            for (Vec2i nbor : vec.getNeighbors()) {
                Zone zone = zones.get(nbor);
                if (zone == null || zone == this || neighbors.contains(zone)) continue;
                neighbors.add(zone);
            }
        }
    }
}
