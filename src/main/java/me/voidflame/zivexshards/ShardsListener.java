package me.voidflame.zivexshards;

import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.entity.Player;

final class ShardsListener implements Listener {
 private final ZivexShardsPlugin plugin;
 ShardsListener(ZivexShardsPlugin p){plugin=p;}
 @EventHandler public void click(InventoryClickEvent e){
  if(!(e.getWhoClicked() instanceof Player p))return;
  String title=ZivexShardsPlugin.color(plugin.getConfig().getString("settings.gui-title","&8Your Shards"));
  if(!e.getView().getTitle().equals(title))return;
  e.setCancelled(true);
  int close=plugin.getConfig().getInt("settings.close-slot",22);
  if(e.getRawSlot()==close)p.closeInventory();
 }
 @EventHandler public void drag(InventoryDragEvent e){
  String title=ZivexShardsPlugin.color(plugin.getConfig().getString("settings.gui-title","&8Your Shards"));
  if(e.getView().getTitle().equals(title))e.setCancelled(true);
 }
}
