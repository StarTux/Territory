package com.cavetale.territory;

import com.cavetale.territory.generator.Generator;
import com.cavetale.territory.manager.Manager;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class TerritoryPlugin extends JavaPlugin {
    private static TerritoryPlugin instance;
    private Generator generator;
    @Getter private Manager manager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        if (getConfig().getBoolean("Generator.Enabled")) {
            if (!Bukkit.getPluginManager().isPluginEnabled("Decorator")) {
                throw new IllegalStateException("Decorator plugin not enabled!");
            }
            if (!Bukkit.getPluginManager().isPluginEnabled("Area")) {
                throw new IllegalStateException("Area plugin not enabled!");
            }
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

    public static TerritoryPlugin territoryPlugin() {
        return instance;
    }
}
