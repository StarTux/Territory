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
}
