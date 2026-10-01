package me.voidflame.zivexshards;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Material;

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
            return handlePlayerCommand(player, args);
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

    private boolean handlePlayerCommand(Player player, String[] args) {
        if (args.length == 0) {
            openWallet(player);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "balance" -> {
                long balance = plugin.service().getBalance(player.getUniqueId());
                if (balance < 0) player.sendMessage(plugin.msg("unavailable"));
                else player.sendMessage(plugin.msg("balance-self").replace("{balance}", Long.toString(balance)));
            }
            case "pay", "send", "transfer" -> transfer(player, args);
            case "help" -> sendHelp(player);
            default -> player.sendMessage(plugin.msg("usage-player"));
        }
        return true;
    }

    private void transfer(Player sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("usage-transfer"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found"));
            return;
        }
        if (target.getUniqueId().equals(sender.getUniqueId())) {
            sender.sendMessage(plugin.msg("self-transfer"));
            return;
        }

        long amount;
        try {
            amount = Long.parseLong(args[2]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            sender.sendMessage(plugin.msg("invalid-number"));
            return;
        }

        if (!plugin.service().transfer(sender.getUniqueId(), target.getUniqueId(), amount)) {
            long balance = plugin.service().getBalance(sender.getUniqueId());
            if (balance >= 0 && balance < amount) {
                sender.sendMessage(plugin.msg("insufficient").replace("{player}", sender.getName()));
            } else {
                sender.sendMessage(plugin.msg("transfer-failed"));
            }
            return;
        }

        long senderBalance = plugin.service().getBalance(sender.getUniqueId());
        long targetBalance = plugin.service().getBalance(target.getUniqueId());

        sender.sendMessage(plugin.msg("sent")
                .replace("{amount}", Long.toString(amount))
                .replace("{player}", target.getName())
                .replace("{balance}", Long.toString(senderBalance)));
        target.sendMessage(plugin.msg("received")
                .replace("{amount}", Long.toString(amount))
                .replace("{player}", sender.getName())
                .replace("{balance}", Long.toString(targetBalance)));
        plugin.sound(sender, "success");
        plugin.sound(target, "success");
    }

    private void sendHelp(Player player) {
        player.sendMessage(plugin.msg("help-header"));
        player.sendMessage(plugin.msg("help-balance"));
        player.sendMessage(plugin.msg("help-pay"));
        player.sendMessage(plugin.msg("help-wallet"));
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
                ZivexShardsPlugin.color("&d" + balance + " Shards"),
                ZivexShardsPlugin.color("&7Use &f/shards pay <player> <amount> &7to transfer.")
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
                && !command.getName().equalsIgnoreCase("shards")) {
            return List.of();
        }

        if (command.getName().equalsIgnoreCase("shards") && sender instanceof Player) {
            if (args.length == 1) return List.of("balance", "pay", "send", "transfer", "help");
            if (args.length == 2 && Set.of("pay", "send", "transfer").contains(args[0].toLowerCase(Locale.ROOT))) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> !name.equalsIgnoreCase(sender.getName()))
                        .sorted()
                        .toList();
            }
            return List.of();
        }

        if (!sender.hasPermission("zivexshards.admin")) return List.of();
        if (args.length == 1) return List.of("give", "take", "set", "balance", "reload");
        if (args.length == 2) return Bukkit.getOnlinePlayers().stream().map(Player::getName).sorted().toList();
        return List.of();
    }
}
