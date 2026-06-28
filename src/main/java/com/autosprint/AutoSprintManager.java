package com.autosprint;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoSprintManager extends BukkitRunnable implements Listener {

    private static final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();
    private static final Set<UUID> explicitlyDisabled = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Location> lastPositions = new ConcurrentHashMap<>();
    private static final Set<UUID> airBoosted = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Float> originalWalkSpeeds = new ConcurrentHashMap<>();

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

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uid = player.getUniqueId();
            if (!enabledPlayers.contains(uid)) {
                if (airBoosted.remove(uid)) {
                    Float original = originalWalkSpeeds.remove(uid);
                    if (original != null) player.setWalkSpeed(original);
                }
                continue;
            }

            Location loc = player.getLocation();
            Location last = lastPositions.get(uid);
            float yaw = loc.getYaw();

            boolean movingForward = false;
            if (last != null && last.getWorld() != null && last.getWorld().equals(loc.getWorld())) {
                double dx = loc.getX() - last.getX();
                double dz = loc.getZ() - last.getZ();
                double distSq = dx * dx + dz * dz;
                movingForward = distSq > 0.0001
                        && dx * -Math.sin(Math.toRadians(yaw))
                         + dz * Math.cos(Math.toRadians(yaw)) > threshold;
            }

            boolean eligible = movingForward && AutoSprint.canSprint(player, minFood);
            if (eligible) player.setSprinting(true);

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

            lastPositions.put(uid, loc);
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
        lastPositions.remove(uid);
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
        lastPositions.clear();
        airBoosted.clear();
        originalWalkSpeeds.clear();
    }
}