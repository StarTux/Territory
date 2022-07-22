package com.cavetale.territory.generator.structure;

import com.cavetale.area.struct.AreasFile;
import com.cavetale.structure.struct.Cuboid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.bukkit.Chunk;
import org.bukkit.World;
import static com.cavetale.territory.TerritoryPlugin.territoryPlugin;

/**
 * All structures will be provided via one world, marked via the Area
 * plugin.
 */
public final class GeneratorStructureCache {
    private final List<SurfaceStructure> surfaceStructures = new ArrayList<>();
    private int surfaceStructureIndex = 0;

    public void load(World world) {
        loadSurfaceStructures(world);
    }

    public void prepare() {
        if (surfaceStructures.isEmpty()) {
            throw new IllegalStateException("surfaceStructures is emtpy");
        }
        territoryPlugin().getLogger().info(surfaceStructures.size() + " surface structures loaded");
        for (GeneratorStructure structure : surfaceStructures) {
            Cuboid cuboid = structure.getBoundingBox().blockToChunk();
            for (int z = cuboid.az; z <= cuboid.bz; z += 1) {
                for (int x = cuboid.ax; x <= cuboid.bx; x += 1) {
                    structure.getOriginWorld()
                        .getChunkAtAsync(x, z, (Consumer<Chunk>) chunk -> chunk.addPluginChunkTicket(territoryPlugin()));
                }
            }
        }
        Collections.shuffle(surfaceStructures);
    }

    public void unload(World world) {
        world.removePluginChunkTickets(territoryPlugin());
    }

    public SurfaceStructure nextSurfaceStructure() {
        SurfaceStructure result = surfaceStructures.get(surfaceStructureIndex);
        surfaceStructureIndex += 1;
        if (surfaceStructureIndex >= surfaceStructureIndex) {
            surfaceStructureIndex = 0;
        }
        return result;
    }

    private void loadSurfaceStructures(World world) {
        final String name = "SurfaceStructures";
        AreasFile areasFile = AreasFile.load(world, name);
        if (areasFile == null) {
            territoryPlugin().getLogger().warning("[GeneratorStructureCache]"
                                                  + " [" + world.getName() + "]"
                                                  + " Nothing found: " + name);
            return;
        }
        for (Map.Entry<String, List<com.cavetale.area.struct.Cuboid>> entry : areasFile.areas.entrySet()) {
            SurfaceStructure surfaceStructure = new SurfaceStructure(world, entry.getKey(), entry.getValue());
            if (!surfaceStructure.isValid()) {
                territoryPlugin().getLogger().severe("[GeneratorStructureCache] not valid: " + surfaceStructure);
                continue;
            } else {
                surfaceStructures.add(surfaceStructure);
            }
        }
    }
}
