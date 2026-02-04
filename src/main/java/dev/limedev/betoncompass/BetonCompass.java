package dev.limedev.betoncompass;

import dev.limedev.betoncompass.commands.CommandsHandler;
import dev.limedev.betoncompass.controllers.PlayerCompass;
import dev.limedev.betoncompass.handlers.PlayerJoinHandler;
import dev.limedev.betoncompass.handlers.PlayerQuitHandler;
import dev.limedev.betoncompass.handlers.QuestCompassTargetChangeHandler;
import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.betonquest.betonquest.BetonQuest;
import org.betonquest.betonquest.api.logger.BetonQuestLogger;
import org.betonquest.betonquest.api.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class BetonCompass extends JavaPlugin {

    private final Map<UUID, PlayerCompass> compasses = new ConcurrentHashMap<>();
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final LegacyComponentSerializer legacySerializer = LegacyComponentSerializer.builder()
            .character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    @Getter
    private MainConfigManager mainConfig;
    private BetonQuest betonQuest;

    @Override
    public void onEnable() {
        printBanner();
        if (!setupBetonQuest()) {
            sendConsole("<gold>[BetonCompass] <red>BetonQuest not found! Disabling...");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        reloadMainConfig();
        setupCommands();
        registerEvents();
        for (Player player : Bukkit.getOnlinePlayers()) {
            getOrCreateCompass(player);
        }
    }

    @Override
    public void onDisable() {
        compasses.values().forEach(PlayerCompass::deleteBossBar);
        compasses.clear();
    }

    private boolean setupBetonQuest() {
        try {
            betonQuest = BetonQuest.getInstance();
            return betonQuest != null;
        } catch (NoClassDefFoundError | Exception e) {
            return false;
        }
    }

    private void setupCommands() {
        var handler = new CommandsHandler(this);
        Optional.ofNullable(getCommand("customcompass")).ifPresent(cmd -> {
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        });
    }

    private void registerEvents() {
        var pm = Bukkit.getPluginManager();
        pm.registerEvents(new PlayerQuitHandler(this), this);
        pm.registerEvents(new QuestCompassTargetChangeHandler(this), this);
        pm.registerEvents(new PlayerJoinHandler(this), this);
    }

    public void reloadMainConfig() {
        compasses.values().forEach(PlayerCompass::deleteBossBar);
        compasses.clear();
        reloadConfig();
        mainConfig = new MainConfigManager(this, getConfig());
        for (Player player : Bukkit.getOnlinePlayers()) {
            getOrCreateCompass(player);
        }
    }

    public PlayerCompass getOrCreateCompass(@NotNull Player player) {
        return compasses.computeIfAbsent(player.getUniqueId(), uuid -> {
            BetonQuestLogger logger = betonQuest.getLoggerFactory().create(this, "PlayerCompass");
            PlayerCompass pc = new PlayerCompass(logger, betonQuest, mainConfig, player);
            pc.runTaskTimer(this, 0L, 1L);
            return pc;
        });
    }

    public Optional<PlayerCompass> getCompass(Profile profile) {
        return Optional.ofNullable(compasses.get(profile.getPlayerUUID()));
    }

    public Optional<PlayerCompass> getCompass(Player player) {
        return Optional.ofNullable(compasses.get(player.getUniqueId()));
    }

    public void deleteCompass(Player player) {
        Optional.ofNullable(compasses.remove(player.getUniqueId())).ifPresent(PlayerCompass::deleteBossBar);
    }

    private void sendConsole(String message) {
        Bukkit.getConsoleSender().sendMessage(legacySerializer.serialize(mm.deserialize(message)));
    }

    private void printBanner() {
        String[] art = {
                "§e  ____       _               §6____                                      ",
                "§e | __ )  ___| |_ ___  _ __  §6/ ___|___  _ __ ___  _ __   __ _ ___ ___ ",
                "§e |  _ \\ / _ \\ __/ _ \\| '_ \\§6| |   / _ \\| '_ ` _ \\| '_ \\ / _` / __/ __|",
                "§e | |_) |  __/ |_ (_) | | | §6| |__| (_) | | | | | | |_) | (_| \\__ \\__ \\",
                "§e |____/ \\___|\\__\\___/|_| |_|§6 \\____\\___/|_| |_| |_| .__/ \\__,_|___/___/",
                "§6                                                |_|                "
        };
        for (String line : art) Bukkit.getConsoleSender().sendMessage(line);
    }
}