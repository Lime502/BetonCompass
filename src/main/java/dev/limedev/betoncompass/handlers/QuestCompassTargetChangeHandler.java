package dev.limedev.betoncompass.handlers;

import dev.limedev.betoncompass.BetonCompass;
import org.betonquest.betonquest.api.bukkit.event.QuestCompassTargetChangeEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class QuestCompassTargetChangeHandler implements Listener {
    private final BetonCompass plugin;

    public QuestCompassTargetChangeHandler(BetonCompass plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onChange(@NotNull QuestCompassTargetChangeEvent e) {
        plugin.getCompass(e.getProfile())
                .ifPresent(pc -> pc.setTargetLocation(e.getLocation()));
    }
}
