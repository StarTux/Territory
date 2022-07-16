package com.cavetale.territory;

import org.junit.Test;
import org.bukkit.block.Biome;

public class TerritoryTest {
    @Test
    public void test() {
        for (Biome biome : Biome.values()) {
            if (BiomeGroup.of(biome.name()) == null) {
                System.err.println("No biome group: " + biome);
            }
        }
    }
}
