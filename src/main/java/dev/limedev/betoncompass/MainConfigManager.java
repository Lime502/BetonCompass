package dev.limedev.betoncompass;

import lombok.Getter;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

@Getter
public class MainConfigManager {
    private final String north, northSelected, east, eastSelected, south, southSelected, west, westSelected;
    private final String fill, fillCenter, fillSelect, fillCenterSelect;
    private final String symbolStart, symbolEnd, titleMessage, titleMessageSelected, barStart, barEnd;
    private final BarColor barColor;
    private final BarStyle barStyle;
    private final int compassLocationsUpdateDelaySeconds, ticksUpdateCompass;
    private final double yDifferenceIcons;
    private final float interpolationSmoothness;
    private final List<String> originCompass;
    private final Map<String, String> replacers = new HashMap<>();
    private final String[][] iconMatrix;

    public MainConfigManager(JavaPlugin plugin, final FileConfiguration config) {
        this.north = get(config, "directions.north", "&e&lN");
        this.northSelected = get(config, "directions.north-selected", "&6&lN");
        this.east = get(config, "directions.east", "&e&lE");
        this.eastSelected = get(config, "directions.east-selected", "&6&lE");
        this.south = get(config, "directions.south", "&e&lS");
        this.southSelected = get(config, "directions.south-selected", "&6&lS");
        this.west = get(config, "directions.west", "&e&lW");
        this.westSelected = get(config, "directions.west-selected", "&6&lW");

        replacers.put(north, northSelected);
        replacers.put(east, eastSelected);
        replacers.put(south, southSelected);
        replacers.put(west, westSelected);

        this.fill = get(config, "appearance.fill", "&7═");
        this.fillSelect = get(config, "appearance.fill-select", "&f&l╩");
        this.fillCenter = get(config, "appearance.fill-center", "&7╪");
        this.fillCenterSelect = get(config, "appearance.fill-center-select", "&f&l╪");

        replacers.put(fill, fillSelect);
        replacers.put(fillCenter, fillCenterSelect);

        String compassTarget = get(config, "icons.default.target", "&f◇");
        String compassTargetSelected = get(config, "icons.default.target-selected", "&f&l◆");
        String compassTargetAbove = get(config, "icons.default.above", "&f△");
        String compassTargetSelectedAbove = get(config, "icons.default.above-selected", "&f&l▲");
        String compassTargetBelow = get(config, "icons.default.below", "&f▽");
        String compassTargetSelectedBelow = get(config, "icons.default.below-selected", "&f&l▼");

        String selectedCompassTarget = get(config, "icons.selected.target", "&a◇");
        String selectedCompassTargetSelected = get(config, "icons.selected.target-selected", "&a&l◆");
        String selectedCompassTargetAbove = get(config, "icons.selected.above", "&a△");
        String selectedCompassTargetSelectedAbove = get(config, "icons.selected.above-selected", "&a&l▲");
        String selectedCompassTargetBelow = get(config, "icons.selected.below", "&a▽");
        String selectedCompassTargetSelectedBelow = get(config, "icons.selected.below-selected", "&a&l▼");

        this.iconMatrix = new String[][]{
                {compassTarget, compassTargetAbove, compassTargetBelow, compassTargetSelected, compassTargetSelectedAbove, compassTargetSelectedBelow},
                {selectedCompassTarget, selectedCompassTargetAbove, selectedCompassTargetBelow, selectedCompassTargetSelected, selectedCompassTargetSelectedAbove, selectedCompassTargetSelectedBelow}
        };

        this.symbolStart = get(config, "appearance.symbol-start", "&e&l╠");
        this.symbolEnd = get(config, "appearance.symbol-end", "&e&l╣");
        this.titleMessage = get(config, "messages.title", "&6{name} &e{distance} м.");
        this.titleMessageSelected = get(config, "messages.title-selected", "&a{name} &2{distance} м.");

        this.barColor = parseEnum(BarColor.class, config.getString("appearance.bar-color", "WHITE"), BarColor.WHITE);
        this.barStyle = parseEnum(BarStyle.class, config.getString("appearance.bar-style", "SOLID"), BarStyle.SOLID);

        this.ticksUpdateCompass = config.getInt("settings.update-ticks", 2);
        this.compassLocationsUpdateDelaySeconds = config.getInt("settings.cache-seconds", 2);
        this.yDifferenceIcons = config.getDouble("settings.y-threshold", 10.0);
        this.interpolationSmoothness = (float) config.getDouble("settings.interpolation", 0.35);

        this.originCompass = Collections.unmodifiableList(formatOriginCompass());

        this.barStart = color(config.getString("messages.prefix-text", ""));
        this.barEnd = color(config.getString("messages.postfix-text", ""));
    }

    public String getIconFromMatrix(boolean isSelected, int type) {
        return iconMatrix[isSelected ? 1 : 0][type];
    }

    private String get(FileConfiguration config, String path, String def) {
        return color(config.getString(path, def));
    }

    private String color(String val) {
        if (val == null || val.isEmpty()) return "";

        String t = val.replace('&', '§');

        if (val.contains("<") && val.contains(">")) {
            return LegacyComponentSerializer.legacySection().serialize(
                    MiniMessage.miniMessage().deserialize(val)
            );
        }

        return t;
    }

    private List<String> formatOriginCompass() {
        List<String> s = new ArrayList<>(80);
        String[] p = {north, east, south, west, north, east, south, west};
        for (String d : p) {
            s.add(d);
            for (int i = 0; i < 4; i++) s.add(fill);
            s.add(fillCenter);
            for (int i = 0; i < 4; i++) s.add(fill);
        }
        return s;
    }

    private <E extends Enum<E>> E parseEnum(Class<E> c, String v, E d) {
        if (v == null) return d;
        try {
            return Enum.valueOf(c, v.toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return d;
        }
    }
}