package com.cavetale.territory;

import lombok.Value;

@Value
public final class Vec3i {
    public final int x;
    public final int y;
    public final int z;

    @Override
    public String toString() {
        return x + "," + y + "," + z;
    }

    public Vec3i add(int dx, int dy, int dz) {
        return new Vec3i(x + dx, y + dy, z + dz);
    }

    public Vec3i add(Vec3i o) {
        return new Vec3i(x + o.x, y + o.y, z + o.z);
    }
}
