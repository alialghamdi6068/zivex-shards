package me.voidflame.zivexshards;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import java.lang.reflect.Method;
import java.util.UUID;

/** Thin compatibility adapter. ZivexShards never creates a second database. */
final class CoreDatabaseBridge {
    private final ZivexShardsPlugin plugin;
    private Object service;
    private Method query;
    private Method execute;

    CoreDatabaseBridge(ZivexShardsPlugin plugin) { this.plugin = plugin; }

    boolean connect() {
        Plugin core = Bukkit.getPluginManager().getPlugin(plugin.getConfig().getString("core.plugin", "VoidFlame-Core"));
        if (core == null || !core.isEnabled()) return false;
        try {
            Method getter = core.getClass().getMethod(plugin.getConfig().getString("core.database-service-getter", "getDatabaseService"));
            service = getter.invoke(core);
            if (service == null) return false;
            String q = plugin.getConfig().getString("core.query-method", "query");
            String e = plugin.getConfig().getString("core.execute-method", "execute");
            query = find(service.getClass(), q);
            execute = find(service.getClass(), e);
            return query != null && execute != null;
        } catch (ReflectiveOperationException ex) {
            plugin.getLogger().warning("Could not connect to VoidFlame-Core DatabaseService: " + ex.getMessage());
            return false;
        }
    }

    Object query(String sql, Object... args) throws Exception { return invoke(query, sql, args); }
    Object execute(String sql, Object... args) throws Exception { return invoke(execute, sql, args); }

    private Object invoke(Method method, String sql, Object[] args) throws Exception {
        if (method == null) throw new IllegalStateException("Database method unavailable");
        Class<?>[] p = method.getParameterTypes();
        if (p.length == 1) return method.invoke(service, sql);
        if (p.length == 2 && p[1].isArray()) return method.invoke(service, sql, args);
        return method.invoke(service, sql, args);
    }

    private Method find(Class<?> type, String name) {
        for (Method m : type.getMethods()) if (m.getName().equals(name)) return m;
        return null;
    }
}
