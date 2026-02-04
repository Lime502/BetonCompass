package dev.limedev.betoncompass.commands;

import dev.limedev.betoncompass.BetonCompass;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import java.util.Collections;
import java.util.List;

public class CommandsHandler implements CommandExecutor, TabCompleter {
    private final BetonCompass betonCompass;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public CommandsHandler(final BetonCompass betonCompass) { this.betonCompass = betonCompass; }

    @Override
    public boolean onCommand(@NotNull CommandSender s, @NotNull Command c, @NotNull String l, @NotNull String[] a) {
        if (!s.hasPermission("betoncompass.admin")) return true;
        if (a.length == 1 && a[0].equalsIgnoreCase("reload")) {
            betonCompass.reloadMainConfig();
            s.sendMessage(mm.deserialize("<gold>[BetonCompass] <yellow>Plugin reloaded"));
            return true;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender s, @NotNull Command c, @NotNull String a, @NotNull String[] args) {
        if (s.hasPermission("betoncompass.admin") && args.length == 1 && "reload".startsWith(args[0].toLowerCase())) return List.of("reload");
        return Collections.emptyList();
    }
}