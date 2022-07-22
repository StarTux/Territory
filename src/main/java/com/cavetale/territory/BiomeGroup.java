package com.cavetale.territory;

import java.awt.Color;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import static com.cavetale.core.util.CamelCase.toCamelCase;

public enum BiomeGroup {
    CAVE(Color.BLACK, Category.INVALID),
    RIVER(Color.BLUE, Category.INVALID),
    DESERT(Color.YELLOW, Category.SURFACE),
    BIRCH(Color.LIGHT_GRAY, Category.SURFACE),
    OCEAN(new Color(0f, 0f, .5f), Category.SURFACE),
    WARM_OCEAN(new Color(0f, 0f, 1f), Category.AQUATIC),
    COLD_OCEAN(new Color(0f, 0f, .25f), Category.AQUATIC),
    MOUNTAIN(Color.GRAY, Category.SURFACE),
    TAIGA(Color.DARK_GRAY, Category.SURFACE),
    BAMBOO(Color.ORANGE, Category.SURFACE),
    JUNGLE(Color.ORANGE, Category.SURFACE),
    SAVANNA(Color.RED, Category.SURFACE),
    BADLANDS(Color.RED, Category.SURFACE),
    SWAMP(new Color(.5f, .25f, 0f), Category.SURFACE),
    DARK_FOREST(new Color(0f, .5f, 0f), Category.SURFACE),
    FOREST(Color.GREEN, Category.SURFACE),
    BEACH(Color.YELLOW, Category.SURFACE),
    PLAINS(Color.PINK, Category.SURFACE),
    MUSHROOM(new Color(1f, 0f, 1f), Category.SURFACE),
    SNOWY(Color.WHITE, Category.SURFACE),
    FROZEN(new Color(0.5f, 0.5f, 1f), Category.SURFACE),
    // Invalid
    VOID(Color.BLACK, Category.INVALID),
    END(Color.BLACK, Category.END),
    CUSTOM(Color.BLACK, Category.INVALID),
    NETHER(Color.RED, Category.NETHER);
    ;

    /**
     * The category tells the surface generator how to treat the
     * place.
     */
    @RequiredArgsConstructor
    public enum Category {
        SURFACE(true),
        AQUATIC(true),
        NETHER(false),
        END(false),
        /**
         * Invalid biomes should never or rarely occur in the world
         * because they are under ground or merged away during
         * PostWorld.
         */
        INVALID(false);

        /**
         * For simplicity's sake, the BiomeGroup's essential quality
         * will be dictated by the Category.
         */
        public final boolean essential;
    }

    public static final Map<String, BiomeGroup> NAMES = new HashMap<>();
    public static final Map<String, BiomeGroup> KEYS = new HashMap<>();
    public final Category category;
    public final Color color;
    public final boolean essential;
    public final String key;
    public final String humanName;
    public final Set<String> names = new HashSet<>();

    BiomeGroup(final Color color, final Category category) {
        this.category = category;
        this.key = name().toLowerCase();
        this.color = color;
        this.essential = category.essential;
        this.humanName = toCamelCase(" ", this);
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
        } else if (biome.contains("MOUNTAIN") || biome.contains("PEAKS") || biome.contains("HILLS")) {
            return MOUNTAIN;
        } else if (biome.contains("SNOWY") || biome.contains("GROVE")) {
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
        } else if (biome.contains("CAVES") || biome.contains("DEEP_DARK")) {
            return CAVE;
        } else if (biome.contains("NETHER") || biome.contains("SOUL_SAND_VALLEY") || biome.contains("BASALT_DELTAS")) {
            return NETHER;
        } else if (biome.contains("END")) {
            return END;
        } else if (biome.contains("VOID")) {
            return VOID;
        } else if (biome.contains("CUSTOM")) {
            return CUSTOM;
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
