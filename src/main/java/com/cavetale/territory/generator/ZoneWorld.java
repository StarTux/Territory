package com.cavetale.territory.generator;

import com.cavetale.territory.BiomeGroup;
import com.cavetale.territory.StructureType;
import com.cavetale.territory.Territory;
import com.cavetale.territory.bb.BoundingBox;
import com.cavetale.territory.util.Markov;
import com.cavetale.territory.util.Vec2i;
import com.google.gson.Gson;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import javax.imageio.ImageIO;
import lombok.Getter;

/**
 * Represents a Minecraft world in a folder. With the following:
 * - A biomes.txt file
 * - A structures.txt file
 * An instance provides methods to parse these files and turn them
 * into an image file, produce cohesive territories (zones) for
 * in-game enhancements.
 *
 * This is to be used by the world generator to setup the data
 * structures and save them in files. It's not intended for the
 * gameplay server.
 *
 * It can also be used by a standalone app. Not Paper required.
 */
@Getter
public final class ZoneWorld {
    private final File folder;
    int ax = 0;
    int bx = 0;
    int az = 0;
    int bz = 0;
    int width;
    int height;
    List<ZoneChunk> chunks;
    List<BoundingBox> structures;
    List<Zone> zones;
    Map<Vec2i, ZoneChunk> findZonesPool;
    Map<Vec2i, Zone> zoneMap;
    Map<BiomeGroup, Vec2i> essentialBiomes;
    BufferedImage img;
    Graphics gfx;
    int generatorState;
    int maxLevel;
    List<Vec2i> todoChunks;
    Random random;

    public ZoneWorld(final File folder) {
        this.folder = folder;
    }

