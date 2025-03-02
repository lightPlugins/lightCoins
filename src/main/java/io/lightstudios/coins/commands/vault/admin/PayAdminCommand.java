package io.lightstudios.coins.commands.vault.admin;

import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class PayAdminCommand implements LightCommand {

    @Override
    public List<String> getSubcommand() {
        return List.of("admin");
    }

    @Override
    public String getDescription() {
        return "Opens the management menu for pending transactions";
    }

    @Override
    public String getSyntax() {
        return "/pay admin";
    }

    @Override
    public int maxArgs() {
        return 2;
    }

    @Override
    public String getPermission() {
        return LightPermissions.PAY_ADMIN_MENU.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {
            if (args.length == 1) {
                return getSubcommand();
            }
            return null;
        };
    }

    @Override
    public boolean performAsPlayer(Player player, String[] strings) {
        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
