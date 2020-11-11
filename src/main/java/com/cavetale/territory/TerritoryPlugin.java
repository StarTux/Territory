package com.cavetale.territory;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryPlugin extends JavaPlugin {
    private Generator generator;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        if (getConfig().getBoolean("Generator") && Bukkit.getPluginManager().isPluginEnabled("Decorator")) {
            generator = new Generator(this).enable();
            getLogger().info("Generator enabled");
        } else {
            getLogger().info("Generator disabled");
        }
    }
}
