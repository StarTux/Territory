package com.cavetale.territory;

import java.io.File;

public final class Main {
    private Main() { }

    public static void main(String[] args) throws Exception {
        File folder = new File(args[0]);
        if (!folder.isDirectory()) {
            System.err.println("Not a folder: " + folder);
            System.exit(1);
            return;
        }
        ZoneWorld zoneWorld = new ZoneWorld(folder);
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
        time("draw", () -> {
                zoneWorld.makeImage(0xFF101010);
                zoneWorld.drawZones(true, true);
                zoneWorld.drawZones(false, false);
                //zoneWorld.drawZoneLabels();
                //zoneWorld.drawStructures();
                zoneWorld.drawEssentialBiomes();
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
}
