package com.cavetale.territory.manager;

import com.cavetale.territory.struct.Territory;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.entity.Player;

@Getter
public final class Session {
    private final UUID uuid;
    private String name;
    @Setter private Territory territory;

    public Session(final Player player) {
        uuid = player.getUniqueId();
        name = player.getName();
    }

    public void resetTerritory() {
        territory = null;
    }
}
