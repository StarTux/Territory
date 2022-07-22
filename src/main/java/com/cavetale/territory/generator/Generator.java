package com.cavetale.territory.generator;

import com.cavetale.core.util.Json;
import com.cavetale.structure.cache.Structure;
import com.cavetale.structure.struct.Cuboid;
import com.cavetale.territory.TerritoryPlugin;
import com.cavetale.territory.struct.SurfaceStructure;
import com.cavetale.territory.struct.Vec2i;
import com.cavetale.territory.struct.Vec3i;
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
import java.util.Random;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.HeightMap;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
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
    final List<Vec2i> relChunkCoords = new ArrayList<>(256); // [-8,7]

    public Generator enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (int y = 0; y < 16; y += 1) {
            for (int x = 0; x < 16; x += 1) {
                inChunkCoords.add(new Vec2i(x, y));
                relChunkCoords.add(new Vec2i(x - 8, y - 8));
            }
        }
        for (String worldName : plugin.getConfig().getStringList("Manager.Worlds")) {
            World world = Bukkit.getWorld(worldName);
            worlds.put(worldName, new GeneratorWorld(world.getWorldFolder(), plugin.getLogger()));
        }
        return this;
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

    @EventHandler(ignoreCancelled = true)
    public void onDecorator(DecoratorEvent event) {
        if (event.getPass() != 2) return;
        World world = event.getChunk().getWorld();
        GeneratorWorld generatorWorld = worlds.get(world.getName());
        if (generatorWorld == null) return;
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
            plugin.getLogger().info("Generator: " + world.getName() + " Done!");
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

    protected Structure generateStructure(World world, GeneratorWorld generatorWorld, Vec2i chunk) {
        // Prepare random coords
        Random random = new Random(chunk.hashCode());
        Collections.shuffle(inChunkCoords, random);
        Collections.shuffle(relChunkCoords, random);
        Iterator<Vec2i> inChunkIter = inChunkCoords.iterator();
        Iterator<Vec2i> relChunkIter = relChunkCoords.iterator();
        // Find center
        Vec2i baseVec = new Vec2i(chunk.x << 4, chunk.y << 4); // base of the chunk
        Cuboid boundingBox = null; // loop will set
        while (inChunkIter.hasNext()) {
            Vec2i inChunkVec = inChunkIter.next();
            Vec2i vec = baseVec.add(inChunkVec);
            Block block = world.getHighestBlockAt(vec.x, vec.y, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Vec3i vec3 = new Vec3i(block.getX(), block.getY(), block.getZ());
            // TODO find actual structure
            boundingBox = new Cuboid(block.getX() - 8,
                                     block.getY(),
                                     block.getZ() - 8,
                                     block.getX() + 7,
                                     block.getY() + 15,
                                     block.getZ() + 7);
            Cuboid checkBox = boundingBox.outset(64, 64, 64);
            if (!structureCache().within(world.getName(), checkBox).isEmpty()) return null;
        }
        if (boundingBox == null) return null; // inChunkIter ran out
        Structure structure = new Structure(world.getName(),
                                            NamespacedKey.fromString("territory:bandig_camp"),
                                            com.cavetale.structure.struct.Vec2i.of(chunk.x, chunk.y),
                                            boundingBox,
                                            Json.serialize(new SurfaceStructure()));
        structureCache().addStructure(structure);
        return structure;
    }

    private String secret(Random random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i += 1) {
            sb.append((char) ((int) 'a' + random.nextInt(26)));
        }
        return sb.toString();
    }
}
