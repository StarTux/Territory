package com.cavetale.territory;

import com.cavetale.area.struct.Area;
import com.cavetale.territory.generator.structure.GeneratorStructure;
import com.cavetale.territory.generator.structure.GeneratorSurfaceStructure;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import static com.cavetale.territory.TerritoryStructureCategory.*;

/**
 * Enumerate all structure types.  Each enum has information about
 * loading the structure from the structures (schematic) world and how
 * create the corresponding data structure.
 */
@RequiredArgsConstructor
public enum TerritoryStructureType {
    MOB_CAMP(SURFACE, "MobCamp", "territory:mob_camp") {
        @Override public GeneratorStructure createGeneratorStructure(World world, String name, List<Area> areas) {
            return new GeneratorSurfaceStructure(this, world, name, areas);
        }
    },
    ;

    public final TerritoryStructureCategory category;
    public final String areasFileName;
    public final NamespacedKey key;

    TerritoryStructureType(final TerritoryStructureCategory category, final String areasFileName, final String key) {
        this.category = category;
        this.areasFileName = areasFileName;
        this.key = NamespacedKey.fromString(key);
    }

    public abstract GeneratorStructure createGeneratorStructure(World world, String name, List<Area> areas);

    public static TerritoryStructureType of(NamespacedKey key) {
        for (TerritoryStructureType it : values()) {
            if (key.equals(it.key)) return it;
        }
        return null;
    }
}
