package com.cavetale.territory.struct;

import com.cavetale.core.struct.Vec3i;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.Data;

/**
 * The JSON tag of a surface structure, stored in the Structure class
 * of the plugin of the same name.
 */
@Data
public final class SurfaceStructureTag {
    private String name;
    private Set<Vec3i> mobs = new HashSet<>();
    private Set<Vec3i> flyingMobs = new HashSet<>();
    private Set<UUID> spawnedMobs = new HashSet<>();
    private Vec3i bossChest = Vec3i.ZERO;

    public SurfaceStructureTag() { }

    public SurfaceStructureTag(final String name) {
        this.name = name;
    }
}
