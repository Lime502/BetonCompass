package dev.limedev.betoncompass.controllers;

import dev.limedev.betoncompass.MainConfigManager;
import dev.limedev.betoncompass.utils.AngleUtil;
import lombok.Setter;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.QuestException;
import org.betonquest.betonquest.api.identifier.CompassIdentifier;
import org.betonquest.betonquest.api.logger.BetonQuestLogger;
import org.betonquest.betonquest.api.profile.OnlineProfile;
import org.betonquest.betonquest.database.PlayerData;
import org.betonquest.betonquest.feature.QuestCompass;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;
import me.clip.placeholderapi.PlaceholderAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PlayerCompass extends BukkitRunnable {

    private final List<QuestCompass> activeCompasses = new ArrayList<>();
    private final BossBar bossBarCompass;
    private final BossBar bossBarMessage;
    private final BetonQuest betonQuest;
    private final MainConfigManager mainConfig;
    private final Player player;
    private final OnlineProfile profile;

    private String lastCompassTitle = "";
    private String lastMessageTitle = "";
    private float interpolatedYaw;
    private int logicTickCounter = 0;
    private int papiTickCounter = 0;

    private String cachedPrefix = "";
    private String cachedPostfix = "";

    @Setter @Nullable private Location targetLocation;

    public PlayerCompass(final BetonQuestLogger logger, final BetonQuest betonQuest, final MainConfigManager mainConfig, final Player player) {
        this.betonQuest = betonQuest;
        this.mainConfig = mainConfig;
        this.player = player;
        this.profile = betonQuest.getProfileProvider().getProfile(player);
        this.bossBarCompass = Bukkit.createBossBar("", mainConfig.getBarColor(), mainConfig.getBarStyle());
        this.bossBarCompass.addPlayer(player);
        this.bossBarMessage = Bukkit.createBossBar("", mainConfig.getBarColor(), mainConfig.getBarStyle());
        this.bossBarMessage.addPlayer(player);
        this.interpolatedYaw = player.getLocation().getYaw();
        this.updateCompassLocations();
        this.updatePapiAsync();
    }

    public void deleteBossBar() {
        bossBarCompass.removeAll();
        bossBarMessage.removeAll();
        try { this.cancel(); } catch (IllegalStateException ignored) {}
    }

    private void updatePapiAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("BetonCompass"), () -> {
            this.cachedPrefix = PlaceholderAPI.setPlaceholders(player, mainConfig.getBarStart());
            this.cachedPostfix = PlaceholderAPI.setPlaceholders(player, mainConfig.getBarEnd());
        });
    }

    public void updateCompassLocations() {
        final PlayerData playerData = betonQuest.getPlayerDataStorage().get(profile);
        List<QuestCompass> found = new ArrayList<>();
        for (final Map.Entry<CompassIdentifier, QuestCompass> entry : betonQuest.getFeatureApi().getCompasses().entrySet()) {
            if (playerData.hasTag(entry.getKey().getTag())) found.add(entry.getValue());
        }
        synchronized (activeCompasses) {
            activeCompasses.clear();
            activeCompasses.addAll(found);
        }
    }

    @Override
    public void run() {
        if (!player.isOnline()) {
            this.deleteBossBar();
            return;
        }

        float targetYaw = player.getLocation().getYaw();
        float diff = targetYaw - interpolatedYaw;
        while (diff < -180) diff += 360;
        while (diff > 180) diff -= 360;

        if (Math.abs(diff) < 0.01 && logicTickCounter > 0) {
            logicTickCounter++;
            if (logicTickCounter >= mainConfig.getTicksUpdateCompass()) {
                updateCompassLocations();
                logicTickCounter = 0;
            }
            return;
        }

        interpolatedYaw += diff * mainConfig.getInterpolationSmoothness();

        if (++logicTickCounter >= mainConfig.getTicksUpdateCompass()) {
            updateCompassLocations();
            logicTickCounter = 0;
        }

        if (++papiTickCounter >= 20) {
            updatePapiAsync();
            papiTickCounter = 0;
        }

        render(interpolatedYaw);
    }

    private void render(float yaw) {
        final Location playerLoc = player.getLocation();
        if (playerLoc.getWorld() == null) return;

        if (yaw < 0) yaw += 360;
        final int currentYaw = ((int) yaw / 9) + 20;

        String[] displayArray = mainConfig.getOriginCompass().toArray(new String[0]);
        String selectedName = null;
        Location selectedLoc = null;
        boolean isCurrentTargetSelected = false;

        synchronized (activeCompasses) {
            for (final QuestCompass compass : activeCompasses) {
                Location compassLoc;
                try {
                    compassLoc = compass.location().getValue(profile);
                } catch (final QuestException e) { continue; }

                if (!playerLoc.getWorld().equals(compassLoc.getWorld())) continue;

                final int pointYaw = AngleUtil.computeAngle(player, compassLoc) / 9 + 20;
                final boolean isSelected = this.targetLocation != null && this.targetLocation.equals(compassLoc);

                if (pointYaw == currentYaw) {
                    try {
                        selectedName = LegacyComponentSerializer.legacySection().serialize(compass.names().asComponent(profile));
                        selectedLoc = compassLoc;
                        isCurrentTargetSelected = isSelected;
                    } catch (QuestException ignored) {}
                }
                setAt(displayArray, pointYaw, getIconOptimized(playerLoc, compassLoc, isSelected, pointYaw == currentYaw));
            }
        }

        StringBuilder builder = new StringBuilder(cachedPrefix);
        builder.append(mainConfig.getSymbolStart());
        for (int i = currentYaw - 10; i <= currentYaw + 10; i++) {
            if (i < 0 || i >= displayArray.length) continue;
            String part = displayArray[i];
            if (i == currentYaw) part = mainConfig.getReplacers().getOrDefault(part, part);
            builder.append(part);
        }
        builder.append(mainConfig.getSymbolEnd());
        builder.append(cachedPostfix);

        String finalTitle = builder.toString();
        if (!finalTitle.equals(lastCompassTitle)) {
            bossBarCompass.setTitle(finalTitle);
            lastCompassTitle = finalTitle;
        }

        if (selectedLoc != null) {
            handleMessageBar(selectedLoc, playerLoc, selectedName, isCurrentTargetSelected);
        } else if (bossBarMessage.isVisible()) {
            bossBarMessage.setVisible(false);
        }
    }

    private void handleMessageBar(Location loc, Location pLoc, String name, boolean selected) {
        String template = selected ? mainConfig.getTitleMessageSelected() : mainConfig.getTitleMessage();
        String msg = template.replace("{name}", name).replace("{distance}", String.valueOf(Math.round(loc.distance(pLoc))));
        String finalMsg = PlaceholderAPI.setPlaceholders(player, msg);
        if (!finalMsg.equals(lastMessageTitle)) {
            bossBarMessage.setTitle(finalMsg);
            lastMessageTitle = finalMsg;
        }
        if (!bossBarMessage.isVisible()) bossBarMessage.setVisible(true);
    }

    private String getIconOptimized(Location pLoc, Location cLoc, boolean isSelected, boolean isCenter) {
        double yDiff = cLoc.getY() - pLoc.getY();
        int type = 0;
        if (yDiff > mainConfig.getYDifferenceIcons()) type = 1;
        else if (-yDiff > mainConfig.getYDifferenceIcons()) type = 2;
        if (isCenter) type += 3;
        return mainConfig.getIconFromMatrix(isSelected, type);
    }

    private void setAt(String[] array, int index, String value) {
        if (index >= 0 && index < array.length) array[index] = value;
        if (index > 40 && index - 40 < array.length) array[index - 40] = value;
        if (index < 40 && index + 40 < array.length) array[index + 40] = value;
    }
}