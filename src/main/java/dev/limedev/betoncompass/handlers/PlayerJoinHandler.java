package dev.limedev.betoncompass.handlers;

import dev.limedev.betoncompass.BetonCompass;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerJoinHandler implements Listener {

    private final BetonCompass plugin;

    public PlayerJoinHandler(BetonCompass plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        plugin.getOrCreateCompass(event.getPlayer());
    }
}
