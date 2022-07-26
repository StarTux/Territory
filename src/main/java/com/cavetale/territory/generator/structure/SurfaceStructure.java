package com.cavetale.territory.generator.structure;

import com.cavetale.area.struct.Area;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.core.struct.Vec2i;
import com.cavetale.core.struct.Vec3i;
import com.cavetale.core.util.Json;
import com.cavetale.structure.cache.Structure;
import com.cavetale.territory.struct.TerritoryStructure;
import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import static com.cavetale.territory.TerritoryPlugin.territoryPlugin;

/**
 * A pre-made structure to be placed on the surface.
 *
 * Blocks are stored in the originWorld.  Areas are marked via the
 * Area plugin.
 *
 * Nomenclature: orignXYZ => targetXYZ
 * Members: origin
 */
@Getter
public final class SurfaceStructure implements GeneratorStructure {
    public static final NamespacedKey SURFACE_STRUCTURE_KEY = NamespacedKey.fromString("territory:surface_structure");
    private final World originWorld;
    private final String name;
    private final Vec3i anchor;
    private final Cuboid boundingBox;
    private final Map<String, List<Cuboid>> markers = new HashMap<>();

    protected SurfaceStructure(final World world, final String name, final List<Area> areas) {
        this.originWorld = world;
        this.name = name;
        Vec3i theAnchor = null;
        this.boundingBox = areas.get(0).toCuboid();
        for (Area area : areas.subList(1, areas.size())) {
            if (area.name == null) {
                continue;
            }
            switch (area.name) {
            case "anchor":
                theAnchor = area.min;
                break;
            case "air":
                markers.computeIfAbsent("air", n -> new ArrayList<>()).add(area.toCuboid());
                break;
            case "ground":
                markers.computeIfAbsent("ground", n -> new ArrayList<>()).add(area.toCuboid());
                break;
            default:
                territoryPlugin().getLogger().warning("[SurfaceStructure] [" + name + "] Unknown area name: " + area.name);
            }
        }
        if (theAnchor != null) {
            this.anchor = theAnchor;
        } else {
            Block block = world.getBlockAt((boundingBox.ax + boundingBox.bx) / 2,
                                           boundingBox.ay,
                                           (boundingBox.az + boundingBox.bz) / 2);
            while (block.isEmpty() && block.getY() < boundingBox.by) {
                block = block.getRelative(0, 1, 0);
            }
            this.anchor = Vec3i.of(block);
        }
        territoryPlugin().getLogger().info("[SurfaceStructure] " + name
                                           + " anchor:" + anchor);
    }

    @Override
    public boolean isValid() {
        return name != null && anchor != null && boundingBox != null;
    }

    @Override
    public List<Cuboid> getMarkers(String markerName) {
        return markers.getOrDefault(markerName, List.of());
    }

    private static final MaterialSetTag SURFACE_AIR_REPLACEABLES = new MaterialSetTag(NamespacedKey.fromString("territory:surface_air_replaceables"));
    private static final MaterialSetTag SURFACE_GROUND_REPLACEABLES = new MaterialSetTag(NamespacedKey.fromString("territory:surface_ground_replaceables"));

    static {
        SURFACE_AIR_REPLACEABLES.add(Tag.LOGS.getValues());
        SURFACE_AIR_REPLACEABLES.add(Tag.LEAVES.getValues());
        SURFACE_AIR_REPLACEABLES.add(Tag.FLOWERS.getValues());
        SURFACE_AIR_REPLACEABLES.add(Material.GRASS, Material.TALL_GRASS,
                                     Material.POWDER_SNOW, Material.SNOW,
                                     Material.MOSS_CARPET);
        SURFACE_AIR_REPLACEABLES.lock();
        SURFACE_GROUND_REPLACEABLES.add(Tag.DIRT.getValues());
        SURFACE_GROUND_REPLACEABLES.add(Tag.BASE_STONE_OVERWORLD.getValues());
        SURFACE_GROUND_REPLACEABLES.add(Tag.STONE_ORE_REPLACEABLES.getValues());
        SURFACE_GROUND_REPLACEABLES.add(MaterialTags.ORES.getValues());
        SURFACE_GROUND_REPLACEABLES.add(Material.GRASS_BLOCK, Material.MOSS_BLOCK,
                                        Material.DIRT, Material.COARSE_DIRT, Material.PODZOL,
                                        Material.STONE, Material.DIORITE, Material.GRANITE, Material.ANDESITE);
        SURFACE_GROUND_REPLACEABLES.lock();
    }

