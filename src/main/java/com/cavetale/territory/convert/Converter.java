package com.cavetale.territory.convert;

public final class Converter {
    public static com.cavetale.structure.struct.Cuboid areaToStructure(com.cavetale.area.struct.Cuboid in) {
        return new com.cavetale.structure.struct.Cuboid(in.min.x, in.min.y, in.min.z,
                                                        in.max.x, in.max.y, in.max.z);
    }

    public static com.cavetale.structure.struct.Vec3i areaToStructure(com.cavetale.area.struct.Vec3i in) {
        return new com.cavetale.structure.struct.Vec3i(in.x, in.y, in.z);
    }

    private Converter() { }
}
