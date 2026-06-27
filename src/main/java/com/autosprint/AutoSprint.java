package com.autosprint;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public class AutoSprint extends JavaPlugin {

    private Debugger debug;
    private AutoSprintManager manager;
    private BukkitTask task;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        debug = new Debugger(this);

        manager = new AutoSprintManager(this);
        getServer().getPluginManager().registerEvents(manager, this);

        getCommand("autosprint").setExecutor(new AutoSprintCommand(this));

        if (getConfig().getBoolean("default-enabled", true)) {
            for (Player player : getServer().getOnlinePlayers()) {
                AutoSprintManager.enableDefault(player);
            }
        }

        long interval = Math.max(1L, getConfig().getLong("interval-ticks", 1L));
        task = manager.runTaskTimer(this, 0L, interval);

        debug.info("Enabled v%s (scheduler-based, interval=%dt)", getDescription().getVersion(), interval);
    }

    @Override
    public void onDisable() {
        if (task != null) {
            task.cancel();
            task = null;
        }
        AutoSprintManager.clear();
        if (debug != null) debug.info("Disabled, cleaned up");
    }

    public Debugger getDebugger() {
        return debug;
    }

    public int getMinFood() {
        return getConfig().getInt("min-food", 6);
    }

    public double getForwardThreshold() {
        return getConfig().getDouble("forward-threshold", 0.05);
    }

    public float getAirWalkSpeed() {
        return (float) getConfig().getDouble("air-walk-speed", 0.3);
    }

    public float getDefaultWalkSpeed() {
        return (float) getConfig().getDouble("default-walk-speed", 0.2);
    }

    public boolean isGloballyEnabled() {
        return getConfig().getBoolean("enabled", true);
    }

    public static boolean canSprint(Player player, int minFood) {
        if (!AutoSprintManager.isEnabled(player)) return false;
        if (player.isGliding()) return false;
        if (player.isInsideVehicle()) return false;
        if (player.isRiptiding()) return false;
        if (player.isSneaking()) return false;
        if (player.isBlocking()) return false;
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return false;
        if (player.getFoodLevel() <= minFood) return false;
        return true;
    }
}