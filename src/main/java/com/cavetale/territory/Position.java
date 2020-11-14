package com.cavetale.territory;

import lombok.Value;

@Value
public class Position {
    public final String name;
    public final Vec3i vector;
}
