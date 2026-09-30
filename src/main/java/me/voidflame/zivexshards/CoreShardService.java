package me.voidflame.zivexshards;

import java.util.UUID;

final class CoreShardService implements ShardService {
    private final ZivexShardsPlugin plugin;
    private final ShardDatabase db;

    CoreShardService(ZivexShardsPlugin plugin, ShardDatabase db) {
        this.plugin = plugin;
        this.db = db;
    }

    void initialize() {
        db.initialize();
    }

    @Override
    public long getBalance(UUID uuid) {
        return db.getBalance(uuid);
    }

    @Override
    public boolean deposit(UUID uuid, long amount) {
        long max = plugin.getConfig().getLong("settings.max-balance", Integer.MAX_VALUE);
        if (amount < 0 || max < 0) return false;
        return db.deposit(uuid, amount, max);
    }

    @Override
    public boolean withdraw(UUID uuid, long amount) {
        if (amount < 0) return false;
        return db.withdraw(uuid, amount);
    }

    @Override
    public boolean setBalance(UUID uuid, long amount) {
        long max = plugin.getConfig().getLong("settings.max-balance", Integer.MAX_VALUE);
        if (amount < 0 || amount > max) return false;
        return db.setBalance(uuid, amount);
    }
}
