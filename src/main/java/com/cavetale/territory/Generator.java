package com.cavetale.territory;

import com.winthier.decorator.DecoratorPostWorldEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * This generator is used by the plugin.
 */
@RequiredArgsConstructor
public final class Generator implements Listener {
    static final long NANOS_PER_TICK = 1000000000L / 20L;
    private final TerritoryPlugin plugin;
    private Map<String, ZoneWorld> worlds = new HashMap<>();
    private Markov markov = new Markov(3);
    List<Vec2i> inChunkCoords;

    public Generator enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        markov.scan(plugin.getResource("names/forest.txt"));
        inChunkCoords = new ArrayList<>();
        for (int y = 0; y < 16; y += 1) {
            for (int x = 0; x < 16; x += 1) {
                inChunkCoords.add(new Vec2i(x, y));
            }
        }
        return this;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDecoratorPostWorld(DecoratorPostWorldEvent event) {
        World world = event.getWorld();
        ZoneWorld zoneWorld = worlds.get(world.getName());
        if (zoneWorld == null) {
            zoneWorld = new ZoneWorld(world.getWorldFolder());
            worlds.put(world.getName(), zoneWorld);
        }
        if (step(world, zoneWorld)) {
            event.setCancelled(true);
        }
    }

    boolean step(World world, ZoneWorld zoneWorld) {
        switch (zoneWorld.generatorState) {
        case 0:
            zoneWorld.loadBiomes();
            zoneWorld.loadStructures();
            zoneWorld.prepareFindZones();
            zoneWorld.generatorState += 1;
            return true;
        case 1:
            if (!timed(zoneWorld::findZonesStep)) {
                zoneWorld.generatorState += 1;
            }
            return true;
        case 2:
            if (!timed(zoneWorld::mergeRiversStep)) {
                zoneWorld.generatorState += 1;
            }
            return true;
        case 3:
            if (!timed(() -> zoneWorld.splitLargeZonesStep(1000))) { // magic number!
                zoneWorld.generatorState += 1;
            }
            return true;
        case 4:
            zoneWorld.findEssentialBiomes(100); // magic number!
            zoneWorld.generatorState += 1;
            return true;
        case 5:
            if (!timed(() -> zoneWorld.mergeZonesStep(500))) { // magic number!
                zoneWorld.generatorState += 1;
                return true;
            }
            return true;
        case 6:
            zoneWorld.scaleZoneLevels();
            zoneWorld.generatorState += 1;
            return true;
        case 7:
            if (!zoneWorld.adventurizeStep(markov, c -> generateStructure(world, zoneWorld, c))) {
                zoneWorld.generatorState += 1;
            }
            return true;
        default:
            zoneWorld.makeImage(0);
            zoneWorld.drawZones(true, true);
            zoneWorld.drawZones(false, false);
            zoneWorld.drawEssentialBiomes();
            zoneWorld.drawZoneLabels();
            try {
                zoneWorld.saveImage(new File(world.getWorldFolder(), "cavetale.map.png"));
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
            plugin.getLogger().info("Generator: " + world.getName() + " Done!");
            return false;
        }
    }

    boolean timed(Supplier<Boolean> fun) {
        long now = System.nanoTime();
        do {
            if (!fun.get()) return false;
        } while (System.nanoTime() - now < NANOS_PER_TICK);
        return true;
    }

    BoundingBox generateStructure(World world, ZoneWorld zoneWorld, Vec2i chunk) {
        int x = chunk.x << 4;
        int z = chunk.y << 4;
        int y = world.getHighestBlockYAt(x + 8, z + 8);
        BoundingBox result = new BoundingBox("bandit_camp",
                                             new Vec3i(x, y - 4, z),
                                             new Vec3i(x + 15, y + 4, z + 15));
        for (BoundingBox bb : zoneWorld.structures) {
            if (bb.overlaps(result)) {
                return null;
            }
        }
        // Prepare random coords
        Random random = new Random(chunk.hashCode());
        Collections.shuffle(inChunkCoords, random);
        Iterator<Vec2i> inChunkIter = inChunkCoords.iterator();
        // Fetch block
        Block block = world.getBlockAt(x + 8, y, z + 8); // Loading sync!
        if (block.isEmpty() || block.isLiquid() || !block.getType().isSolid()) return null;
        // Chest
        final Block chestBlock = block.getRelative(0, 1, 0);
        result.addPosition(new Position("chest", new Vec3i(chestBlock.getX(), chestBlock.getY(), chestBlock.getZ())));
        // Some mobs
        int mobCount = 0;
        while (mobCount < 8 && inChunkIter.hasNext()) {
            Vec2i vec = inChunkIter.next();
            block = world.getHighestBlockAt(x + vec.x, z + vec.y);
            if (block.isEmpty() || block.isLiquid() || !block.getType().isSolid()) continue;
            result.addPosition(new Position("enemy", new Vec3i(block.getX(), block.getY() + 1, block.getZ())));
            mobCount += 1;
        }
        // Finalize
        org.bukkit.block.data.type.Chest data = (org.bukkit.block.data.type.Chest) Material.CHEST.createBlockData();
        List<BlockFace> faces = new ArrayList<>(data.getFaces());
        data.setFacing(faces.get(random.nextInt(faces.size())));
        chestBlock.setBlockData(data);
        org.bukkit.block.Chest chest = (org.bukkit.block.Chest) chestBlock.getState();
        chest.setLock(secret(random));
        chest.update();
        return result;
    }

    String secret(Random random) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 16; i += 1) {
            sb.append((char) ((int) 'a' + random.nextInt(26)));
        }
        return sb.toString();
    }
}