    public void loadBiomes() {
        try {
            chunks = ZoneChunk.fromBiomesFile(new File(folder, "biomes.txt"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public void loadStructures() {
        try {
            structures = BoundingBox.fromStructuresFile(new File(folder, "structures.txt"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        for (ZoneChunk chunk : chunks) {
            if (chunk.vec.x < ax) ax = chunk.vec.x;
            if (chunk.vec.x > bx) bx = chunk.vec.x;
            if (chunk.vec.y < az) az = chunk.vec.y;
            if (chunk.vec.y > bz) bz = chunk.vec.y;
            width = bx - ax + 1;
            height = bz - az + 1;
        }
    }

    public void makeImage(int backgroundColor) {
        img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y += 1) {
            for (int x = 0; x < width; x += 1) {
                img.setRGB(x, y, backgroundColor);
            }
        }
        gfx = img.getGraphics();
    }

    public void drawBiomes()  {
        Set<String> unhandled = new HashSet<>();
        for (ZoneChunk chunk : chunks) {
            int x = chunk.vec.x - ax;
            int y = chunk.vec.y - az;
            img.setRGB(x, y, chunk.biome.color.getRGB());
        }
        if (!unhandled.isEmpty()) {
            System.err.println("Unhandled biomes: " + unhandled);
        }
    }

    public void drawStructures() {
        Set<String> unhandled = new HashSet<>();
        Collections.sort(structures, (a, b) -> Integer.compare(a.min.y, b.min.y));
        for (BoundingBox bb : structures) {
            StructureType structureType = StructureType.forKey(bb.name);
            if (structureType == null) {
                unhandled.add(bb.name);
                continue;
            }
            if (structureType == StructureType.MINESHAFT) continue;
            if (structureType.color == 0) continue;
            if (bb.width() < 16 || bb.length() < 16) continue;
            int x1 = bb.min.x >> 4;
            int x2 = bb.max.x >> 4;
            int y1 = bb.min.z >> 4;
            int y2 = bb.max.z >> 4;
            rect(new Color(structureType.color), x1, y1, x2 - x1 + 1, y2 - y1 + 1);
            print(new Color(structureType.color), bb.name.substring(0, 3), x1, y1);
        }
        if (!unhandled.isEmpty()) {
            System.err.println("Unhandled structure types: " + unhandled);
        }
    }

    public void drawCustomStructures() {
        for (Zone zone : zones) {
            if (zone.customStructures == null) continue;
            for (BoundingBox bb : zone.customStructures.values()) {
                int x1 = bb.min.x >> 4;
                int x2 = bb.max.x >> 4;
                int y1 = bb.min.z >> 4;
                int y2 = bb.max.z >> 4;
                rect(zone.biome.color.darker(), x1, y1, x2 - x1 + 1, y2 - y1 + 1);
            }
        }
    }

    public void drawZones(boolean fill, boolean dynamicColor) {
        for (Zone zone : zones) {
            Color color;
            if (dynamicColor) {
                Vec2i center = zone.getCenter();
                float east = (float) (center.x - ax) / (float) width;
                float[] hsb = Color.RGBtoHSB(zone.biome.color.getRed(),
                                             zone.biome.color.getGreen(),
                                             zone.biome.color.getBlue(),
                                             new float[3]);
                float lvl = (float) zone.level / (float) maxLevel;
                color = Color.getHSBColor(hsb[0], hsb[1], 1f - lvl);
            } else {
                color = zone.biome.color;
            }
            if (fill) {
                for (Vec2i vec : zone.chunks) {
                    pixel(color, vec.x, vec.y);
                }
            } else {
                for (Vec2i vec : zone.getBorderChunks()) {
                    pixel(color, vec.x, vec.y);
                }
            }
        }
    }

    public void drawZoneLabels() {
        for (Zone zone : zones) {
            Vec2i center = zone.getCenter();
            print(Color.WHITE, "" + zone.level, center.x, center.y);
        }
    }

    public void drawEssentialBiomes() {
        for (Map.Entry<BiomeGroup, Vec2i> entry : essentialBiomes.entrySet()) {
            BiomeGroup biome = entry.getKey();
            Vec2i pos = entry.getValue();
            print(biome.color, biome.name().toLowerCase().replace("_", " "), pos.x, pos.y);
            for (int i = 0; i < 5; i += 1) {
                pixel(biome.color, pos.x + i, pos.y + i);
                pixel(biome.color, pos.x + i, pos.y - i);
                pixel(biome.color, pos.x - i, pos.y + i);
                pixel(biome.color, pos.x - i, pos.y - i);
            }
        }
    }

    public void drawSmallZones(int maxSize) {
        for (Zone zone : zones) {
            if (zone.size() > maxSize) continue;
            int left = Integer.MAX_VALUE;
            int top = Integer.MAX_VALUE;
            for (Vec2i vec : zone.chunks) {
                if (left > vec.x) left = vec.x;
                if (top > vec.y) top = vec.y;
                img.setRGB(vec.x - ax, vec.y - az, zone.biome.color.getRGB());
            }
            print(zone.biome.color, zone.biome.name(), left, top);
        }
    }

    public void saveImage(File file) throws IOException {
        ImageIO.write(img, "png", file);
    }

    void print(Color color, String str, int x, int y) {
        x -= ax;
        y -= az;
        gfx.setColor(Color.BLACK);
        gfx.drawString(str, x - 1, y - 1);
        gfx.drawString(str, x + 1, y - 1);
        gfx.drawString(str, x - 1, y + 1);
        gfx.drawString(str, x + 1, y + 1);
        gfx.drawString(str, x + 1, y);
        gfx.drawString(str, x - 1, y);
        gfx.drawString(str, x, y + 1);
        gfx.drawString(str, x, y - 1);
        gfx.setColor(color);
        gfx.drawString(str, x, y);
    }

    void rect(Color color, int x, int y, int w, int h) {
        x -= ax;
        y -= az;
        gfx.setColor(Color.BLACK);
        gfx.drawRect(x - 1, y - 1, w, h);
        gfx.setColor(color);
        gfx.drawRect(x, y, w, h);
    }

    void fillRect(Color color, int x, int y, int w, int h) {
        x -= ax;
        y -= az;
        gfx.setColor(color);
        gfx.fillRect(x, y, w, h);
    }

    void pixel(Color color, int x, int y) {
        if (x < ax || x > bx || y < az || y > bz) return;
        img.setRGB(x - ax, y - az, color.getRGB());
    }

    public int findZones() {
        prepareFindZones();
        int steps = 0;
        while (findZonesStep()) {
            steps += 1;
        }
        return steps;
    }

    public void prepareFindZones() {
        zones = new ArrayList<>();
        findZonesPool = new HashMap<>();
        zoneMap = new HashMap<>();
        for (ZoneChunk zoneChunk : chunks) {
            findZonesPool.put(zoneChunk.vec, zoneChunk);
        }
    }

    public boolean findZonesStep() {
        if (findZonesPool.isEmpty()) return false;
        Set<Vec2i> todo = new HashSet<>();
        ZoneChunk pivot = findZonesPool.values().iterator().next();
        findZonesPool.remove(pivot.vec);
        // if (pivot.biome == BiomeGroup.RIVER) return true;
        // if (pivot.biome == BiomeGroup.OCEAN) return true;
        Zone zone = new Zone(pivot.biome);
        zones.add(zone);
        zone.addChunk(pivot.vec);
        todo.clear();
        todo.add(pivot.vec);
        while (!todo.isEmpty()) {
            Vec2i vec = todo.iterator().next();
            todo.remove(vec);
            for (Vec2i nbor : vec.getNeighbors()) {
                if (!findZonesPool.containsKey(nbor)) continue;
                if (todo.contains(nbor)) continue;
                if (zone.containsChunk(nbor)) continue;
                ZoneChunk nborChunk = findZonesPool.get(nbor);
                nbor = nborChunk.vec;
                if (nborChunk.biome != zone.biome) continue;
                findZonesPool.remove(nbor);
                todo.add(nbor);
                zone.addChunk(nbor);
            }
        }
        zone.putIn(zoneMap);
        return true;
    }

    public int mergeZones(int maxSize) {
        int steps = 0;
        while (mergeZonesStep(maxSize)) {
            steps += 1;
        }
        return steps;
    }

    public boolean mergeZonesStep(int maxSize) {
        // Find smallest
        Zone zone = null;
        for (Zone z : zones) {
            if (zone == null || z.size() < zone.size()) zone = z;
        }
        if (zone == null) return false;
        if (zone.size() > maxSize) return false;
        List<Zone> nbors = new ArrayList<>();
        for (Vec2i vec : zone.getBorderChunks()) {
            for (Vec2i vec2 : vec.getNeighbors()) {
                Zone nbor = zoneMap.get(vec2);
                if (nbor == null || zone == nbor) continue;
                nbors.add(nbor);
            }
        }
        if (nbors.isEmpty()) {
            zones.remove(zone);
            return true;
        }
        Zone nbor = nbors.get(0);
        zone.removeFrom(zoneMap);
        nbor.addAll(zone);
        nbor.putIn(zoneMap);
        nbor.essential = nbor.essential || zone.essential;
        zones.remove(zone);
        return true;
    }

    public int mergeRivers() {
        int steps = 0;
        while (mergeRiversStep()) {
            steps += 1;
        }
        return steps;
    }

    /**
     * Merge all rivers into their neighboring zones.
     */
    public boolean mergeRiversStep() {
        boolean result = false;
        for (Zone zone : new ArrayList<>(zones)) {
            if (zone.biome == BiomeGroup.RIVER) continue;
            for (Vec2i chunk : zone.getBorderChunks()) {
                for (Vec2i chunk2 : chunk.getNeighbors()) {
                    Zone nbor = zoneMap.get(chunk2);
                    if (nbor == null || nbor == zone) continue;
                    if (nbor.biome != BiomeGroup.RIVER) continue;
                    zone.addChunk(chunk2);
                    nbor.removeChunk(chunk2);
                    zoneMap.put(chunk2, zone);
                    if (nbor.size() == 0) zones.remove(nbor);
                    result = true;
                }
            }
        }
        return result;
    }

    public int splitLargeZones(int preferredSize) {
        int steps = 0;
        while (splitLargeZonesStep(preferredSize)) steps += 1;
        return steps;
    }

    public boolean splitLargeZonesStep(int preferredSize) {
        Zone zone = null;
        // Find first
        for (Zone z : zones) {
            if (z.size() < preferredSize * 2) continue;
            zone = z;
            break;
        }
        if (zone == null) return false;
        Vec2i dim = Vec2i.dimensionsOf(zone.chunks);
        Vec2i start = dim.x >= dim.y
            ? Vec2i.minX(zone.chunks)
            : Vec2i.minY(zone.chunks);
        Zone newZone = new Zone(zone.biome);
        zones.add(newZone);
        List<Vec2i> todo = new ArrayList<>();
        todo.add(start);
        int finalSize = Math.min(preferredSize, zone.size() / 2);
        random = new Random(start.hashCode());
        while (!todo.isEmpty() && newZone.size() < finalSize) {
            Vec2i chunk = todo.remove(random.nextInt(todo.size()));
            if (!zone.containsChunk(chunk)) continue;
            zone.removeChunk(chunk);
            newZone.addChunk(chunk);
            zoneMap.put(chunk, newZone);
            todo.addAll(chunk.getNeighbors());
        }
        splitIfNecessary(zone);
        return true;
    }

    private boolean splitIfNecessary(Zone zone) {
        List<Set<Vec2i>> list = new ArrayList<>();
        Set<Vec2i> allChunks = new HashSet<>(zone.chunks);
        while (!allChunks.isEmpty()) {
            Set<Vec2i> currentSet = new HashSet<>();
            Vec2i start = allChunks.iterator().next();
            currentSet.add(start);
            list.add(currentSet);
            Set<Vec2i> todo = new HashSet<>();
            todo.add(start);
            while (!todo.isEmpty()) {
                Vec2i vec = todo.iterator().next();
                todo.remove(vec);
                if (!allChunks.contains(vec)) continue;
                allChunks.remove(vec);
                currentSet.add(vec);
                todo.addAll(vec.getNeighbors());
            }
        }
        if (list.size() < 2) return false;
        zones.remove(zone);
        zone.removeFrom(zoneMap);
        for (Set<Vec2i> set : list) {
            Zone newZone = new Zone(zone.biome);
            newZone.addAllChunks(set);
            zones.add(newZone);
            newZone.essential = zone.essential;
            newZone.putIn(zoneMap);
        }
        return true;
    }

    /**
     * Find each biome closest to the center, preferring ones that
     * have the preferred size.
     * Ideally this should be called before biomes are merged.
     */
    public void findEssentialBiomes(int preferredSize) {
        Map<BiomeGroup, Zone> biomeZoneMap = new EnumMap<>(BiomeGroup.class);
        List<BiomeGroup> essentialGroups = Arrays
            .asList(BiomeGroup.FOREST,
                    BiomeGroup.MOUNTAIN,
                    BiomeGroup.DARK_FOREST,
                    BiomeGroup.SWAMP,
                    BiomeGroup.TAIGA,
                    BiomeGroup.DESERT,
                    BiomeGroup.JUNGLE,
                    BiomeGroup.BAMBOO,
                    BiomeGroup.SAVANNA,
                    BiomeGroup.SNOWY,
                    BiomeGroup.FROZEN,
                    BiomeGroup.OCEAN,
                    BiomeGroup.WARM_OCEAN,
                    BiomeGroup.MUSHROOM);
        for (Zone zone : zones) {
            int absCoord = zone.getCenter().minAbsCoord();
            Zone old = biomeZoneMap.get(zone.biome);
            if (old != null && old.size() >= preferredSize && zone.size() < preferredSize) continue;
            if (old == null || absCoord < old.getCenter().minAbsCoord()
                || (old.size() < preferredSize && zone.size() >= preferredSize)) {
                biomeZoneMap.put(zone.biome, zone);
            }
        }
        essentialBiomes = new EnumMap<>(BiomeGroup.class);
        for (BiomeGroup biomeGroup : essentialGroups) {
            Zone zone = biomeZoneMap.get(biomeGroup);
            if (zone != null) {
                zone.essential = true;
                essentialBiomes.put(biomeGroup, zone.getCenter());
            } else {
                System.err.println("No essential zone: " + biomeGroup);
            }
        }
    }

    public void scaleZoneLevels() {
        List<Zone> scaledZones = new ArrayList<>();
        for (Zone zone : zones) {
            zone.computeNeighbors(zoneMap);
            if (zone.essential) {
                zone.level = 0;
                scaledZones.add(zone);
            }
        }
        if (scaledZones.isEmpty()) throw new IllegalStateException("Not starter zones!");
        int distance = 0;
        while (scaledZones.size() < zones.size()) {
            distance += 1;
            maxLevel = distance / 2;
            for (Zone scaled : new ArrayList<>(scaledZones)) {
                for (Zone newZone : scaled.neighbors) {
                    if (scaledZones.contains(newZone)) continue;
                    newZone.level = maxLevel;
                    scaledZones.add(newZone);
                }
            }
            // Pad surrounding (for now) empty biomes, which means
            // oceans, and give them the current level.
            for (Zone scaled : new ArrayList<>(scaledZones)) {
                for (Zone newZone : scaled.neighbors) {
                    if (scaledZones.contains(newZone)) continue;
                    switch (newZone.biome) {
                    case OCEAN:
                    case WARM_OCEAN:
                    case COLD_OCEAN:
                    case RIVER: // Shouldn't exist but let's be sure
                        newZone.level = maxLevel;
                        scaledZones.add(newZone);
                        break;
                    default:
                        break;
                    }
                }
            }
        }
    }

    public int adventurize(Markov markov, Function<Vec2i, BoundingBox> structureGenerator) {
        int steps = 0;
        while (adventurizeStep(markov, structureGenerator)) steps += 1;
        return steps;
    }

    public boolean adventurizeStep(Markov markov, Function<Vec2i, BoundingBox> structureGenerator) {
        Zone zone = null;
        for (Zone z : zones) {
            if (z.structuresDone) continue;
            zone = z;
            break;
        }
        if (zone == null) {
            return false;
        }
        if (zone.customStructures == null) {
            // initialize new zone
            zone.customStructures = new HashMap<>();
            zone.name = markov.generate();
            random = new Random(zone.getCenter().hashCode());
            todoChunks = new ArrayList<>(zone.chunks);
        }
        switch (zone.biome) {
        case OCEAN:
        case WARM_OCEAN:
        case COLD_OCEAN:
        case RIVER: // Shouldn't exist but let's be sure
            zone.structuresDone = true;
            saveZone(zone);
            return true;
        default:
            break;
        }
        if (todoChunks.isEmpty()) {
            zone.structuresDone = true;
            saveZone(zone);
            return true;
        }
        Vec2i structureChunk = todoChunks.remove(random.nextInt(todoChunks.size()));
        BoundingBox bb = structureGenerator.apply(structureChunk);
        if (bb != null) {
            zone.customStructures.put(structureChunk, bb);
            todoChunks.removeIf(c -> c.maxDistance(structureChunk) <= 4);
        }
        return true;
    }

    public void saveZone(Zone zone) {
        Gson gson = new Gson();
        File folder2 = new File(folder, "cavetale.zones");
        folder2.mkdirs();
        Territory t = zone.getTerritory();
        File file = new File(folder2, t.getFileName());
        try (FileWriter fw = new FileWriter(file)) {
            gson.toJson(t, fw);
        } catch (IOException ioe) {
            System.err.println("Saving " + file);
            ioe.printStackTrace();
        }
    }

    public void debug(PrintStream out) {
        int[] count = new int[BiomeGroup.values().length];
        for (Zone zone : zones) {
            count[zone.biome.ordinal()] += zone.size();
        }
        for (BiomeGroup biomeGroup : BiomeGroup.values()) {
            out.println(count[biomeGroup.ordinal()] + " " + biomeGroup + " " + biomeGroup.names);
        }
    }
}
