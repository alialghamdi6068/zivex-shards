package me.voidflame.zivexshards;

import java.util.UUID;

final class CoreShardService implements ShardService {
    private final ZivexShardsPlugin plugin;
    private final CoreDatabaseBridge db;
    private final Object lock = new Object();

    CoreShardService(ZivexShardsPlugin plugin, CoreDatabaseBridge db) { this.plugin = plugin; this.db = db; }

    void initialize() throws Exception {
        db.execute("CREATE TABLE IF NOT EXISTS player_shards (uuid TEXT PRIMARY KEY, balance INTEGER NOT NULL DEFAULT 0)");
    }

    @Override public long getBalance(UUID uuid) {
        synchronized (lock) {
            try {
                Object result = db.query("SELECT balance FROM player_shards WHERE uuid = ?", uuid.toString());
                if (result instanceof Number n) return Math.max(0, n.longValue());
                if (result instanceof Iterable<?> it) {
                    for (Object row : it) {
                        if (row instanceof Number n) return Math.max(0, n.longValue());
                        if (row != null) {
                            try { var m=row.getClass().getMethod("getLong",String.class); return Math.max(0,((Number)m.invoke(row,"balance")).longValue()); } catch(Exception ignored) {}
                        }
                    }
                }
            } catch (Exception ex) { plugin.getLogger().warning("Shard balance query failed: " + ex.getMessage()); }
            return 0;
        }
    }

    @Override public boolean deposit(UUID uuid, long amount) { return change(uuid, amount); }
    @Override public boolean withdraw(UUID uuid, long amount) { if (amount < 0) return false; return change(uuid, -amount); }

    @Override public boolean setBalance(UUID uuid, long amount) {
        if (amount < 0) return false;
        synchronized(lock) {
            try {
                db.execute("INSERT INTO player_shards(uuid,balance) VALUES(?,?) ON CONFLICT(uuid) DO UPDATE SET balance=excluded.balance", uuid.toString(), amount);
                return true;
            } catch(Exception ex) { plugin.getLogger().warning("Shard balance update failed: " + ex.getMessage()); return false; }
        }
    }

    private boolean change(UUID uuid, long delta) {
        if (delta == Long.MIN_VALUE) return false;
        synchronized(lock) {
            long current=getBalance(uuid);
            long next;
            try { next=Math.addExact(current,delta); } catch(ArithmeticException e){return false;}
            long max=plugin.getConfig().getLong("settings.max-balance",Integer.MAX_VALUE);
            if(next<0 || next>max) return false;
            return setBalance(uuid,next);
        }
    }
}
