package com.cavetale.territory;

import lombok.RequiredArgsConstructor;

/**
 * En entry read from the biomes.txt file.
 */
@RequiredArgsConstructor
public final class ZoneChunk {
    public final Vec2i vec;
    public final BiomeGroup biome;
}
