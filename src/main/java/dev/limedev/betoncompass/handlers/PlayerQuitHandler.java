package dev.limedev.betoncompass.handlers;

import dev.limedev.betoncompass.BetonCompass;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

public class PlayerQuitHandler implements Listener {
    private final BetonCompass plugin;

    public PlayerQuitHandler(BetonCompass plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(@NotNull PlayerQuitEvent e) {
        plugin.deleteCompass(e.getPlayer());
    }
}