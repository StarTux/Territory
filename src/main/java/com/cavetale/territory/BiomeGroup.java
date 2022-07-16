package com.cavetale.territory;

import java.awt.Color;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public enum BiomeGroup {
    VOID(Color.BLACK),
    CAVE(Color.BLACK),
    RIVER(Color.BLUE),
    DESERT(Color.YELLOW),
    BIRCH(Color.LIGHT_GRAY),
    OCEAN(new Color(0f, 0f, .5f)),
    WARM_OCEAN(new Color(0f, 0f, 1f)),
    COLD_OCEAN(new Color(0f, 0f, .25f)),
    MOUNTAIN(Color.GRAY),
    TAIGA(Color.DARK_GRAY),
    BAMBOO(Color.ORANGE),
    JUNGLE(Color.ORANGE),
    SAVANNA(Color.RED),
    BADLANDS(Color.RED),
    SWAMP(new Color(.5f, .25f, 0f)),
    DARK_FOREST(new Color(0f, .5f, 0f)),
    FOREST(Color.GREEN),
    BEACH(Color.YELLOW),
    PLAINS(Color.PINK),
    MUSHROOM(new Color(1f, 0f, 1f)),
    SNOWY(Color.WHITE),
    FROZEN(new Color(0.5f, 0.5f, 1f));

    public static final Map<String, BiomeGroup> NAMES = new HashMap<>();
    public static final Map<String, BiomeGroup> KEYS = new HashMap<>();
    public final Set<String> names = new HashSet<>();
    public final Color color;
    public final boolean essential;
    public final String key;
    public final String humanName;

    BiomeGroup(final Color color, final boolean essential) {
        this.key = name().toLowerCase();
        this.color = color;
        this.essential = essential;
        this.humanName = Stream.of(name().split("_"))
            .map(s -> s.substring(0, 1) + s.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }

    BiomeGroup(final Color color) {
        this(color, false);
    }

    static {
        for (BiomeGroup biomeGroup : BiomeGroup.values()) {
            NAMES.put(biomeGroup.name(), biomeGroup);
            biomeGroup.names.add(biomeGroup.name());
            KEYS.put(biomeGroup.key, biomeGroup);
        }
    }

    public static BiomeGroup of(String name) {
        return NAMES.computeIfAbsent(name, BiomeGroup::forName);
    }

    public static BiomeGroup ofKey(String name) {
        return KEYS.get(name);
    }

    private static BiomeGroup forName(String biome) {
        BiomeGroup result = forName2(biome);
        if (result != null) result.names.add(biome);
        return result;
    }

    private static BiomeGroup forName2(String biome) {
        if (biome.contains("FROZEN") || biome.contains("ICE")) {
            return FROZEN;
        } else if (biome.contains("RIVER")) {
            return RIVER;
        } else if (biome.contains("MUSHROOM")) {
            return MUSHROOM;
        } else if (biome.contains("DESERT")) {
            return DESERT;
        } else if (biome.contains("BIRCH")) {
            return BIRCH;
        } else if (biome.contains("WARM_OCEAN")) {
            return WARM_OCEAN;
        } else if (biome.contains("COLD_OCEAN")) {
            return COLD_OCEAN;
        } else if (biome.contains("OCEAN")) {
            return OCEAN;
        } else if (biome.contains("MOUNTAIN") || biome.contains("PEAKS")) {
            return MOUNTAIN;
        } else if (biome.contains("SNOWY")) {
            return SNOWY;
        } else if (biome.contains("TAIGA")) {
            return TAIGA;
        } else if (biome.contains("BAMBOO")) {
            return BAMBOO;
        } else if (biome.contains("JUNGLE")) {
            return JUNGLE;
        } else if (biome.contains("BADLANDS")) {
            return BADLANDS;
        } else if (biome.contains("SAVANNA")) {
            return SAVANNA;
        } else if (biome.contains("SWAMP")) {
            return SWAMP;
        } else if (biome.contains("DARK_FOREST")) {
            return DARK_FOREST;
        } else if (biome.contains("FOREST") || biome.contains("WOOD")) {
            return FOREST;
        } else if (biome.contains("BEACH") || biome.contains("SHORE")) {
            return BEACH;
        } else if (biome.contains("PLAINS") || biome.contains("MEADOW")) {
            return PLAINS;
        } else if (biome.contains("CAVES")) {
            return CAVE;
        } else {
            return null;
        }
    }

    public static void debug(PrintStream out) {
        for (BiomeGroup biomeGroup : BiomeGroup.values()) {
            out.println(biomeGroup + " " + biomeGroup.names);
        }
    }
}
