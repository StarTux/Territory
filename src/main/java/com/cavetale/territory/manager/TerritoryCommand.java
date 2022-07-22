package com.cavetale.territory.manager;

import com.cavetale.core.command.CommandNode;
import com.cavetale.core.command.CommandWarn;
import com.cavetale.territory.TerritoryPlugin;
import com.cavetale.territory.struct.Territory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import static net.kyori.adventure.text.Component.join;
import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.JoinConfiguration.noSeparators;
import static net.kyori.adventure.text.format.NamedTextColor.*;

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

    private boolean here(Player player, String[] args) {
        Manager manager = plugin.getManager();
        TerritoryWorld territoryWorld = manager.getWorld(player.getWorld().getName());
        if (territoryWorld == null) throw new CommandWarn("Not a territory world: " + player.getWorld().getName());
        player.sendMessage(text("World " + territoryWorld.worldName
                                + ": " + territoryWorld.getTerritories().size() + " territories", AQUA));
        Territory territory = territoryWorld.at(player.getLocation());
        player.sendMessage(join(noSeparators(),
                                text("Territory ", GRAY), text(territory.getName()),
                                text(" id:", GRAY), text(territory.getId()),
                                text(" lvl:", GRAY), text(territory.getLevel()),
                                text(" c:", GRAY), text("" + territory.getCenter()),
                                text(" name:", GRAY), text(territory.getName()),
                                text(" biome:", GRAY), text(territory.getBiome()))
                           .color(YELLOW));
        return true;
    }
}
