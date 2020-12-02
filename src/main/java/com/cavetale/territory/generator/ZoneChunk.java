package com.cavetale.territory.generator;

import com.cavetale.territory.BiomeGroup;
import com.cavetale.territory.util.Vec2i;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;

/**
 * Entry read from the biomes.txt file.
 *
 * Line Format:
 * x,y,z,BIOME1,BIOME2,...
 * Example:
 * 1,-2,17,TAIGA,RIVER,FROZEN_TAIGA
 *
 * The biomes.txt contains vanilla biome groups whereas this class
 * will parse that info into one main BiomeGroup. River overrides all
 * other biome groups for the purpose of splitting the map into
 * territories along river boundaries.
 */
@RequiredArgsConstructor
public final class ZoneChunk {
    public final Vec2i vec;
    public final BiomeGroup biome;

    public static List<ZoneChunk> fromBiomesFile(File file) throws IOException {
        List<ZoneChunk> result = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while (null != (line = in.readLine())) {
                String[] toks = line.split(",");
                if (toks.length < 3) throw new IllegalStateException("toks.length=" + toks.length);
                int x = Integer.parseInt(toks[0]);
                int z = Integer.parseInt(toks[1]);
                BiomeGroup mainBiome = null;
                for (int i = 2; i < toks.length; i += 1) {
                    String name = toks[i];
                    BiomeGroup biomeGroup = BiomeGroup.of(name);
                    if (biomeGroup == null) {
                        System.err.println("Unknown biome group: " + name);
                    }
                    if (mainBiome == null || biomeGroup == BiomeGroup.RIVER) mainBiome = biomeGroup;
                }
                if (mainBiome == null) mainBiome = BiomeGroup.VOID;
                result.add(new ZoneChunk(new Vec2i(x, z), mainBiome));
            }
        }
        return result;
    }
}
