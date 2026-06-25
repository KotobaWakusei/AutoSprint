package com.autosprint;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class AutoSprintManager implements Listener {

    private static final Set<UUID> enabledPlayers = new HashSet<>();
    private final AutoSprint plugin;

    public AutoSprintManager(AutoSprint plugin) {
        this.plugin = plugin;
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
        enabledPlayers.remove(event.getPlayer().getUniqueId());
    }

    public static void clear() {
        enabledPlayers.clear();
    }
}
