package com.autosprint;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class AutoSprintCommand implements CommandExecutor {

    private final AutoSprint plugin;

    public AutoSprintCommand(AutoSprint plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("autosprint.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            plugin.reloadConfig();
            if (plugin.getConfig().getBoolean("default-enabled", true)) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    AutoSprintManager.setEnabled(p, true);
                }
            }
            sender.sendMessage(ChatColor.GREEN + "AutoSprint config reloaded.");
            return true;
        }

        if (args.length > 0 && args[0].equalsIgnoreCase("debug")) {
            if (!sender.hasPermission("autosprint.admin")) {
                sender.sendMessage(ChatColor.RED + "No permission.");
                return true;
            }
            boolean current = plugin.getConfig().getBoolean("debug", false);
            plugin.getConfig().set("debug", !current);
            plugin.saveConfig();
            plugin.reloadConfig();
            sender.sendMessage(ChatColor.GRAY + "AutoSprint debug: " + (!current ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"));
            plugin.getDebugger().broadcast("Debug mode %s by %s", !current ? "enabled" : "disabled", sender.getName());
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(ChatColor.RED + "Usage: /autosprint [reload|debug]");
            return true;
        }

        if (!player.hasPermission("autosprint.use")) {
            player.sendMessage(ChatColor.RED + "No permission.");
            return true;
        }

        boolean current = AutoSprintManager.isEnabled(player);
        AutoSprintManager.setEnabled(player, !current);

        if (current) {
            player.sendMessage(ChatColor.GRAY + "AutoSprint » " + ChatColor.RED + "Disabled");
        } else {
            player.sendMessage(ChatColor.GRAY + "AutoSprint » " + ChatColor.GREEN + "Enabled");
        }

        return true;
    }
}
