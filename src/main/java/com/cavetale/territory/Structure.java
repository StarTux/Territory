package com.cavetale.territory;

import java.util.HashMap;
import java.util.Map;

public enum Structure {
    MINESHAFT(0xffFFFFFF),
    VILLAGE(0xff88FF88),
    STRONGHOLD(0xffFF4444),
    RUINED_PORTAL(0xffFF00FF),
    OCEAN_RUIN(0xffFF88FF),
    MONUMENT(0xff8888FF),
    SHIPWRECK(0xff008800),
    DESERT_PYRAMID(0xff008800),
    JUNGLE_PYRAMID(0xff008800),
    BURIED_TREASURE(0xff888800),
    SWAMP_HUT(0xff884400),
    IGLOO(0xffBBBBBB),
    PILLAGER_OUTPOST(0xffFF0000),
    WOODLAND_MANSION(0xffFFFFFF),
    NETHER_FORTRESS,
    END_CITY,
    NETHER_FOSSIL,
    BASTION_REMNANT;

    public final String key;
    public final int color;
    private static final Map<String, Structure> KEYS = new HashMap<>();

    Structure(final int color) {
        this.key = name().toLowerCase();
        this.color = color;
    }

    Structure() {
        this(0);
    }

    static {
        for (Structure structure : Structure.values()) {
            KEYS.put(structure.key, structure);
        }
    }

    public static Structure forKey(String key) {
        return KEYS.get(key);
    }
}
