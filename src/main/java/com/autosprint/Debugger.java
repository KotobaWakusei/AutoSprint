package com.autosprint;

import org.bukkit.Bukkit;

import java.util.logging.Logger;

public class Debugger {

    private final AutoSprint plugin;
    private final Logger log;

    public Debugger(AutoSprint plugin) {
        this.plugin = plugin;
        this.log = plugin.getLogger();
    }

    public boolean isDebug() {
        return plugin.getConfig().getBoolean("debug", false);
    }

    public void info(String msg, Object... args) {
        log.info(format(msg, args));
    }

    public void fine(String msg, Object... args) {
        if (isDebug()) {
            log.info("[DEBUG] " + format(msg, args));
        }
    }

    public void warn(String msg, Object... args) {
        log.warning(format(msg, args));
    }

    public void broadcast(String msg, Object... args) {
        String text = "§8[§6AutoSprint§8] §7" + format(msg, args);
        Bukkit.getOnlinePlayers().forEach(p -> {
            if (p.hasPermission("autosprint.debug")) {
                p.sendMessage(text);
            }
        });
        if (isDebug()) {
            log.info("[BROADCAST] " + format(msg, args));
        }
    }

    private String format(String msg, Object[] args) {
        if (args.length == 0) return msg;
        return String.format(msg, args);
    }
}
