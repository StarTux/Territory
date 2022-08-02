package com.cavetale.territory.generator.structure;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;

/**
 * The ground on which a surface structure may be placed.
 */
public enum GroundType {
    ANY {
        @Override public boolean matches(Block block) {
            return true;
        }
    },
    GRASS(Tag.DIRT),
    SAND(Tag.SAND),
    TERRACOTTA(Tag.TERRACOTTA);

    private final Tag<Material> tag;

    GroundType() {
        this.tag = null;
    }

    GroundType(final Tag<Material> tag) {
        this.tag = tag;
    }

    public static GroundType of(Block anchorBlock) {
        for (GroundType it : values()) {
            if (it.tag != null && it.tag.isTagged(anchorBlock.getType())) {
                return it;
            }
        }
        return ANY;
    }

    public boolean matches(Block groundBlock) {
        return tag.isTagged(groundBlock.getType());
    }
}
