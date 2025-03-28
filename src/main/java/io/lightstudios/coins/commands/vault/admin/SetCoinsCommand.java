package io.lightstudios.coins.commands.vault.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.proxy.messaging.SendProxyRequest;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.interfaces.LightCommand;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SetCoinsCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("set");
    }

    @Override
    public String getDescription() {
        return "Set the coins of a player";
    }

    @Override
    public String getSyntax() {
        return "/coins set <player> <amount>";
    }

    @Override
    public int maxArgs() {
        return 3;
    }

    @Override
    public String getPermission() {
        return LightPermissions.COINS_SET_COMMAND.getPerm();
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
                    return Arrays.stream(Bukkit.getServer().getOfflinePlayers()).map(OfflinePlayer::getName).toList();
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
        if(args.length != 3) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().wrongSyntax().stream().map(str -> str
                                    .replace("#syntax#", getSyntax())
                            ).collect(Collectors.joining()));
            return false;
        }

        String targetName = args[1];

        BigDecimal amount = LightNumbers.parseMoney(args[2]);

        if(amount == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            String.join("", LightCoins.instance.getMessageConfig().noNumber()));
            return false;
        }

        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            String.join("", LightCoins.instance.getMessageConfig().noNegativ()));
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
                                        .replace("#player#", targetName)
                                ).collect(Collectors.joining()));
                return false;
            }


            CoinsData coinsPlayer = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(target.getUniqueId()).join();

            if(coinsPlayer == null) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                        .replace("#info#", "Could not find data from: " + targetName)
                                ).collect(Collectors.joining()));
                return false;
            }

            EconomyResponse response = coinsPlayer.setCoins(amount);

            if(response.transactionSuccess()) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        List.of(
                                LightCoins.instance.getMessageConfig().prefix() +
                                        LightCoins.instance.getMessageConfig().coinsSet().stream().map(str -> str
                                                .replace("#coins#", LightNumbers.formatForMessages(amount,
                                                        LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()))
                                                .replace("#currency#", coinsPlayer.getFormattedCurrency())
                                                .replace("#player#", coinsPlayer.getName())
                                        ).collect(Collectors.joining())
                        )
                );
                return true;
            } else {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                        .replace("#info#", response.errorMessage)
                                ).collect(Collectors.joining()));
                return false;
            }
        }

        AccountData playerData = LightCoins.instance.getLightCoinsAPI().getAccountData(targetName);
        if(playerData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                    .replace("#info#", "Could not find player data")
                            ).collect(Collectors.joining()));
            return false;
        }

        CoinsData coinsPlayer = playerData.getCoinsData();
        EconomyResponse response = coinsPlayer.setCoins(amount);

        if(response.transactionSuccess()) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    List.of(
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().coinsSet().stream().map(str -> str
                                            .replace("#coins#", LightNumbers.formatForMessages(amount,
                                                    LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()))
                                            .replace("#currency#", coinsPlayer.getFormattedCurrency())
                                            .replace("#player#", coinsPlayer.getName())
                                    ).collect(Collectors.joining())
                    )
            );
            return true;
        } else {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                    .replace("#info#", response.errorMessage)
                            ).collect(Collectors.joining()));
            return false;
        }
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
