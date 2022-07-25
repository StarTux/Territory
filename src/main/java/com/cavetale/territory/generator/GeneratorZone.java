package com.cavetale.territory.generator;

import com.cavetale.core.struct.Vec2i;
import com.cavetale.territory.BiomeGroup;
import com.cavetale.territory.struct.Territory;
import com.cavetale.territory.util.Vectors;
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
 * Zones are used to eventually generate Territories.
 */
@RequiredArgsConstructor @Getter
public final class GeneratorZone {
    protected final BiomeGroup biomeGroup;
    protected final Set<Vec2i> chunks = new HashSet<>();
    private Vec2i center;
    protected boolean essential = false; // deprecated?
    protected int level;
    protected List<GeneratorZone> neighbors;
    protected int id;

    public Territory createTerritory() {
        List<Integer> chunkList = new ArrayList<>();
        for (Vec2i chunk : chunks) {
            chunkList.add(chunk.x);
            chunkList.add(chunk.z);
        }
        return new Territory(id, level, getCenter(), biomeGroup.humanName, biomeGroup, chunkList);
    }

    public boolean isBorder(Vec2i vec) {
        return chunks.contains(vec)
            && (!chunks.contains(vec.add(0, 1))
                || !chunks.contains(vec.add(1, 0))
                || !chunks.contains(vec.add(0, -1))
                || !chunks.contains(vec.add(-1, 0)));
    }

    public void putIn(Map<Vec2i, GeneratorZone> map) {
        for (Vec2i vec : chunks) {
            map.put(vec, this);
        }
    }

    public void removeFrom(Map<Vec2i, GeneratorZone> map) {
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

    public void addAll(GeneratorZone other) {
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
        int ay = sample.z;
        int by = sample.z;
        for (Vec2i chunk : chunks) {
            if (chunk.x < ax) ax = chunk.x;
            if (chunk.x > bx) bx = chunk.x;
            if (chunk.z < ay) ay = chunk.z;
            if (chunk.z > by) by = chunk.z;
        }
        // Nearest of Median
        center = Vectors.nearest(new Vec2i((ax + bx) / 2, (ay + by) / 2), chunks);
        return center;
    }

    public Vec2i getCenter() {
        return center != null ? center : computeCenter();
    }

    public void computeNeighbors(Map<Vec2i, GeneratorZone> zones) {
        neighbors = new ArrayList<>();
        for (Vec2i vec : getBorderChunks()) {
            for (Vec2i nbor : Vectors.neighbors(vec)) {
                GeneratorZone zone = zones.get(nbor);
                if (zone == null || zone == this || neighbors.contains(zone)) continue;
                neighbors.add(zone);
            }
        }
    }
}
