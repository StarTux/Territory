package com.cavetale.territory;

import com.cavetale.core.command.AbstractCommand;
import com.cavetale.core.command.CommandArgCompleter;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.core.struct.Cuboid;
import com.cavetale.territory.generator.structure.GeneratorStructure;
import com.cavetale.territory.generator.structure.GeneratorStructureCache;
import com.cavetale.territory.generator.structure.PlacementResult;
import com.cavetale.territory.manager.Manager;
import com.cavetale.territory.manager.ManagerWorld;
import com.cavetale.territory.manager.TerritoryWorld;
import com.cavetale.territory.struct.Territory;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

public final class TerritoryCommand extends AbstractCommand<TerritoryPlugin> {
    private GeneratorStructureCache debugStructureCache;

    protected TerritoryCommand(final TerritoryPlugin plugin) {
        super(plugin, "territory");
    }

    @Override
    protected void onEnable() {
        rootNode.addChild("here").denyTabCompletion()
            .description("Print info about structure in location")
            .playerCaller(this::here);
        rootNode.addChild("canplace").arguments("<type> <name>")
            .completers(CommandArgCompleter.enumLowerList(TerritoryStructureType.class),
                        CommandArgCompleter.supplyList(() -> getStructureCache().allNames()))
            .description("Test if a structure can be placed")
            .playerCaller(this::canPlace);
    }

    private void here(Player player) {
        Manager manager = plugin.getManager();
        if (manager == null) {
            throw new CommandWarn("Manager disabled! See config.yml");
        }
        ManagerWorld managerWorld = manager.getWorld(player.getWorld().getName());
        if (managerWorld == null) throw new CommandWarn("Not a managed world: " + player.getWorld().getName());
        TerritoryWorld territoryWorld = managerWorld.getTerritoryWorld();
        player.sendMessage(text("World " + managerWorld.getWorldName()
                                + ": " + territoryWorld.getTerritories().size() + " territories", AQUA));
        Territory territory = territoryWorld.at(player.getLocation());
        player.sendMessage(join(noSeparators(),
                                text("Territory ", GRAY), text(territory.getName()),
                                text(" id:", GRAY), text(territory.getId()),
                                text(" lvl:", GRAY), text(territory.getLevel()),
                                text(" center:", GRAY), text("" + territory.getCenter()),
                                text(" name:", GRAY), text(territory.getName()),
                                text(" biome:", GRAY), text(territory.getBiomeGroup().humanName),
                                text(" chunk:", GRAY), text(territory.getChunkCount()))
                           .color(YELLOW));
    }

    private boolean canPlace(Player player, String[] args) {
        if (args.length != 2) return false;
        Cuboid cuboid = Cuboid.requireSelectionOf(player);
        TerritoryStructureType type = CommandArgCompleter.requireEnum(TerritoryStructureType.class, args[0]);
        String name = args[1];
        GeneratorStructure generatorStructure = getStructureCache().getStructure(type, name);
        if (generatorStructure == null) {
            throw new CommandWarn("Structure lNot found: " + type + "/" + name);
        }
        Block base = cuboid.getMin().toBlock(player.getWorld());
        boolean baseBlockAllowed = generatorStructure.canPlace(base);
        Cuboid boundingBox = generatorStructure.createTargetBoundingBox(base);
        PlacementResult result = generatorStructure.canPlace(player.getWorld(), boundingBox);
        String blockData = result.isSuccessful()
            ? "-"
            : result.getVector().toBlock(player.getWorld()).getBlockData().getAsString();
        player.sendMessage(join(noSeparators(),
                                text(result.getType().name(), (result.isSuccessful() ? GREEN : RED)),
                                text("/" + result.getVector(), YELLOW),
                                text("/" + blockData, YELLOW),
                                text(" base:" + cuboid.getMin(), GRAY),
                                text("->" + baseBlockAllowed, (baseBlockAllowed ? GREEN : RED)),
                                text(" bb:" + boundingBox, GRAY)));
        return true;
    }

    private GeneratorStructureCache getStructureCache() {
        if (debugStructureCache == null) {
            debugStructureCache = new GeneratorStructureCache();
            for (String worldName : List.of("structures")) {
                World world = Bukkit.getWorld(worldName);
                if (world == null) {
                    plugin.getLogger().severe("Structure world not found: " + worldName);
                    continue;
                }
                debugStructureCache.load(world);
            }
        }
        return debugStructureCache;
    }
}
