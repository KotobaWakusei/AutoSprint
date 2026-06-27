package com.autosprint;

import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.github.retrooper.packetevents.event.SimplePacketListenerAbstract;
import com.github.retrooper.packetevents.event.simple.PacketPlayReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPosition;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientPlayerPositionAndRotation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AutoSprintManager extends SimplePacketListenerAbstract implements Listener {

    private static final Set<UUID> enabledPlayers = ConcurrentHashMap.newKeySet();
    private static final Map<UUID, Vector3d> lastPositions = new ConcurrentHashMap<>();
    private static final Map<UUID, Float> lastYaws = new ConcurrentHashMap<>();

    private final AutoSprint plugin;

    public AutoSprintManager(AutoSprint plugin) {
        super(PacketListenerPriority.LOWEST);
        this.plugin = plugin;
    }

    @Override
    public void onPacketPlayReceive(PacketPlayReceiveEvent event) {
        PacketType.Play.Client type = event.getPacketType();
        if (type != PacketType.Play.Client.PLAYER_POSITION
                && type != PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            return;
        }

        Player player = event.getPlayer();
        if (player == null) return;
        UUID uid = player.getUniqueId();
        if (!enabledPlayers.contains(uid)) return;

        Vector3d pos;
        float yaw;

        if (type == PacketType.Play.Client.PLAYER_POSITION_AND_ROTATION) {
            WrapperPlayClientPlayerPositionAndRotation wrapper =
                    new WrapperPlayClientPlayerPositionAndRotation(event);
            pos = wrapper.getPosition();
            yaw = wrapper.getYaw();
            lastYaws.put(uid, yaw);
        } else {
            WrapperPlayClientPlayerPosition wrapper =
                    new WrapperPlayClientPlayerPosition(event);
            pos = wrapper.getPosition();
            Float cachedYaw = lastYaws.get(uid);
            if (cachedYaw == null) return;
            yaw = cachedYaw;
        }

        Vector3d last = lastPositions.get(uid);
        if (last == null) {
            lastPositions.put(uid, pos);
            return;
        }

        double dx = pos.x - last.x;
        double dz = pos.z - last.z;
        double distSq = dx * dx + dz * dz;

        if (distSq > 0.0001
                && dx * -Math.sin(Math.toRadians(yaw))
                 + dz * Math.cos(Math.toRadians(yaw)) > 0.05) {
            scheduleSprint(uid);
        }

        lastPositions.put(uid, pos);
    }

    private void scheduleSprint(UUID uid) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(uid);
            if (player != null && player.isOnline()
                    && !player.isSprinting()
                    && AutoSprint.canSprint(player, plugin.getMinFood())) {
                player.setSprinting(true);
            }
        });
    }

    public static boolean isEnabled(Player player) {
        return enabledPlayers.contains(player.getUniqueId());
    }

    public static void setEnabled(Player player, boolean state) {
        if (state) {
            enabledPlayers.add(player.getUniqueId());
        } else {
            enabledPlayers.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (plugin.getConfig().getBoolean("default-enabled", true)) {
            enabledPlayers.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID uid = event.getPlayer().getUniqueId();
        enabledPlayers.remove(uid);
        lastPositions.remove(uid);
        lastYaws.remove(uid);
    }

    public static void clear() {
        enabledPlayers.clear();
        lastPositions.clear();
        lastYaws.clear();
    }
}
