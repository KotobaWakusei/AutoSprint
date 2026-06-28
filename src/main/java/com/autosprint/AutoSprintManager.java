package com.autosprint;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoSprintManager extends BukkitRunnable implements Listener {

    private static final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> explicitlyDisabled = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> airBoosted = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Float> originalWalkSpeeds = new ConcurrentHashMap<>();
    private static final Map<UUID, Boolean> wasEligible = new ConcurrentHashMap<>();

    private final AutoSprint plugin;

    public AutoSprintManager(AutoSprint plugin) {
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!plugin.isGloballyEnabled()) return;

        double threshold = plugin.getForwardThreshold();
        int minFood = plugin.getMinFood();
        float airWalkSpeed = plugin.getAirWalkSpeed();
        Debugger debug = plugin.getDebugger();

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uid = player.getUniqueId();
            if (!enabledPlayers.contains(uid)) {
                Boolean prev = wasEligible.remove(uid);
                if (prev != null && prev && debug.isDebug()) {
                    debug.fine("%s: sprint OFF (disabled)", player.getName());
                }
                if (airBoosted.remove(uid)) {
                    Float original = originalWalkSpeeds.remove(uid);
                    if (original != null) player.setWalkSpeed(original);
                }
                continue;
            }

            Vector vel = player.getVelocity();
            double dx = vel.getX();
            double dz = vel.getZ();

            double dot = 0;
            boolean movingForward = false;
            if (dx * dx + dz * dz > 0.0001) {
                float yaw = player.getLocation().getYaw();
                dot = dx * -Math.sin(Math.toRadians(yaw))
                    + dz * Math.cos(Math.toRadians(yaw));
                movingForward = dot > threshold;
            }

            boolean eligible = movingForward && AutoSprint.canSprint(player, minFood);

            Boolean prev = wasEligible.get(uid);
            if (prev == null || prev != eligible) {
                wasEligible.put(uid, eligible);
                if (debug.isDebug()) {
                    String why;
                    if (eligible) {
                        why = String.format("vel=%.3f,%.3f dot=%.3f", dx, dz, dot);
                    } else {
                        if (!movingForward) why = "not moving forward";
                        else if (player.isGliding()) why = "gliding";
                        else if (player.isInsideVehicle()) why = "vehicle";
                        else if (player.isRiptiding()) why = "riptiding";
                        else if (player.isFlying()) why = "flying";
                        else if (player.isSneaking()) why = "sneaking";
                        else if (player.isBlocking()) why = "blocking";
                        else if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) why = "game mode";
                        else if (player.getFoodLevel() <= minFood) why = "food(" + player.getFoodLevel() + ") <= min(" + minFood + ")";
                        else why = "unknown";
                    }
                    debug.fine("%s: sprint %s (%s)", player.getName(), eligible ? "ON" : "OFF", why);
                }
            }

            if (eligible) {
                player.setSprinting(true);
            } else if (player.isSprinting()) {
                player.setSprinting(false);
            }

            boolean inAir = !player.isOnGround() && !player.isSwimming();
            boolean shouldBoost = eligible && inAir;
            if (shouldBoost) {
                if (airBoosted.add(uid)) {
                    originalWalkSpeeds.put(uid, player.getWalkSpeed());
                }
                player.setWalkSpeed(airWalkSpeed);
            } else if (airBoosted.remove(uid)) {
                Float original = originalWalkSpeeds.remove(uid);
                if (original != null) player.setWalkSpeed(original);
            }
        }
    }

    public static boolean isEnabled(Player player) {
        return enabledPlayers.contains(player.getUniqueId());
    }

    public static void setEnabled(Player player, boolean state) {
        if (state) {
            enabledPlayers.add(player.getUniqueId());
            explicitlyDisabled.remove(player.getUniqueId());
        } else {
            enabledPlayers.remove(player.getUniqueId());
            explicitlyDisabled.add(player.getUniqueId());
        }
    }

    public static void enableDefault(Player player) {
        if (!explicitlyDisabled.contains(player.getUniqueId())) {
            enabledPlayers.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (plugin.getConfig().getBoolean("default-enabled", true)) {
            enableDefault(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID uid = player.getUniqueId();
        enabledPlayers.remove(uid);
        explicitlyDisabled.remove(uid);
        wasEligible.remove(uid);
        if (airBoosted.remove(uid)) {
            Float original = originalWalkSpeeds.remove(uid);
            if (original != null) player.setWalkSpeed(original);
        }
    }

    public static void clear() {
        for (UUID uid : airBoosted) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null) {
                Float original = originalWalkSpeeds.remove(uid);
                if (original != null) p.setWalkSpeed(original);
            }
        }
        enabledPlayers.clear();
        explicitlyDisabled.clear();
        wasEligible.clear();
        airBoosted.clear();
        originalWalkSpeeds.clear();
    }
}