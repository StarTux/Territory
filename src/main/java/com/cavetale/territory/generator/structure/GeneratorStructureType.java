package com.cavetale.territory.generator.structure;

import com.cavetale.area.struct.Area;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import static com.cavetale.territory.generator.structure.GeneratorStructureCategory.*;

@RequiredArgsConstructor
public enum GeneratorStructureType {
    MOB_CAMP(SURFACE, "MobCamp", NamespacedKey.fromString("territory:mob_camp")) {
        @Override public GeneratorStructure createGeneratorStructure(World world, String name, List<Area> areas) {
            return new SurfaceStructure(this, world, name, areas);
        }
    },
    ;

    public final GeneratorStructureCategory category;
    public final String areasFileName;
    public final NamespacedKey key;

    public abstract GeneratorStructure createGeneratorStructure(World world, String name, List<Area> areas);

    public static GeneratorStructureType of(NamespacedKey key) {
        for (GeneratorStructureType it : values()) {
            if (key.equals(it.key)) return it;
        }
        return null;
    }
}
