package me.voidflame.zivexshards;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.*;

final class ShardsCommand implements CommandExecutor, TabCompleter {
 private final ZivexShardsPlugin plugin;
 ShardsCommand(ZivexShardsPlugin p){plugin=p;}
 private boolean admin(CommandSender s){if(s.hasPermission("zivexshards.admin"))return true;s.sendMessage(plugin.msg("no-permission"));return false;}
 @Override public boolean onCommand(CommandSender s,Command c,String label,String[] a){
  boolean isAdmin=c.getName().equalsIgnoreCase("shardsadmin");
  if(!isAdmin){if(!(s instanceof Player p)){s.sendMessage(plugin.msg("player-only"));return true;} if(!p.hasPermission("zivexshards.use")){s.sendMessage(plugin.msg("no-permission"));return true;} openWallet(p); return true;}
  if(!admin(s))return true;
  if(a.length==0){s.sendMessage(plugin.msg("usage-admin"));return true;}
  String sub=a[0].toLowerCase(Locale.ROOT);
  if(sub.equals("reload")){plugin.reloadConfig();s.sendMessage(plugin.msg("reloaded"));return true;}
  if(a.length<2){s.sendMessage(plugin.msg("usage-admin"));return true;}
  Player target=Bukkit.getPlayerExact(a[1]);
  if(target==null){s.sendMessage(plugin.msg("player-not-found"));return true;}
  if(sub.equals("balance")){s.sendMessage(plugin.msg("balance").replace("{player}",target.getName()).replace("{balance}",Long.toString(plugin.service().getBalance(target.getUniqueId()))));return true;}
  if(a.length<3){s.sendMessage(plugin.msg("usage-admin"));return true;}
  long amount;try{amount=Long.parseLong(a[2]);if(amount<0)throw new NumberFormatException();}catch(NumberFormatException e){s.sendMessage(plugin.msg("invalid-number"));return true;}
  boolean ok=switch(sub){case "give"->plugin.service().deposit(target.getUniqueId(),amount);case "take"->plugin.service().withdraw(target.getUniqueId(),amount);case "set"->plugin.service().setBalance(target.getUniqueId(),amount);default->false;};
  if(!ok){s.sendMessage(plugin.msg("unavailable"));return true;}
  long bal=plugin.service().getBalance(target.getUniqueId());
  String key=switch(sub){case "give"->"gave";case "take"->"took";default->"set";};
  s.sendMessage(plugin.msg(key).replace("{amount}",Long.toString(amount)).replace("{player}",target.getName()).replace("{balance}",Long.toString(bal)));
  target.sendMessage(plugin.msg("changed-by-admin").replace("{balance}",Long.toString(bal)));
  plugin.sound(target,"success"); return true;
 }
 private void openWallet(Player p){
  int size=plugin.getConfig().getInt("settings.gui-size",27); if(size<9||size>54||size%9!=0)size=27;
  Inventory inv=Bukkit.createInventory(null,size,ZivexShardsPlugin.color(plugin.getConfig().getString("settings.gui-title","&8Your Shards")));
  int slot=plugin.getConfig().getInt("settings.balance-slot",13);
  ItemStack item=new ItemStack(Material.AMETHYST_SHARD); ItemMeta meta=item.getItemMeta();
  meta.setDisplayName(ZivexShardsPlugin.color("&d&lShards"));
  meta.setLore(List.of(ZivexShardsPlugin.color("&7Your current balance"),ZivexShardsPlugin.color("&8"),ZivexShardsPlugin.color("&d"+plugin.service().getBalance(p.getUniqueId())+" Shards")));
  item.setItemMeta(meta); if(slot>=0&&slot<size)inv.setItem(slot,item);
  int close=plugin.getConfig().getInt("settings.close-slot",22); if(close>=0&&close<size)inv.setItem(close,button(Material.BARRIER,"&cClose","&7Close this menu"));
  p.openInventory(inv); plugin.sound(p,"open");
 }
 private ItemStack button(Material m,String name,String lore){ItemStack i=new ItemStack(m);ItemMeta x=i.getItemMeta();x.setDisplayName(ZivexShardsPlugin.color(name));x.setLore(List.of(ZivexShardsPlugin.color(lore)));i.setItemMeta(x);return i;}
 @Override public List<String> onTabComplete(CommandSender s,Command c,String l,String[] a){if(!c.getName().equalsIgnoreCase("shardsadmin"))return List.of();if(a.length==1)return List.of("give","take","set","balance","reload");if(a.length==2)return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();return List.of();}
}
