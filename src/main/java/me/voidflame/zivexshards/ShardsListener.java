package me.voidflame.zivexshards;

import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

final class ShardsListener implements Listener {
    private final ZivexShardsPlugin plugin;

    ShardsListener(ZivexShardsPlugin plugin) {
        this.plugin = plugin;
    }

    private String title() {
        return ZivexShardsPlugin.color(
                plugin.getConfig().getString("settings.gui.title", "&8Shards")
        );
    }

    @EventHandler
    public void click(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (!(event.getView().getTopInventory().getHolder() instanceof WalletHolder)) return;

        event.setCancelled(true);

        int close = plugin.getConfig().getInt("settings.gui.close-slot", 22);
        if (event.getRawSlot() == close) {
            player.closeInventory();
        }
    }

    @EventHandler
    public void drag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof WalletHolder) {
            event.setCancelled(true);
        }
    }
}
