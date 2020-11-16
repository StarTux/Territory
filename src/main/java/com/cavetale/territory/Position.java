package com.cavetale.territory;

import lombok.Value;

@Value
public final class Position {
    public final String name;
    public final Vec3i vector;

    public boolean isAt(int x, int y, int z) {
        return x == vector.x && y == vector.y && z == vector.z;
    }

    @Override
    public String toString() {
        return name + "(" + vector + ")";
    }
}
