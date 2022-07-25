package com.cavetale.territory.generator;

import com.cavetale.territory.BiomeGroup;
import com.cavetale.core.struct.Vec2i;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
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
public final class GeneratorChunk {
    public final Vec2i vec;
    public final BiomeGroup biome;
    public final Map<BiomeGroup, Integer> biomeGroups;

    public static List<GeneratorChunk> fromBiomesFile(File file, Logger logger) throws IOException {
        List<GeneratorChunk> result = new ArrayList<>();
        try (BufferedReader in = new BufferedReader(new FileReader(file))) {
            String line;
            while (null != (line = in.readLine())) {
                String[] toks = line.split(",");
                if (toks.length < 3) throw new IllegalStateException("toks.length=" + toks.length);
                int x = Integer.parseInt(toks[0]);
                int z = Integer.parseInt(toks[1]);
                Map<BiomeGroup, Integer> biomeGroups = new EnumMap<>(BiomeGroup.class);
                boolean isRiver = false;
                BiomeGroup maxBiome = BiomeGroup.VOID;
                int maxCount = 0;
                for (int i = 2; i < toks.length; i += 1) {
                    String[] fields = toks[i].split(":", 2);
                    if (fields.length != 2) throw new IllegalStateException("field=" + toks[i]);
                    String name = fields[0];
                    String number = fields[1];
                    int count;
                    try {
                        count = Integer.parseInt(number);
                    } catch (NumberFormatException nfe) {
                        throw new IllegalStateException("number=" + number);
                    }
                    BiomeGroup biomeGroup = BiomeGroup.of(name);
                    if (biomeGroup == null) {
                        logger.warning("Unknown biome group: " + name);
                        continue;
                    }
                    biomeGroups.put(biomeGroup, count);
                    if (biomeGroup == BiomeGroup.RIVER) isRiver = true;
                    if (count > maxCount) {
                        maxBiome = biomeGroup;
                        maxCount = count;
                    }
                }
                BiomeGroup mainBiome = isRiver ? BiomeGroup.RIVER : maxBiome;
                result.add(new GeneratorChunk(new Vec2i(x, z), mainBiome, biomeGroups));
            }
        }
        return result;
    }
}
