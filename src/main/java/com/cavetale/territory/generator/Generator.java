package com.cavetale.territory.generator;

import com.cavetale.territory.TerritoryPlugin;
import com.cavetale.territory.bb.BoundingBox;
import com.cavetale.territory.bb.Position;
import com.cavetale.territory.util.Vec2i;
import com.cavetale.territory.util.Vec3i;
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
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import static com.cavetale.structure.StructurePlugin.structureCache;

/**
 * This generator is used by the plugin.
 */
@RequiredArgsConstructor
public final class Generator implements Listener {
    static final long NANOS_PER_TICK = 1000000000L / 20L;
    private final TerritoryPlugin plugin;
    private Map<String, ZoneWorld> worlds = new HashMap<>();
    final List<Vec2i> inChunkCoords = new ArrayList<>(256); // [0,15]
    final List<Vec2i> relChunkCoords = new ArrayList<>(256); // [-8,7]
    public static final EnumSet<Material> GROUND_FLOOR_MATS = EnumSet.noneOf(Material.class);
    public static final EnumSet<Material> REJECTED_MATS = EnumSet.noneOf(Material.class);

    static {
        for (Material mat : Material.values()) {
            if (isSuitableGroundMat(mat)) GROUND_FLOOR_MATS.add(mat);
        }
    }

    static boolean isSuitableGroundMat(Material mat) {
        switch (mat) {
        case GRASS_BLOCK:
        case DIRT:
        case COARSE_DIRT:
        case PODZOL:
        case SAND:
        case COBBLESTONE:
        case MOSSY_COBBLESTONE:
        case GRAVEL:
        case SANDSTONE:
        case STONE:
        case GRANITE:
        case ANDESITE:
        case DIORITE:
        case ICE:
        case BLUE_ICE:
        case SNOW_BLOCK:
            return true;
        default: break;
        }
        String name = mat.name();
        if (name.endsWith("_SLAB")) return false;
        if (name.endsWith("_STAIRS")) return false;
        if (name.endsWith("_TERRACOTTA")) return true;
        if (name.contains("_SANDSTONE")) return true;
        return false;
    }

    public Generator enable() {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (int y = 0; y < 16; y += 1) {
            for (int x = 0; x < 16; x += 1) {
                inChunkCoords.add(new Vec2i(x, y));
                relChunkCoords.add(new Vec2i(x - 8, y - 8));
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
            if (!zoneWorld.adventurizeStep(c -> generateStructure(world, zoneWorld, c))) {
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

    boolean isSuitableGroundFloor(Block block) {
        Material mat = block.getType();
        if (GROUND_FLOOR_MATS.contains(mat)) return true;
        if (!REJECTED_MATS.contains(mat)) {
            REJECTED_MATS.add(mat);
            plugin.getLogger().info("Rejected ground floor mat: " + mat);
        }
        return false;
    }

    BoundingBox generateStructure(World world, ZoneWorld zoneWorld, Vec2i chunk) {
        // Prepare random coords
        Random random = new Random(chunk.hashCode());
        Collections.shuffle(inChunkCoords, random);
        Collections.shuffle(relChunkCoords, random);
        Iterator<Vec2i> inChunkIter = inChunkCoords.iterator();
        Iterator<Vec2i> relChunkIter = relChunkCoords.iterator();
        // Find center
        Vec2i baseVec = new Vec2i(chunk.x << 4, chunk.y << 4); // base of the chunk
        Block centerBlock = null; // loop will set
        Vec2i centerVec = null; // loop will set
        BoundingBox result = null; // loop will set
        while (inChunkIter.hasNext()) {
            Vec2i inChunkVec = inChunkIter.next();
            Vec2i vec = baseVec.add(inChunkVec);
            Block block = world.getHighestBlockAt(vec.x, vec.y, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            Vec3i vec3 = new Vec3i(block.getX(), block.getY(), block.getZ());
            BoundingBox bb = new BoundingBox("bandit_camp", vec3.add(-8, -8, -8), vec3.add(7, 7, 7), chunk);
            if (!structureCache().within(world.getName(), bb.toStructureCuboid()).isEmpty()) return null;
            if (!isSuitableGroundFloor(block)) continue;
            // Success
            centerVec = vec;
            centerBlock = block;
            result = bb;
        }
        if (centerBlock == null) return null; // inChunkIter ran out
        // Chest
        final Block chestBlock = centerBlock.getRelative(0, 1, 0);
        result.addPosition(new Position("chest", new Vec3i(chestBlock.getX(), chestBlock.getY(), chestBlock.getZ())));
        // Some mobs
        int mobCount = 0;
        while (mobCount < 8 && relChunkIter.hasNext()) {
            Vec2i vec = centerVec.add(relChunkIter.next());
            Block block = world.getHighestBlockAt(vec.x, vec.y, HeightMap.MOTION_BLOCKING_NO_LEAVES);
            if (!isSuitableGroundFloor(block)) continue;
            // Success
            Position enemyPos = new Position("enemy", new Vec3i(block.getX(), block.getY() + 1, block.getZ()));
            result.addPosition(enemyPos);
            mobCount += 1;
        }
        if (mobCount == 0) return null;
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
