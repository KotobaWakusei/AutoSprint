package com.autosprint;

import com.github.retrooper.packetevents.PacketEvents;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class AutoSprint extends JavaPlugin {

    private Debugger debug;
    private AutoSprintManager manager;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        debug = new Debugger(this);

        manager = new AutoSprintManager(this);
        PacketEvents.getAPI().getEventManager().registerListener(manager);
        getServer().getPluginManager().registerEvents(manager, this);

        getCommand("autosprint").setExecutor(new AutoSprintCommand(this));

        if (getConfig().getBoolean("default-enabled", true)) {
            for (Player player : getServer().getOnlinePlayers()) {
                AutoSprintManager.setEnabled(player, true);
            }
        }

        debug.info("Enabled v%s (packet-based)", getDescription().getVersion());
    }

    @Override
    public void onDisable() {
        if (manager != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(manager);
        }
        AutoSprintManager.clear();
        debug.info("Disabled, cleaned up");
    }

    public Debugger getDebugger() {
        return debug;
    }

    public int getMinFood() {
        return getConfig().getInt("min-food", 6);
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
        if (!player.isOnGround() && !player.isSwimming()) return false;
        return true;
    }
}
