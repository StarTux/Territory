package com.cavetale.territory.generator.structure;

import com.cavetale.area.struct.Area;
import com.cavetale.area.struct.AreasFile;
import com.cavetale.core.struct.Cuboid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
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
    private final Map<GeneratorStructureType, List<GeneratorStructure>> cache = new EnumMap<>(GeneratorStructureType.class);

    public void load(World world) {
        for (GeneratorStructureType type : GeneratorStructureType.values()) {
            loadStructures(world, type);
        }
    }

    public void prepare() {
        for (GeneratorStructureType type : GeneratorStructureType.values()) {
            if (cache.get(type) == null || cache.get(type).isEmpty()) {
                throw new IllegalStateException(type + " cache is emtpy");
            }
            territoryPlugin().getLogger().info(cache.get(type).size() + " " + type + " structures loaded");
            for (GeneratorStructure structure : cache.get(type)) {
                Cuboid cuboid = structure.getBoundingBox().blockToChunk();
                for (int z = cuboid.az; z <= cuboid.bz; z += 1) {
                    for (int x = cuboid.ax; x <= cuboid.bx; x += 1) {
                        structure.getOriginWorld().getChunkAtAsync(x, z, (Consumer<Chunk>) chunk -> {
                                chunk.addPluginChunkTicket(territoryPlugin());
                            });
                    }
                }
            }
            Collections.shuffle(cache.get(type));
        }
    }

    public void unload(World world) {
        world.removePluginChunkTickets(territoryPlugin());
    }

    private void loadStructures(World world, GeneratorStructureType type) {
        AreasFile areasFile = AreasFile.load(world, type.areasFileName);
        if (areasFile == null) {
            territoryPlugin().getLogger().warning("[GeneratorStructureCache]"
                                                  + " [" + world.getName() + "]"
                                                  + " Nothing found: " + type);
            return;
        }
        for (Map.Entry<String, List<Area>> entry : areasFile.areas.entrySet()) {
            GeneratorStructure generatorStructure = type.createGeneratorStructure(world, entry.getKey(), entry.getValue());
            if (!generatorStructure.isValid()) {
                territoryPlugin().getLogger().severe("[GeneratorStructureCache] [" + type + "] not valid: " + generatorStructure);
                continue;
            } else {
                cache.computeIfAbsent(type, t -> new ArrayList<>()).add(generatorStructure);
            }
        }
    }

    public List<GeneratorStructure> getStructures(GeneratorStructureType type) {
        return cache.get(type);
    }

    public List<GeneratorStructure> getStructures(GeneratorStructureCategory category) {
        List<GeneratorStructure> result = new ArrayList<>();
        for (GeneratorStructureType type : GeneratorStructureType.values()) {
            if (category == type.category) {
                result.addAll(cache.get(type));
            }
        }
        return result;
    }

    public GeneratorStructure getStructure(GeneratorStructureType type, String name) {
        List<GeneratorStructure> list = getStructures(type);
        for (GeneratorStructure it : list) {
            if (name.equals(it.getName())) return it;
        }
        return null;
    }

    public List<String> allNames() {
        List<String> result = new ArrayList<>();
        for (GeneratorStructureType type : GeneratorStructureType.values()) {
            for (GeneratorStructure it : getStructures(type)) {
                result.add(it.getName());
            }
        }
        return result;
    }
}
