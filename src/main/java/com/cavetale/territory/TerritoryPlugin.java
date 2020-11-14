package com.cavetale.territory;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryPlugin extends JavaPlugin {
    private Generator generator;
    private Manager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        if (getConfig().getBoolean("Generator.Enabled") && Bukkit.getPluginManager().isPluginEnabled("Decorator")) {
            generator = new Generator(this).enable();
            getLogger().info("Generator enabled");
        } else {
            getLogger().info("Generator disabled");
        }
        if (getConfig().getBoolean("Manager.Enabled")) {
            manager = new Manager(this).enable();
            getLogger().info("Manager enabled");
        } else {
            getLogger().info("Manager disabled");
        }
    }
}
