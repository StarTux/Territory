package com.cavetale.territory.manager;

import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.territory.Territory;
import com.cavetale.territory.TerritoryPlugin;
import com.cavetale.territory.bb.BoundingBox;
import com.cavetale.territory.bb.Position;
import com.cavetale.territory.util.Vec3i;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

@RequiredArgsConstructor
public final class TerritoryCommand implements TabExecutor {
    private final TerritoryPlugin plugin;
    CommandNode rootNode;

    public TerritoryCommand enable() {
        plugin.getCommand("territory").setExecutor(this);
        rootNode = new CommandNode("territory");
        CommandNode hereNode = rootNode.addChild("here");
        hereNode.playerCaller(this::here);
        return this;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        if (plugin.getManager() == null) {
            sender.sendMessage("Manager disabled! See config.yml");
            return true;
        }
        return rootNode.call(sender, command, alias, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return rootNode.complete(sender, command, alias, args);
    }

    boolean here(Player player, String[] args) {
        Manager manager = plugin.getManager();
        TerritoryWorld tworld = manager.getWorld(player.getWorld().getName());
        if (tworld == null) throw new CommandWarn("Not a territory world: " + player.getWorld().getName());
        player.sendMessage("World " + tworld.worldName + ": " + tworld.territoryList.size() + " territories");
        Location location = player.getLocation();
        // Territory
        Territory territory = tworld.getTerritoryAtChunk(location.getBlockX() >> 4, location.getBlockZ() >> 4);
        if (territory == null) throw new CommandWarn("There is no territory here!");
        player.sendMessage("Territory " + territory.name
                           + " biome=" + territory.biome
                           + " lvl=" + territory.level
                           + " center=" + territory.center
                           + " chunks=" + territory.chunks.size()
                           + " structs=" + territory.customStructures.size());
        // Custom Structure
        BoundingBox bb = territory.customStructureAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (bb == null) throw new CommandWarn("Not custom structure here!");
        player.sendMessage("Structure " + bb.name + " " + bb.min + "-" + bb.max);
        // Positions
        if (bb.getPositions() != null && !bb.getPositions().isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Position position : bb.getPositions()) {
                names.add(position.toString());
                Vec3i vec = position.vector;
                Location loc = player.getWorld().getBlockAt(vec.x, vec.y, vec.z).getLocation().add(.5, .5, .5);
                player.spawnParticle(Particle.END_ROD, loc, 1, .0, .0, .0, .0);
            }
            player.sendMessage("Positions " + String.join(", ", names));
        }
        // Look at Block
        Block block = player.getTargetBlock(4);
        if (block != null) {
            Position position = bb.getPositionAt(block.getX(), block.getY(), block.getZ());
            if (position != null) {
                player.sendMessage("LookAt " + position.name + " " + position.vector);
            }
        }
        return true;
    }
}