    /**
     * Check if a block is above ground.
     */
    private boolean isSurfaceAirReplaceable(Block block) {
        Material mat = block.getType();
        return mat.isAir()
            || SURFACE_AIR_REPLACEABLES.isTagged(mat)
            || block.getCollisionShape().getBoundingBoxes().isEmpty();
    }

    private boolean isSurfaceGroundReplaceable(Block block) {
        Material mat = block.getType();
        return SURFACE_GROUND_REPLACEABLES.isTagged(mat);
    }

    /**
     * Place this structure in the target world.
     * This is called after:
     * - No structure, natural or custom, is found nearby
     * - The highest block has been found
     * - The bounding box has been created based on said block:
     *   Via this#createWorldBoundingBox
     *
     * So, we can reasonably assume that ground and dirt blocks are
     * part of the ground, while air blocks are above.  Other ground
     * structures like trees must also be considered for replacement.
     *
     * This function will check if the location is valid and return
     * false otherwise.  Specifically, all "air" markers (Area
     * cuboids) must be an considered air, and "ground" markers must
     * be considered ground.
     */
    @Override
    public PlacementResult canPlace(World targetWorld, Cuboid targetBoundingBox) {
        Vec3i targetOffset = targetBoundingBox.getMin();
        Vec3i originOffset = this.boundingBox.getMin();
        for (Cuboid airMarker : getMarkers("air")) {
            for (Vec3i targetPos : airMarker.shift(originOffset.negate()).shift(targetOffset).enumerate()) {
                if (!isSurfaceAirReplaceable(targetPos.toBlock(targetWorld))) {
                    return PlacementResult.Type.AIR.make(targetPos);
                }
            }
        }
        for (Cuboid airMarker : getMarkers("ground")) {
            for (Vec3i targetPos : airMarker.shift(originOffset.negate()).shift(targetOffset).enumerate()) {
                if (!isSurfaceGroundReplaceable(targetPos.toBlock(targetWorld))) {
                    return PlacementResult.Type.GROUND.make(targetPos);
                }
            }
        }
        // None of the floor blocks may hover in midair!
        for (int z = 0; z < boundingBox.getSizeZ(); z += 1) {
            for (int x = 0; x < boundingBox.getSizeX(); x += 1) {
                Y: for (int y = 0; y < boundingBox.getSizeY(); y += 1) {
                    Vec3i originPos = originOffset.add(x, y, z);
                    BlockData originBlockData = originPos.toBlock(originWorld).getBlockData();
                    if (originPos.toBlock(originWorld).isEmpty()) continue Y;
                    Vec3i targetPos = targetOffset.add(x, y, z);
                    if (!isSurfaceGroundReplaceable(targetPos.toBlock(targetWorld))) {
                        return PlacementResult.Type.AUTO.make(targetPos);
                    } else {
                        break Y; // Match found!
                    }
                }
            }
        }
        return PlacementResult.Type.SUCCESS.make(0, 0, 0);
    }

    /**
     * Place the structure and never fail.
     */
    @Override
    public Structure place(World targetWorld, Cuboid targetBoundingBox, Vec2i chunkVector) {
        Vec3i targetOffset = targetBoundingBox.getMin();
        Vec3i originOffset = this.boundingBox.getMin();
        for (int z = 0; z < boundingBox.getSizeZ(); z += 1) {
            for (int x = 0; x < boundingBox.getSizeX(); x += 1) {
                boolean pillarStarted = false;
                for (int y = 0; y < boundingBox.getSizeY(); y += 1) {
                    Vec3i originPos = originOffset.add(x, y, z);
                    Vec3i targetPos = targetOffset.add(x, y, z);
                    BlockData blockData = originPos.toBlock(originWorld).getBlockData();
                    if (!pillarStarted && blockData.getMaterial().isAir()) continue;
                    pillarStarted = true;
                    targetPos.toBlock(targetWorld).setBlockData(blockData, false);
                }
            }
        }
        TerritoryStructure territoryStructure = new TerritoryStructure();
        territoryStructure.setName(name);
        return new Structure(targetWorld.getName(),
                             SURFACE_STRUCTURE_KEY,
                             chunkVector,
                             targetBoundingBox,
                             Json.serialize(territoryStructure));
    }
}
