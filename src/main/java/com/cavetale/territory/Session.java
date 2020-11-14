package com.cavetale.territory;

import java.util.UUID;
import org.bukkit.entity.Player;

public final class Session {
    private final UUID uuid;
    private String name;
    private String worldName;
    private Vec2i territoryCenter;

    public Session(final Player player) {
        uuid = player.getUniqueId();
        name = player.getName();
    }

    public void resetTerritory() {
        this.worldName = null;
        this.territoryCenter = null;
    }

    public void setTerritory(Territory territory) {
        this.worldName = territory.tworld.worldName;
        this.territoryCenter = territory.center;
    }

    public Territory getTerritory(Manager manager) {
        if (worldName == null || territoryCenter == null) return null;
        TerritoryWorld tworld = manager.getWorld(worldName);
        if (tworld == null) return null;
        return tworld.getTerritoryAtChunk(territoryCenter.x, territoryCenter.y);
    }
}
