package me.voidflame.zivexshards;

import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;

final class ShardsListener implements Listener {
 private final ZivexShardsPlugin plugin;
 ShardsListener(ZivexShardsPlugin p){plugin=p;}
 @EventHandler public void join(PlayerJoinEvent e){if(plugin.service()!=null) plugin.service().getBalance(e.getPlayer().getUniqueId());}
}
