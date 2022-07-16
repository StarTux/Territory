package com.cavetale.territory;

import com.cavetale.territory.bb.BoundingBox;
import com.cavetale.territory.generator.ZoneWorld;
import com.cavetale.territory.util.Vec2i;
import com.cavetale.territory.util.Vec3i;
import java.io.File;
import java.util.logging.Logger;

public final class Main {
    private ZoneWorld zoneWorld;
    private final Logger logger;

    private Main() {
        logger = Logger.getLogger("Structure");
    }

    public static void main(String[] args) throws Exception {
        Main main = new Main();
        main.run(args);
    }

    private static void usage() {
        System.err.println("Usage: java -jar Territory.jar biomes|territories <worldpath> <imagepath>");
        System.exit(1);
    }

    private void run(String[] args) throws Exception {
        if (args.length != 3) {
            usage();
            return;
        }
        boolean makeTerritories = false;
        switch (args[0]) {
        case "biomes": break;
        case "territories":
            makeTerritories = true;
            break;
        default: usage(); return;
        }
        File folder = new File(args[1]);
        File imageFile = new File(args[2]);
        if (!folder.isDirectory()) {
            System.err.println("Not a folder: " + folder);
            System.exit(1);
            return;
        }
        zoneWorld = new ZoneWorld(folder, logger);
        time("loadBiomes", zoneWorld::loadBiomes);
        time("findZones", () -> zoneWorld.findZones());
        if (makeTerritories) {
            time("mergeRivers", () -> zoneWorld.mergeRivers());
            time("splitLargeZones", () -> zoneWorld.splitLargeZones(1000));
            time("findEssentialBiomes", () -> zoneWorld.findEssentialBiomes(100));
            time("mergeZones", () -> zoneWorld.mergeZones(500));
        }
        zoneWorld.makeImage(0);
        if (!makeTerritories) {
            zoneWorld.drawBiomes();
        } else {
            zoneWorld.drawZones(true, false);
        }
        zoneWorld.saveImage(imageFile);
        zoneWorld.debug(System.out);
    }

    private static void time(String label, Runnable run) {
        long time = System.currentTimeMillis();
        run.run();
        time = System.currentTimeMillis() - time;
        System.out.println("TIME " + label + ": " + ((double) time / 1000.0) + "s");
    }

    private BoundingBox generateStructure(Vec2i chunk) {
        int x = chunk.x << 4;
        int z = chunk.y << 4;
        int y = 65;
        BoundingBox result = new BoundingBox("bandit_camp",
                                             new Vec3i(x, y - 4, z),
                                             new Vec3i(x + 15, y + 4, z + 15),
                                             chunk);
        // for (BoundingBox bb : zoneWorld.getStructures()) {
        //     if (bb.overlaps(result)) {
        //         System.out.println("Overlaps: " + result + " / " + bb);
        //         return null;
        //     }
        // }
        return result;
    }
}
