package io.lightstudios.coins.commands.vault.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;

public class ReloadCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("reload");
    }

    @Override
    public String getDescription() {
        return "Reloads the plugin configuration";
    }

    @Override
    public String getSyntax() {
        return "/coins reload";
    }

    @Override
    public int maxArgs() {
        return 1;
    }

    @Override
    public String getPermission() {
        return LightPermissions.RELOAD_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {
            if(args.length == 1) {
                return getSubcommand();
            }
            return null;
        };
    }

    @Override
    public boolean performAsPlayer(Player player, String[] strings) {

        LightCoins.instance.loadDefaults();

        LightCore.instance.getMessageSender().sendPlayerMessage(
                player,
                LightCoins.instance.getMessageConfig().prefix() +
                        String.join("", LightCoins.instance.getMessageConfig().reloadSuccess()));

        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {

        LightCoins.instance.loadDefaults();
        LightCoins.instance.getConsolePrinter().printInfo("Plugin configuration reloaded");
        return false;
    }
}
