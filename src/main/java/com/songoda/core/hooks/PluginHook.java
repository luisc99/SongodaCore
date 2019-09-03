package com.songoda.core.hooks;

import com.songoda.core.hooks.economies.Economy;
import com.songoda.core.hooks.economies.PlayerPointsEconomy;
import com.songoda.core.hooks.economies.ReserveEconomy;
import com.songoda.core.hooks.economies.VaultEconomy;
import com.songoda.core.hooks.stackers.StackMob;
import com.songoda.core.hooks.stackers.Stacker;
import com.songoda.core.hooks.stackers.UltimateStacker;
import com.songoda.core.hooks.stackers.WildStacker;
import com.songoda.core.hooks.holograms.Holograms;
import com.songoda.core.hooks.holograms.HologramsHolograms;
import com.songoda.core.hooks.holograms.HolographicDisplaysHolograms;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

public final class PluginHook <T extends Class> {

    public static final PluginHook ECO_VAULT            = new PluginHook(Economy.class, "Vault", VaultEconomy.class);
    public static final PluginHook ECO_PLAYER_POINTS    = new PluginHook(Economy.class, "PlayerPoints", PlayerPointsEconomy.class);
    public static final PluginHook ECO_RESERVE          = new PluginHook(Economy.class, "Reserve", ReserveEconomy.class);
    public static final PluginHook STACKER_ULTIMATE     = new PluginHook(Stacker.class, "UltimateStacker", UltimateStacker.class);
    public static final PluginHook STACKER_WILD         = new PluginHook(Stacker.class, "WildStacker", WildStacker.class);
    public static final PluginHook STACKER_STACK_MOB    = new PluginHook(Stacker.class, "StackMob", StackMob.class);
    public static final PluginHook HOLO_DISPLAYS        = new PluginHook(Holograms.class, "HolographicDisplays", HolographicDisplaysHolograms.class);
    public static final PluginHook HOLO_HOLOGRAMS       = new PluginHook(Holograms.class, "Holograms", HologramsHolograms.class);

    /******* Start Manager stuff *******/

    protected final T hookGeneric;
    protected final String plugin;
    protected final Class managerClass;
    protected static Map<Class, PluginHook> hooks;
    protected Constructor pluginConstructor;

    private PluginHook(T type, String pluginName, Class handler) {
        if (!Hook.class.isAssignableFrom(handler)) {
            throw new RuntimeException("Tried to register a non-Hook plugin hook! " + pluginName + " -> " + handler.getName());
        }
        this.hookGeneric = type;
        this.plugin = pluginName;
        this.managerClass = handler;
        if (hooks == null) {
            hooks = new LinkedHashMap();
        }
        hooks.put(handler, this);
        // Does this class have a plugin constructor?
        try {
            pluginConstructor = type.getDeclaredConstructor(Plugin.class);
        } catch (NoSuchMethodException | SecurityException ex) {
            // nope!
        }
    }

    protected static Map<PluginHook, Hook> loadHooks(Class type, Plugin plugin) {
        Map<PluginHook, Hook> loaded = new LinkedHashMap<>();
        PluginManager pluginManager = Bukkit.getPluginManager();

        for (PluginHook hook : getHooks(type)) {
            if (pluginManager.isPluginEnabled(hook.plugin)) {
                Hook handler = (Hook) (plugin != null ? hook.load(plugin) : hook.load());
                if (handler != null && handler.isEnabled()) {
                    loaded.put(hook, handler);
                }
            }
        }

        return loaded;
    }

    protected static List<PluginHook> getHooks(Class type) {
        return hooks.entrySet().parallelStream()
                .filter(e -> e.getKey() == type || e.getValue().managerClass == type || type.isAssignableFrom(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public String getPluginName() {
        return plugin;
    }

    protected Object load() {
        try {
            return managerClass.cast(
                    pluginConstructor != null
                            ? pluginConstructor.newInstance((Plugin) null)
                            : managerClass.getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Unexpected Error while creating a new Hook Manager for " + plugin, ex);
        }
        return null;
    }

    protected Object load(Plugin hookingPlugin) {
        try {
            return managerClass.cast(
                    pluginConstructor != null
                            ? pluginConstructor.newInstance(hookingPlugin)
                            : managerClass.getConstructor().newInstance());
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException ex) {
            Bukkit.getLogger().log(Level.SEVERE, "Unexpected Error while creating a new Hook Manager for " + plugin, ex);
        }
        return null;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + Objects.hashCode(this.plugin);
        hash = 37 * hash + Objects.hashCode(this.managerClass);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PluginHook<?> other = (PluginHook<?>) obj;
        return Objects.equals(this.plugin, other.plugin)
                && Objects.equals(this.managerClass, other.managerClass);
    }
}