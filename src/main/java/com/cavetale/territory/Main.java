package com.cavetale.territory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public final class Main {
    ZoneWorld zoneWorld;

    private Main() { }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.run(args);
    }

    void run(String[] args) throws Exception {
        File folder = new File(args[0]);
        if (!folder.isDirectory()) {
            System.err.println("Not a folder: " + folder);
            System.exit(1);
            return;
        }
        zoneWorld = new ZoneWorld(folder);
        time("loadBiomes", zoneWorld::loadBiomes);
        time("loadStructures", zoneWorld::loadStructures);
        time("findZones", () -> System.out.println("find steps=" + zoneWorld.findZones()));
        zoneWorld.mergeRivers();
        System.out.println("zones=" + zoneWorld.zones.size());
        time("splitLargeZones", () -> System.out.println("split steps=" + zoneWorld.splitLargeZones(1000)));
        System.out.println("zones=" + zoneWorld.zones.size());
        zoneWorld.findEssentialBiomes(100);
        time("mergeZones", () -> System.out.println("merge steps=" + zoneWorld.mergeZones(500)));
        System.out.println("zones=" + zoneWorld.zones.size());
        zoneWorld.scaleZoneLevels();
        Markov markov = new Markov(4);
        markov.scan(new BufferedReader(new FileReader("src/main/resources/names/forest.txt")));
        time("adventurize", () -> zoneWorld.adventurize(markov, this::generateStructure));
        time("draw", () -> {
                zoneWorld.makeImage(0xFF101010);
                zoneWorld.drawZones(true, true);
                zoneWorld.drawZones(false, false);
                //zoneWorld.drawStructures();
                zoneWorld.drawEssentialBiomes();
                zoneWorld.drawZoneLabels();
                zoneWorld.drawCustomStructures();
            });
        zoneWorld.saveImage(new File("map.png"));
        zoneWorld.debug(System.out);
    }

    static void time(String label, Runnable run) {
        long time = System.currentTimeMillis();
        run.run();
        time = System.currentTimeMillis() - time;
        System.out.println("TIME " + label + ": " + ((double) time / 1000.0) + "s");
    }

    BoundingBox generateStructure(Vec2i chunk) {
        int x = chunk.x << 4;
        int z = chunk.y << 4;
        int y = 65;
        BoundingBox result = new BoundingBox("bandit_camp",
                                             new Vec3i(x, y - 4, z),
                                             new Vec3i(x + 15, y + 4, z + 15));
        for (BoundingBox bb : zoneWorld.structures) {
            if (bb.overlaps(result)) {
                System.out.println("Overlaps: " + result + " / " + bb);
                return null;
            }
        }
        return result;
    }
}
