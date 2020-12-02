package com.cavetale.territory.bb;

import com.cavetale.territory.util.Vec2i;
import com.cavetale.territory.util.Vec3i;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
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

    public boolean overlapsAny(Collection<BoundingBox> others) {
        for (BoundingBox other : others) {
            if (overlaps(other)) return true;
        }
        return false;
    }

    public static List<BoundingBox> fromStructuresFile(File file) throws IOException {
        Gson gson = new Gson();
        TypeToken<List<BoundingBox>> token = new TypeToken<List<BoundingBox>>() { };
        List<BoundingBox> result = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while (null != (line = in.readLine())) {
                String[] toks = line.split(",", 3);
                if (toks.length != 3) throw new IllegalStateException("toks.length=" + toks.length);
                int x = Integer.parseInt(toks[0]);
                int z = Integer.parseInt(toks[1]);
                List<BoundingBox> list;
                try {
                    list = gson.fromJson(toks[2], token.getType());
                } catch (Exception e) {
                    System.err.println(e.getMessage() + ": " + toks[2]);
                    e.printStackTrace();
                    continue;
                }
                result.addAll(list);
            }
        }
        return result;
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
}
