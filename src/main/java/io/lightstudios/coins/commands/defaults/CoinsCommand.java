package io.lightstudios.coins.commands.defaults;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class CoinsCommand implements LightCommand {
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
        return "/coins show";
    }

    @Override
    public int maxArgs() {
        return 1;
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

        OfflinePlayer offlinePlayer = Bukkit.getPlayer(player.getUniqueId());

        if(offlinePlayer == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().playerNotFound().stream().map(str -> str
                                    .replace("#player#", args[1])
                            ).toList());
            return false;
        }

        PlayerData playerData = LightCoins.instance.getLightCoinsAPI().getPlayerData(offlinePlayer);

        if(playerData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                    .replace("#info#", "Could not find your player data")
                            ).toList());
            return false;
        }

        CoinsPlayer coinsPlayer = playerData.getCoinsPlayer();

        if(coinsPlayer == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                    .replace("#info#", "Could not found your coins data")
                            ).toList());
            return false;
        }

        String coins = coinsPlayer.getFormattedCoins();
        String currency = coinsPlayer.getFormattedCurrency();

        LightCore.instance.getMessageSender().sendPlayerMessage(
                player,
                LightCoins.instance.getMessageConfig().prefix() +
                LightCoins.instance.getMessageConfig().coinsShow().stream().map(str -> str
                        .replace("#coins#", coinsPlayer.getFormattedCoins())
                        .replace("#currency#", currency)
                ).collect(Collectors.joining()));

        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
