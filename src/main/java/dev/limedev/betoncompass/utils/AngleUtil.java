package dev.limedev.betoncompass.utils;

import org.bukkit.Location;
import org.bukkit.entity.Player;

public class AngleUtil {
    private static final double RAD_TO_DEG = 180.0 / Math.PI;

    public static int computeAngle(Player player, Location target) {
        Location origin = player.getLocation();
        double dx = target.getX() - origin.getX();
        double dz = target.getZ() - origin.getZ();
        double angle = Math.atan2(-dx, dz) * RAD_TO_DEG;
        if (angle < 0) angle += 360;
        return (int) Math.round(angle);
    }
}
