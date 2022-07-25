package com.cavetale.territory.generator;

import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.util.Json;
import com.cavetale.territory.BiomeGroup;
import com.cavetale.territory.manager.TerritoryWorld;
import com.cavetale.territory.struct.Territory;
import com.cavetale.territory.util.Vectors;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import static com.cavetale.territory.manager.TerritoryWorld.TERRITORY_FOLDER;

/**
 * Represents a Minecraft world in a folder. With the following:
 * - A biomes.txt file
 *
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
@Getter @RequiredArgsConstructor
public final class GeneratorWorld {
    protected final String worldName;
    private final File folder;
    private final Logger logger;
    protected final Random random = ThreadLocalRandom.current();
    int ax = 0;
    int bx = 0;
    int az = 0;
    int bz = 0;
    int width;
    int height;
    List<GeneratorChunk> chunks;
    List<GeneratorZone> zones;
    Map<Vec2i, GeneratorChunk> findZonesPool;
    Map<Vec2i, GeneratorZone> zoneMap;
    Map<BiomeGroup, Vec2i> essentialBiomes;
    BufferedImage img;
    Graphics gfx;
    int generatorState;
    int maxLevel;
    // Used in late stage to generate structures
    protected TerritoryWorld territoryWorld;

    public void loadBiomes() {
        try {
            chunks = GeneratorChunk.fromBiomesFile(new File(folder, "biomes.txt"), logger);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        for (GeneratorChunk chunk : chunks) {
            if (chunk.vec.x < ax) ax = chunk.vec.x;
            if (chunk.vec.x > bx) bx = chunk.vec.x;
            if (chunk.vec.z < az) az = chunk.vec.z;
            if (chunk.vec.z > bz) bz = chunk.vec.z;
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
        for (GeneratorChunk chunk : chunks) {
            int x = chunk.vec.x - ax;
            int y = chunk.vec.z - az;
            img.setRGB(x, y, chunk.biome.color.getRGB());
        }
        if (!unhandled.isEmpty()) {
            logger.severe("[GeneratorWorld] Unhandled biomes: " + unhandled);
        }
    }

    public void drawZones(boolean fill, boolean dynamicColor) {
        for (GeneratorZone zone : zones) {
            Color color;
            if (dynamicColor) {
                Vec2i center = zone.getCenter();
                float east = (float) (center.x - ax) / (float) width;
                float[] hsb = Color.RGBtoHSB(zone.biomeGroup.color.getRed(),
                                             zone.biomeGroup.color.getGreen(),
                                             zone.biomeGroup.color.getBlue(),
                                             new float[3]);
                float lvl = (float) zone.level / (float) maxLevel;
                color = Color.getHSBColor(hsb[0], hsb[1], 1f - lvl);
            } else {
                color = zone.biomeGroup.color;
            }
            if (fill) {
                for (Vec2i vec : zone.chunks) {
                    pixel(color, vec.x, vec.z);
                }
            } else {
                for (Vec2i vec : zone.getBorderChunks()) {
                    pixel(color, vec.x, vec.z);
                }
            }
        }
    }

    public void drawZoneLabels() {
        for (GeneratorZone zone : zones) {
            Vec2i center = zone.getCenter();
            print(Color.WHITE, "" + zone.level, center.x, center.z);
        }
    }

    public void drawEssentialBiomes() {
        for (Map.Entry<BiomeGroup, Vec2i> entry : essentialBiomes.entrySet()) {
            BiomeGroup biome = entry.getKey();
            Vec2i pos = entry.getValue();
            print(biome.color, biome.name().toLowerCase().replace("_", " "), pos.x, pos.z);
            for (int i = 0; i < 5; i += 1) {
                pixel(biome.color, pos.x + i, pos.z + i);
                pixel(biome.color, pos.x + i, pos.z - i);
                pixel(biome.color, pos.x - i, pos.z + i);
                pixel(biome.color, pos.x - i, pos.z - i);
            }
        }
    }

    public void drawSmallZones(int maxSize) {
        for (GeneratorZone zone : zones) {
            if (zone.size() > maxSize) continue;
            int left = Integer.MAX_VALUE;
            int top = Integer.MAX_VALUE;
            for (Vec2i vec : zone.chunks) {
                if (left > vec.x) left = vec.x;
                if (top > vec.z) top = vec.z;
                img.setRGB(vec.x - ax, vec.z - az, zone.biomeGroup.color.getRGB());
            }
            print(zone.biomeGroup.color, zone.biomeGroup.name(), left, top);
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
        for (GeneratorChunk generatorChunk : chunks) {
            findZonesPool.put(generatorChunk.vec, generatorChunk);
        }
    }

    public boolean findZonesStep() {
        if (findZonesPool.isEmpty()) return false;
        Set<Vec2i> todo = new HashSet<>();
        GeneratorChunk pivot = findZonesPool.values().iterator().next();
        findZonesPool.remove(pivot.vec);
        // if (pivot.biome == BiomeGroup.RIVER) return true;
        // if (pivot.biome == BiomeGroup.OCEAN) return true;
        GeneratorZone zone = new GeneratorZone(pivot.biome);
        zones.add(zone);
        zone.addChunk(pivot.vec);
        todo.clear();
        todo.add(pivot.vec);
        while (!todo.isEmpty()) {
            Vec2i vec = todo.iterator().next();
            todo.remove(vec);
            for (Vec2i nbor : Vectors.neighbors(vec)) {
                if (!findZonesPool.containsKey(nbor)) continue;
                if (todo.contains(nbor)) continue;
                if (zone.containsChunk(nbor)) continue;
                GeneratorChunk nborChunk = findZonesPool.get(nbor);
                nbor = nborChunk.vec;
                if (nborChunk.biome != zone.biomeGroup) continue;
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
        GeneratorZone zone = null;
        for (GeneratorZone z : zones) {
            if (zone == null || z.size() < zone.size()) zone = z;
        }
        if (zone == null) return false;
        if (zone.size() > maxSize) return false;
        List<GeneratorZone> nbors = new ArrayList<>();
        for (Vec2i vec : zone.getBorderChunks()) {
            for (Vec2i vec2 : Vectors.neighbors(vec)) {
                GeneratorZone nbor = zoneMap.get(vec2);
                if (nbor == null || zone == nbor) continue;
                nbors.add(nbor);
            }
        }
        if (nbors.isEmpty()) {
            zones.remove(zone);
            return true;
        }
        GeneratorZone nbor = nbors.get(0);
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
        for (GeneratorZone zone : new ArrayList<>(zones)) {
            if (zone.biomeGroup == BiomeGroup.RIVER) continue;
            for (Vec2i chunk : zone.getBorderChunks()) {
                for (Vec2i chunk2 : Vectors.neighbors(chunk)) {
                    GeneratorZone nbor = zoneMap.get(chunk2);
                    if (nbor == null || nbor == zone) continue;
                    if (nbor.biomeGroup != BiomeGroup.RIVER) continue;
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
        GeneratorZone zone = null;
        // Find first
        for (GeneratorZone z : zones) {
            if (z.size() < preferredSize * 2) continue;
            zone = z;
            break;
        }
        if (zone == null) return false;
        Vec2i dim = Vectors.dimensions(zone.chunks);
        Vec2i start = dim.x >= dim.z
            ? Vectors.minX(zone.chunks)
            : Vectors.minZ(zone.chunks);
        GeneratorZone newZone = new GeneratorZone(zone.biomeGroup);
        zones.add(newZone);
        List<Vec2i> todo = new ArrayList<>();
        todo.add(start);
        int finalSize = Math.min(preferredSize, zone.size() / 2);
        while (!todo.isEmpty() && newZone.size() < finalSize) {
            Vec2i chunk = todo.remove(random.nextInt(todo.size()));
            if (!zone.containsChunk(chunk)) continue;
            zone.removeChunk(chunk);
            newZone.addChunk(chunk);
            zoneMap.put(chunk, newZone);
            todo.addAll(Vectors.neighbors(chunk));
        }
        splitIfNecessary(zone);
        return true;
    }

    private boolean splitIfNecessary(GeneratorZone zone) {
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
                todo.addAll(Vectors.neighbors(vec));
            }
        }
        if (list.size() < 2) return false;
        zones.remove(zone);
        zone.removeFrom(zoneMap);
        for (Set<Vec2i> set : list) {
            GeneratorZone newZone = new GeneratorZone(zone.biomeGroup);
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
        Map<BiomeGroup, GeneratorZone> biomeZoneMap = new EnumMap<>(BiomeGroup.class);
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
        for (GeneratorZone zone : zones) {
            int absCoord = Vectors.minAbsCoord(zone.getCenter());
            GeneratorZone old = biomeZoneMap.get(zone.biomeGroup);
            if (old != null && old.size() >= preferredSize && zone.size() < preferredSize) continue;
            if (old == null || absCoord < Vectors.minAbsCoord(old.getCenter())
                || (old.size() < preferredSize && zone.size() >= preferredSize)) {
                biomeZoneMap.put(zone.biomeGroup, zone);
            }
        }
        essentialBiomes = new EnumMap<>(BiomeGroup.class);
        for (BiomeGroup biomeGroup : essentialGroups) {
            GeneratorZone zone = biomeZoneMap.get(biomeGroup);
            if (zone != null) {
                zone.essential = true;
                essentialBiomes.put(biomeGroup, zone.getCenter());
            } else {
                logger.warning("[GeneratorWorld] No essential zone: " + biomeGroup);
            }
        }
    }

    public void scaleZoneLevels() {
        List<GeneratorZone> scaledZones = new ArrayList<>();
        for (GeneratorZone zone : zones) {
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
            for (GeneratorZone scaled : List.copyOf(scaledZones)) {
                for (GeneratorZone newZone : scaled.neighbors) {
                    if (scaledZones.contains(newZone)) continue;
                    newZone.level = maxLevel;
                    scaledZones.add(newZone);
                }
            }
        }
    }

    public void saveZones() {
        File territoryFolder = new File(folder, TERRITORY_FOLDER);
        territoryFolder.mkdirs();
        zones.sort((a, b) -> {
                int val = Integer.compare((a.essential ? 1 : 0),
                                          (b.essential ? 1 : 0));
                return val != 0
                    ? val
                    : Integer.compare(a.level, b.level);
            });
        int id = 0;
        for (GeneratorZone zone : zones) {
            zone.id = ++id;
            Territory territory = zone.createTerritory();
            File file = new File(territoryFolder, territory.getFileName());
            Json.save(file, territory, true);
        }
    }

    public void debug(PrintStream out) {
        int[] count = new int[BiomeGroup.values().length];
        for (GeneratorZone zone : zones) {
            count[zone.biomeGroup.ordinal()] += zone.size();
        }
        for (BiomeGroup biomeGroup : BiomeGroup.values()) {
            out.println(count[biomeGroup.ordinal()] + " " + biomeGroup + " " + biomeGroup.names);
        }
    }

    public TerritoryWorld getTerritoryWorld() {
        if (territoryWorld == null) {
            territoryWorld = new TerritoryWorld(worldName);
            territoryWorld.load();
            logger.info("[GeneratorWorld] Loaded TerritoryWorld: " + territoryWorld.getTerritories().size());
        }
        return territoryWorld;
    }
}
