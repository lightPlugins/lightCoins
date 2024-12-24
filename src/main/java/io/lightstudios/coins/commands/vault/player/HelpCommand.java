package io.lightstudios.coins.commands.vault.player;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class HelpCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("help");
    }

    @Override
    public String getDescription() {
        return "Displays help for vault commands";
    }

    @Override
    public String getSyntax() {
        return "/coins help";
    }

    @Override
    public int maxArgs() {
        return 1;
    }

    @Override
    public String getPermission() {
        return "";
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
    public boolean performAsPlayer(Player player, String[] args) {

        if(args.length != 1) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().wrongSyntax().stream().map(str -> str
                                    .replace("#syntax#", getSyntax())
                            ).collect(Collectors.joining()));
            return false;
        }

        if(player.hasPermission(LightPermissions.COINS_HELP_ADMIN_COMMAND.getPerm())) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().helpCommandCoins());
            return false;
        }

        LightCore.instance.getMessageSender().sendPlayerMessage(
                player,
                LightCoins.instance.getMessageConfig().helpCommandPlayer());

        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
