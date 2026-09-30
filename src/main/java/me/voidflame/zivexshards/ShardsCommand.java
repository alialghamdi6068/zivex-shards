package me.voidflame.zivexshards;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

final class ShardsCommand implements CommandExecutor, TabCompleter {
    private final ZivexShardsPlugin plugin;

    ShardsCommand(ZivexShardsPlugin plugin) {
        this.plugin = plugin;
    }

    private boolean admin(CommandSender sender) {
        if (sender.hasPermission("zivexshards.admin")) return true;
        sender.sendMessage(plugin.msg("no-permission"));
        return false;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        boolean adminCommand = command.getName().equalsIgnoreCase("shardsadmin");

        if (!adminCommand) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.msg("player-only"));
                return true;
            }
            if (!player.hasPermission("zivexshards.use")) {
                player.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            openWallet(player);
            return true;
        }

        if (!admin(sender)) return true;
        if (args.length == 0) {
            sender.sendMessage(plugin.msg("usage-admin"));
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        if (sub.equals("reload")) {
            plugin.reloadConfig();
            sender.sendMessage(plugin.msg("reloaded"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(plugin.msg("usage-admin"));
            return true;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);
        if (target.getName() == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return true;
        }

        if (sub.equals("balance")) {
            long balance = plugin.service().getBalance(target.getUniqueId());
            if (balance < 0) {
                sender.sendMessage(plugin.msg("unavailable"));
                return true;
            }
            sender.sendMessage(plugin.msg("balance")
                    .replace("{player}", target.getName())
                    .replace("{balance}", Long.toString(balance)));
            return true;
        }

        if (!Set.of("give", "take", "set").contains(sub) || args.length < 3) {
            sender.sendMessage(plugin.msg("usage-admin"));
            return true;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
            if (amount < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.msg("invalid-number"));
            return true;
        }

        long before = plugin.service().getBalance(target.getUniqueId());
        if (before < 0) {
            sender.sendMessage(plugin.msg("unavailable"));
            return true;
        }

        if (sub.equals("take") && before < amount) {
            sender.sendMessage(plugin.msg("insufficient").replace("{player}", target.getName()));
            return true;
        }

        boolean success = switch (sub) {
            case "give" -> plugin.service().deposit(target.getUniqueId(), amount);
            case "take" -> plugin.service().withdraw(target.getUniqueId(), amount);
            case "set" -> plugin.service().setBalance(target.getUniqueId(), amount);
            default -> false;
        };

        if (!success) {
            sender.sendMessage(plugin.msg("unavailable"));
            return true;
        }

        long balance = plugin.service().getBalance(target.getUniqueId());
        String message = switch (sub) {
            case "give" -> plugin.msg("gave");
            case "take" -> plugin.msg("took");
            default -> plugin.msg("set");
        };

        sender.sendMessage(message
                .replace("{amount}", Long.toString(amount))
                .replace("{player}", target.getName())
                .replace("{balance}", Long.toString(balance)));

        if (target.isOnline()) {
            Player online = target.getPlayer();
            if (online != null) {
                online.sendMessage(plugin.msg("changed-by-admin")
                        .replace("{balance}", Long.toString(balance)));
                plugin.sound(online, "success");
            }
        }
        return true;
    }

    private void openWallet(Player player) {
        int size = plugin.getConfig().getInt("settings.gui.size", 27);
        if (size != 27) size = 27;

        String title = ZivexShardsPlugin.color(
                plugin.getConfig().getString("settings.gui.title", "&8Shards")
        );
        Inventory inventory = Bukkit.createInventory(null, size, title);

        int balanceSlot = plugin.getConfig().getInt("settings.gui.balance-slot", 13);
        ItemStack item = new ItemStack(Material.AMETHYST_SHARD);
        ItemMeta meta = item.getItemMeta();
        long balance = plugin.service().getBalance(player.getUniqueId());

        meta.setDisplayName(ZivexShardsPlugin.color("&d&lShards"));
        meta.setLore(List.of(
                ZivexShardsPlugin.color("&7Your current balance"),
                ZivexShardsPlugin.color("&8"),
                ZivexShardsPlugin.color("&d" + balance + " Shards")
        ));
        item.setItemMeta(meta);

        if (balanceSlot >= 0 && balanceSlot < size) inventory.setItem(balanceSlot, item);

        int closeSlot = plugin.getConfig().getInt("settings.gui.close-slot", 22);
        if (closeSlot >= 0 && closeSlot < size) {
            inventory.setItem(closeSlot, button(
                    Material.BARRIER, "&cClose", "&7Close this menu"
            ));
        }

        player.openInventory(inventory);
        plugin.sound(player, "open");
    }

    private ItemStack button(Material material, String name, String lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ZivexShardsPlugin.color(name));
        meta.setLore(List.of(ZivexShardsPlugin.color(lore)));
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equalsIgnoreCase("shardsadmin")
                || !sender.hasPermission("zivexshards.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("give", "take", "set", "balance", "reload");
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .sorted()
                    .toList();
        }

        return List.of();
    }
}
