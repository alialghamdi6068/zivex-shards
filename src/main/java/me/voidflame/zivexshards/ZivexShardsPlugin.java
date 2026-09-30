package me.voidflame.zivexshards;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZivexShardsPlugin extends JavaPlugin {
    private ShardService service;
    private ShardDatabase database;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!getConfig().getBoolean("settings.enabled", true)) {
            getLogger().info("ZivexShards is disabled in config.");
            return;
        }

        try {
            database = new ShardDatabase(this);
            service = new CoreShardService(this, database);
            ((CoreShardService) service).initialize();

            Bukkit.getServicesManager().register(
                    ShardService.class, service, this, ServicePriority.Normal
            );

            ShardsCommand command = new ShardsCommand(this);
            PluginCommand shards = getCommand("shards");
            PluginCommand admin = getCommand("shardsadmin");
            if (shards != null) shards.setExecutor(command);
            if (admin != null) {
                admin.setExecutor(command);
                admin.setTabCompleter(command);
            }

            Bukkit.getPluginManager().registerEvents(new ShardsListener(this), this);
            getLogger().info("ZivexShards enabled with a local SQLite database.");
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize ZivexShards: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (service != null) {
            Bukkit.getServicesManager().unregister(ShardService.class, service);
        }
        if (database != null) database.close();
    }

    public ShardService service() {
        return service;
    }

    public String msg(String key) {
        String prefix = getConfig().getString("settings.prefix", "");
        return color(prefix + getConfig().getString("messages." + key, key));
    }

    public static String color(String s) {
        return ChatColor.translateAlternateColorCodes('&', s == null ? "" : s);
    }

    public void sound(Player player, String key) {
        if (!getConfig().getBoolean("settings.sounds", true)) return;
        String raw = getConfig().getString("sounds." + key + ".sound", "");
        if (raw == null || raw.isBlank()) return;
        try {
            player.playSound(
                    player.getLocation(),
                    org.bukkit.Sound.valueOf(raw),
                    (float) getConfig().getDouble("sounds." + key + ".volume", 1),
                    (float) getConfig().getDouble("sounds." + key + ".pitch", 1)
            );
        } catch (IllegalArgumentException ignored) {
        }
    }
}
