package com.cavetale.territory.generator.structure;

import com.cavetale.core.struct.Vec3i;
import lombok.Value;

/**
 * Technially, the result of a canPlace() inquiry.
 */
@Value
public final class PlacementResult {
    public final Type type;
    public final int x;
    public final int y;
    public final int z;

    public enum Type {
        SUCCESS,
        AIR,
        GROUND,
        AUTO,
        ;

        public PlacementResult make(int ax, int ay, int az) {
            return new PlacementResult(this, ax, ay, az);
        }

        public PlacementResult make(Vec3i v) {
            return new PlacementResult(this, v.x, v.y, v.z);
        }
    }

    public boolean isSuccessful() {
        return type == Type.SUCCESS;
    }

    public Vec3i getVector() {
        return Vec3i.of(x, y, z);
    }
}
