package io.lightstudios.coins.commands.vault.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ShowCoinsCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("show");
    }

    @Override
    public String getDescription() {
        return "Shows the amount of coins you have";
    }

    @Override
    public String getSyntax() {
        return "/coins show <playername>";
    }

    @Override
    public int maxArgs() {
        return -1;
    }

    @Override
    public String getPermission() {
        return LightPermissions.COINS_COMMAND.getPerm();
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

        if(args.length > 2 || args.length == 1) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().wrongSyntax().stream().map(str -> str
                                    .replace("#syntax#", getSyntax())
                            ).collect(Collectors.joining()));
            return false;
        }

        if(args.length == 0) {
            AccountData playerData = LightCoins.instance.getLightCoinsAPI().getAccountData(player);

            if(playerData == null) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                        .replace("#info#", "Could not find your player data")
                                ).collect(Collectors.joining()));
                return false;
            }

            CoinsData coinsPlayer = playerData.getCoinsData();

            String coins = coinsPlayer.getFormattedCoins();
            String currency = coinsPlayer.getFormattedCurrency();

            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().coinsShow().stream().map(str -> str
                                    .replace("#coins#", coins)
                                    .replace("#currency#", currency)
                            ).collect(Collectors.joining()));

            return false;
        }

        if(!player.hasPermission(LightPermissions.COINS_COMMAND.getPerm())) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().noPermission().stream().map(str -> str
                                    .replace("#permission#", getSyntax())
                            ).collect(Collectors.joining()));
            return false;
        }

        OfflinePlayer offlinePlayer = Bukkit.getPlayer(args[1]);

        if(offlinePlayer == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().playerNotFound().stream().map(str -> str
                                    .replace("#player#", args[1])
                            ).collect(Collectors.joining()));
            return false;
        }

        AccountData playerData = LightCoins.instance.getLightCoinsAPI().getAccountData(offlinePlayer);

        if(playerData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                    .replace("#info#", "Could not find your player data")
                            ).collect(Collectors.joining()));
            return false;
        }

        CoinsData coinsPlayer = playerData.getCoinsData();

        if(coinsPlayer == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                    .replace("#info#", "Could not found your coins data")
                            ).collect(Collectors.joining()));
            return false;
        }

        String coins = coinsPlayer.getFormattedCoins();
        String currency = coinsPlayer.getFormattedCurrency();

        LightCore.instance.getMessageSender().sendPlayerMessage(
                player,
                LightCoins.instance.getMessageConfig().prefix() +
                LightCoins.instance.getMessageConfig().coinsShowTarget().stream().map(str -> str
                        .replace("#coins#", coins)
                        .replace("#player#", offlinePlayer.getName())
                        .replace("#currency#", currency)
                ).collect(Collectors.joining()));

        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
