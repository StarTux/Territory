package com.cavetale.territory.struct;

import com.cavetale.core.struct.Vec2i;
import com.cavetale.territory.BiomeGroup;
import java.util.List;
import lombok.Data;

/**
 * Saved to JSON file.
 *
 * One contingent area within a world, comprised ideally of one
 * BiomeGroup, sometimes several.
 *
 * Each territory is stored in a file named after the chunk that's
 * considered its center. Example:
 * zone.-1.17.json
 */
@Data
public final class Territory {
    protected int id;
    protected int level;
    protected Vec2i center;
    protected String name;
    protected BiomeGroup biomeGroup;
    protected List<Integer> chunks;

    public Territory() { }

    public Territory(final int id, final int level, final Vec2i center,
                     final String name, final BiomeGroup biomeGroup, final List<Integer> chunks) {
        this.id = id;
        this.level = level;
        this.center = center;
        this.name = name;
        this.biomeGroup = biomeGroup;
        this.chunks = chunks;
    }

    public String getFileName() {
        String simpleName = name.toLowerCase()
            .replace(" ", "")
            .replace("/", "")
            .replace(".", "");
        return "territory." + id + "." + center.x + "." + center.z + "." + simpleName + ".json";
    }

    public int getChunkCount() {
        return chunks.size() / 2;
    }

    public Vec2i getChunk(int index) {
        return new Vec2i(chunks.get(index * 2),
                         chunks.get(index * 2 + 1));
    }

    public BiomeGroup getBiomeGroup() {
        return biomeGroup != null
            ? biomeGroup
            : BiomeGroup.VOID;
    }
}
