package com.cavetale.territory;

import com.cavetale.territory.generator.GeneratorWorld;
import java.io.File;
import java.util.logging.Logger;

public final class Main {
    private GeneratorWorld generatorWorld;
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
        generatorWorld = new GeneratorWorld(folder.getName(), folder, logger);
        time("loadBiomes", generatorWorld::loadBiomes);
        time("findZones", () -> generatorWorld.findZones());
        if (makeTerritories) {
            time("mergeRivers", () -> generatorWorld.mergeRivers());
            time("splitLargeZones", () -> generatorWorld.splitLargeZones(1000));
            time("findEssentialBiomes", () -> generatorWorld.findEssentialBiomes(100));
            time("mergeZones", () -> generatorWorld.mergeZones(500));
        }
        generatorWorld.makeImage(0);
        if (!makeTerritories) {
            generatorWorld.drawBiomes();
        } else {
            generatorWorld.drawZones(true, false);
        }
        generatorWorld.saveImage(imageFile);
        generatorWorld.debug(System.out);
    }

    private static void time(String label, Runnable run) {
        long time = System.currentTimeMillis();
        run.run();
        time = System.currentTimeMillis() - time;
        System.out.println("TIME " + label + ": " + ((double) time / 1000.0) + "s");
    }
}
