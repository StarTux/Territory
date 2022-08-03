package com.cavetale.territory.generator;

import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.structure.cache.Structure;
import com.cavetale.territory.BiomeGroup;
import com.cavetale.territory.TerritoryPlugin;
import com.cavetale.territory.generator.structure.GeneratorStructure;
import com.cavetale.territory.generator.structure.GeneratorStructureCache;
import com.cavetale.territory.generator.structure.GeneratorStructureCategory;
import com.cavetale.territory.struct.Territory;
import com.winthier.decorator.DecoratorEvent;
import com.winthier.decorator.DecoratorPostWorldEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import static com.cavetale.structure.StructurePlugin.structureCache;

/**
 * This generator is used by the plugin.
 */
@RequiredArgsConstructor
public final class Generator implements Listener {
    static final long NANOS_PER_TICK = 1000_000_000L / 20L;
    private final TerritoryPlugin plugin;
    private Map<String, GeneratorWorld> worlds = new HashMap<>();
    final List<Vec2i> inChunkCoords = new ArrayList<>(256); // [0,15]
    private GeneratorStructureCache generatorStructureCache;
    private final List<String> structureWorlds = List.of("structures");

    public Generator enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (int y = 0; y < 16; y += 1) {
            for (int x = 0; x < 16; x += 1) {
                inChunkCoords.add(new Vec2i(x, y));
            }
        }
        plugin.getLogger().info("Loading Worlds");
        for (String worldName : plugin.getConfig().getStringList("Generator.Worlds")) {
            World world = Bukkit.getWorld(worldName);
            if (world == null) {
                throw new IllegalStateException("Generator world not found: " + worldName);
            }
            worlds.put(worldName, new GeneratorWorld(worldName, world.getWorldFolder(), plugin.getLogger()));
        }
        // Load structure worlds
        return this;
    }

    private GeneratorStructureCache getStructureCache() {
        if (generatorStructureCache == null) {
            plugin.getLogger().info("Loading Structure Cache");
            generatorStructureCache = new GeneratorStructureCache();
            for (String worldName : structureWorlds) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    throw new IllegalStateException("Structure world not found: " + worldName);
                }
                generatorStructureCache.load(world);
            }
            generatorStructureCache.prepare();
        }
        return generatorStructureCache;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDecoratorPostWorld(DecoratorPostWorldEvent event) {
        if (event.getPass() != 1) return;
        World world = event.getWorld();
        GeneratorWorld generatorWorld = worlds.get(world.getName());
        if (generatorWorld == null) return;
        if (step(world, generatorWorld)) {
            event.setCancelled(true);
        }
    }

    private boolean step(World world, GeneratorWorld generatorWorld) {
        plugin.getLogger().info("Generator: Step " + generatorWorld.generatorState);
        switch (generatorWorld.generatorState) {
        case 0:
            generatorWorld.loadBiomes();
            generatorWorld.prepareFindZones();
            generatorWorld.generatorState += 1;
            return true;
        case 1:
            if (!timed(generatorWorld::findZonesStep)) {
                generatorWorld.generatorState += 1;
            }
            return true;
        case 2:
            if (!timed(generatorWorld::mergeRiversStep)) {
                generatorWorld.generatorState += 1;
            }
            return true;
        case 3:
            if (!timed(() -> generatorWorld.splitLargeZonesStep(1000))) { // magic number!
                generatorWorld.generatorState += 1;
            }
            return true;
        case 4:
            generatorWorld.findEssentialBiomes(100); // magic number!
            generatorWorld.generatorState += 1;
            return true;
        case 5:
            if (!timed(() -> generatorWorld.mergeZonesStep(500))) { // magic number!
                generatorWorld.generatorState += 1;
                return true;
            }
            return true;
        case 6:
            generatorWorld.scaleZoneLevels();
            generatorWorld.generatorState += 1;
            return true;
        case 7:
            generatorWorld.saveZones();
            generatorWorld.generatorState += 1;
            return true;
        default:
            generatorWorld.makeImage(0);
            generatorWorld.drawZones(true, true);
            generatorWorld.drawZones(false, false);
            generatorWorld.drawEssentialBiomes();
            generatorWorld.drawZoneLabels();
            try {
                generatorWorld.saveImage(new File("map.png"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            plugin.getLogger().info("[Generator] [" + world.getName() + "] Done!");
            return false;
        }
    }

    private boolean timed(Supplier<Boolean> fun) {
        long now = System.nanoTime();
        do {
            if (!fun.get()) return false;
        } while (System.nanoTime() - now < NANOS_PER_TICK);
        return true;
    }

    private  static final EnumSet<Material> REJECTED_MATS = EnumSet.noneOf(Material.class);

    @EventHandler(ignoreCancelled = true)
    public void onDecorator(DecoratorEvent event) {
        if (event.getPass() != 2) return;
        Chunk chunk = event.getChunk();
        World world = chunk.getWorld();
        GeneratorWorld generatorWorld = worlds.get(world.getName());
        if (generatorWorld == null) return;
        Territory territory = generatorWorld.getTerritoryWorld().at(chunk);
        BiomeGroup biomeGroup = territory.getBiomeGroup();
        switch (biomeGroup.category) {
        case SURFACE: generateSurfaceStructure(chunk, generatorWorld); break;
        default: break;
        }
    }

    /**
     * Surface structures generate on or above Y=63 and will not
     * tolerate nearby structures on or above 48 within 64 blocks of
     * the chunk.
     * @return true if structure was generated, false otherwise
     */
    protected boolean generateSurfaceStructure(Chunk chunk, GeneratorWorld generatorWorld) {
        final Vec2i baseVec = new Vec2i(chunk.getX() << 4, chunk.getZ() << 4);
        World world = chunk.getWorld();
        Cuboid chunkZone = new Cuboid(baseVec.x, 48, baseVec.z,
                                      baseVec.x + 15, world.getMaxHeight(), baseVec.z + 15);
        Cuboid exclusionZone = chunkZone.outset(128, 0, 128);
        var chunkVector = Vec2i.of(chunk);
        if (!structureCache().within(world.getName(), exclusionZone).isEmpty()) {
            return false;
        }
        // Prepare random coords
        Collections.shuffle(inChunkCoords, generatorWorld.random);
        final Iterator<Vec2i> inChunkIter = inChunkCoords.iterator();
        // Find center
        while (inChunkIter.hasNext()) {
            final Vec2i worldXZ = baseVec.add(inChunkIter.next());
            Block anchor = world.getHighestBlockAt(worldXZ.x, worldXZ.z, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (anchor.getY() < 63) {
                continue;
            }
            List<GeneratorStructure> surfaceStructureList = new ArrayList<>(getStructureCache().getStructures(GeneratorStructureCategory.SURFACE));
            surfaceStructureList.removeIf(s -> !s.canPlace(anchor));
            if (surfaceStructureList.isEmpty()) continue;
            Collections.shuffle(surfaceStructureList, generatorWorld.random);
            for (GeneratorStructure surfaceStructure : surfaceStructureList) {
                Cuboid boundingBox = surfaceStructure.createTargetBoundingBox(anchor);
                if (!surfaceStructure.canPlace(world, boundingBox).isSuccessful()) {
                    continue;
                }
                Structure structure = surfaceStructure.place(world, boundingBox, chunkVector);
                if (structure == null) {
                    continue;
                }
                plugin.getLogger().info(chunkVector + ": Placed structure: " + structure);
                structureCache().addStructure(structure);
                return true;
            }
        }
        return false;
    }
}
