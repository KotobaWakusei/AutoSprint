package com.autosprint;

import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoSprint extends JavaPlugin implements Listener {

    private Debugger debug;
    private int taskId = -1;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        reloadConfig();
        debug = new Debugger(this);

        getServer().getPluginManager().registerEvents(this, this);
        AutoSprintManager manager = new AutoSprintManager(this);
        getServer().getPluginManager().registerEvents(manager, this);

        getCommand("autosprint").setExecutor(new AutoSprintCommand(this));

        if (getConfig().getBoolean("default-enabled", true)) {
            for (Player player : getServer().getOnlinePlayers()) {
                AutoSprintManager.setEnabled(player, true);
            }
        }

        taskId = getServer().getScheduler().runTaskTimer(this, () -> {
            if (!getConfig().getBoolean("enabled", true)) return;
            int minFood = getMinFood();
            for (Player player : getServer().getOnlinePlayers()) {
                if (player.isSprinting()) continue;
                if (canSprint(player, minFood)) {
                    forceSprint(player, "scheduler");
                }
            }
        }, 10L, 1L).getTaskId();

        debug.info("Enabled (task=%d)", taskId);
    }

    @Override
    public void onDisable() {
        if (taskId != -1) {
            getServer().getScheduler().cancelTask(taskId);
            taskId = -1;
        }
        AutoSprintManager.clear();
        debug.info("Disabled, cleaned up");
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (!getConfig().getBoolean("enabled", true)) return;
        if (player.isSprinting()) return;
        if (!canSprint(player, getMinFood())) return;
        if (!movedHorizontally(event)) return;
        forceSprint(player, "move");
    }

    private boolean canSprint(Player player, int minFood) {
        if (!AutoSprintManager.isEnabled(player)) return false;
        if (player.isSwimming()) return false;
        if (player.isGliding()) return false;
        if (player.isInsideVehicle()) return false;
        if (player.isRiptiding()) return false;
        if (player.isSneaking()) return false;
        if (player.isBlocking()) return false;
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) return false;
        if (player.getFoodLevel() <= minFood) return false;
        if (!player.isOnGround()) return false;
        return true;
    }

    private boolean movedHorizontally(PlayerMoveEvent event) {
        double dx = event.getTo().getX() - event.getFrom().getX();
        double dz = event.getTo().getZ() - event.getFrom().getZ();
        return Math.sqrt(dx * dx + dz * dz) > 0.001;
    }

    private void forceSprint(Player player, String source) {
        try {
            player.setSprinting(true);
            debug.fine("Sprint %s [%s]", player.getName(), source);
        } catch (Exception e) {
            debug.warn("Sprint failed %s: %s", player.getName(), e.getMessage());
        }
    }

    public int getMinFood() {
        return getConfig().getInt("min-food", 6);
    }

    public boolean isGloballyEnabled() {
        return getConfig().getBoolean("enabled", true);
    }

    public Debugger getDebugger() {
        return debug;
    }
}
