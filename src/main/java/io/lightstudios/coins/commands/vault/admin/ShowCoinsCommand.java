package io.lightstudios.coins.commands.vault.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.data.type.Light;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
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
        return LightPermissions.COINS_SHOW_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {
            if(args.length == 1) {
                return getSubcommand();
            }

            if(args.length == 2) {
                if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                        LightCore.instance.getSettings().multiServerEnabled()) {
                    // only support offline players from the target server !
                    return Arrays.stream(Bukkit.getServer().getOfflinePlayers()).map(OfflinePlayer::getName).collect(Collectors.toList());
                } else {
                    // support all players from the network
                    return LightCoins.instance.getLightCoinsAPI().getAccountDataPlayerNames();
                }
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
            if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                    LightCore.instance.getSettings().multiServerEnabled()) {
                CoinsData dataFromDatabase = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(player.getUniqueId()).join();

                if(dataFromDatabase == null) {
                    LightCore.instance.getMessageSender().sendPlayerMessage(
                            player,
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                            .replace("#info#", "Could not find your player data")
                                    ).collect(Collectors.joining()));
                    return false;
                }

                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().coinsShow().stream().map(str -> str
                                        .replace("#coins#", dataFromDatabase.getFormattedCoins())
                                        .replace("#currency#", dataFromDatabase.getFormattedCurrency())
                                ).collect(Collectors.joining()));

                return false;
            }

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

        if(!player.hasPermission(LightPermissions.COINS_SHOW_COMMAND.getPerm())) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().noPermission().stream().map(str -> str
                                    .replace("#permission#", getSyntax())
                            ).collect(Collectors.joining()));
            return false;
        }

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {

            OfflinePlayer target = Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                    .filter(offlinePlayer -> offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(args[1]))
                    .findFirst()
                    .orElse(null);


            if(target == null) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().playerNotFound().stream().map(str -> str
                                        .replace("#player#", args[1])
                                ).collect(Collectors.joining()));
                return false;
            }

            CoinsData dataFromDatabase = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(target.getUniqueId()).join();

            if(dataFromDatabase == null) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                        .replace("#info#", "Could not find " + args[0] + "´s data")
                                ).collect(Collectors.joining()));
                return false;
            }

            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().coinsShowTarget().stream().map(str -> str
                                    .replace("#coins#", dataFromDatabase.getFormattedCoins())
                                    .replace("#player#", dataFromDatabase.getName())
                                    .replace("#currency#", dataFromDatabase.getFormattedCurrency())
                            ).collect(Collectors.joining()));

            return false;
        }

        AccountData targetData = LightCoins.instance.getLightCoinsAPI().getAccountData(args[1]);

        if(targetData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().playerNotFound().stream().map(str -> str
                                    .replace("#player#", args[1])
                            ).collect(Collectors.joining()));
            return false;
        }

        CoinsData coinsPlayer = targetData.getCoinsData();

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
                        .replace("#player#", coinsPlayer.getName())
                        .replace("#currency#", currency)
                ).collect(Collectors.joining()));

        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] args) {

        if(args.length != 2) {
            LightCoins.instance.getConsolePrinter().printError("Wrong syntax! Please use: " + getSyntax());
            return false;
        }

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {

            OfflinePlayer target = Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                    .filter(offlinePlayer -> offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(args[1]))
                    .findFirst()
                    .orElse(null);


            if(target == null) {
                LightCoins.instance.getConsolePrinter().printError("Could not find player " + args[1]);
                return false;
            }

            CoinsData dataFromDatabase = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(target.getUniqueId()).join();

            if(dataFromDatabase == null) {
                LightCoins.instance.getConsolePrinter().printError("Could not find " + args[0] + "´s data");
                return false;
            }

            LightCoins.instance.getConsolePrinter().printInfo(
                    "Player " + dataFromDatabase.getName() + " has " + dataFromDatabase.getFormattedCoins() +
                            " " + dataFromDatabase.getFormattedCurrency());

            return false;
        }

        AccountData targetData = LightCoins.instance.getLightCoinsAPI().getAccountData(args[1]);

        if(targetData == null) {
            LightCoins.instance.getConsolePrinter().printError("Could not find player " + args[1]);
            return false;
        }

        CoinsData coinsPlayer = targetData.getCoinsData();

        if(coinsPlayer == null) {
            LightCoins.instance.getConsolePrinter().printError("Could not found your coins data");
            return false;
        }

        LightCoins.instance.getConsolePrinter().printInfo(
                "Player " + coinsPlayer.getName() + " has " + coinsPlayer.getFormattedCoins() +
                        " " + coinsPlayer.getFormattedCurrency());

        return false;
    }
}
