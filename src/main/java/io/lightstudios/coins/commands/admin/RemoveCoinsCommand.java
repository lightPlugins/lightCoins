package io.lightstudios.coins.commands.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.interfaces.LightCommand;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class RemoveCoinsCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("remove", "take", "withdraw");
    }

    @Override
    public String getDescription() {
        return "Remove coins from a player";
    }

    @Override
    public String getSyntax() {
        return "/coins remove <player> <amount>";
    }

    @Override
    public int maxArgs() {
        return 3;
    }

    @Override
    public String getPermission() {
        return LightPermissions.COINS_REMOVE_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {

            if(args.length == 1) {
                return getSubcommand();
            }

            if(args.length == 2) {
                return Bukkit.getServer().getOnlinePlayers().stream().map(Player::getName).toList();
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

        OfflinePlayer target = Bukkit.getServer().getPlayer(args[1]);

        if(target == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().playerNotFound().stream().map(str -> str
                                    .replace("#player#", args[1])
                            ).collect(Collectors.joining()));
            return false;
        }

        PlayerData playerData = LightCoins.instance.getLightCoinsAPI().getPlayerData(target);
        if(playerData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                    .replace("#info#", "Could not find player data")
                            ).collect(Collectors.joining()));
            return false;
        }

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

        CoinsPlayer coinsPlayer = playerData.getCoinsPlayer();
        EconomyResponse response = coinsPlayer.removeCoins(amount);

        if(response.transactionSuccess()) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().coinsRemove().stream().map(str -> str
                                    .replace("#coins#", LightNumbers.formatForMessages(amount, 2))
                                    .replace("#currency#", coinsPlayer.getFormattedCurrency())
                                    .replace("#player#", target.getName())
                            ).collect(Collectors.joining()));
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
