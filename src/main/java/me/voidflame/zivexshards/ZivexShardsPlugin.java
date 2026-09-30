package me.voidflame.zivexshards;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class ZivexShardsPlugin extends JavaPlugin {
    private ShardService service;

    @Override public void onEnable() {
        saveDefaultConfig();
        CoreDatabaseBridge bridge=new CoreDatabaseBridge(this);
        if(!bridge.connect()) { getLogger().severe("VoidFlame-Core DatabaseService is unavailable. Shards disabled safely."); return; }
        try {
            CoreShardService impl=new CoreShardService(this,bridge);
            impl.initialize();
            service=impl;
            Bukkit.getServicesManager().register(ShardService.class,service,this,ServicePriority.Normal);
            ShardsCommand command=new ShardsCommand(this);
            PluginCommand shards=getCommand("shards"); PluginCommand admin=getCommand("shardsadmin");
            if(shards!=null) shards.setExecutor(command);
            if(admin!=null) {admin.setExecutor(command);admin.setTabCompleter(command);}
            Bukkit.getPluginManager().registerEvents(new ShardsListener(this),this);
            getLogger().info("ZivexShards enabled and registered as a shared service.");
        } catch(Exception ex) { getLogger().severe("Failed to initialize Shards: "+ex.getMessage()); }
    }

    @Override public void onDisable(){ if(service!=null) Bukkit.getServicesManager().unregister(ShardService.class,service); }
    public ShardService service(){return service;}
    public String msg(String key){return color(getConfig().getString("settings.prefix","")+getConfig().getString("messages."+key,key));}
    public static String color(String s){return ChatColor.translateAlternateColorCodes('&',s==null?"":s);}
    public void sound(org.bukkit.entity.Player p,String key){if(!getConfig().getBoolean("settings.sounds",true))return;String raw=getConfig().getString("sounds."+key+".sound","");try{p.playSound(p.getLocation(),Sound.valueOf(raw), (float)getConfig().getDouble("sounds."+key+".volume",1), (float)getConfig().getDouble("sounds."+key+".pitch",1));}catch(Exception ignored){}}
}
