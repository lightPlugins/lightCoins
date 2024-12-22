package io.lightstudios.coins.commands.virtual.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.VirtualResponse;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VirtualAddCommand implements LightCommand {

    @Override
    public List<String> getSubcommand() {
        return List.of("add", "give");
    }

    @Override
    public String getDescription() {
        return "Add virtual currency to a offline player";
    }

    @Override
    public String getSyntax() {
        return "/virtual add <player> <currency> <amount>";
    }

    @Override
    public int maxArgs() {
        return 4;
    }

    @Override
    public String getPermission() {
        return LightPermissions.VIRTUAL_ADD_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {

            if(args.length == 1) {
                return getSubcommand();
            }

            if(args.length == 2) {
                return Arrays.stream(Bukkit.getServer().getOfflinePlayers()).map(OfflinePlayer::getName).toList();
            }

            if(args.length == 3) {
                return LightCoins.instance.getVirtualCurrencyFiles().getYamlFiles().stream()
                        .map(file -> file.getName().replace(".yml", "")).toList();
            }

            return null;
        };
    }

    @Override
    public boolean performAsPlayer(Player player, String[] args) {

        if(args.length != 4) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().wrongSyntax().stream().map(str -> str
                                    .replace("#syntax#", getSyntax())
                            ).collect(Collectors.joining()));
            return false;
        }

        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(args[1]);

        if(offlinePlayer.getPlayer() == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().playerNotFound().stream().map(str -> str
                                    .replace("#player#", args[1])
                            ).collect(Collectors.joining()));
            return false;
        }

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getPlayerData(offlinePlayer);

        if(accountData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                    .replace("#info#", "Could not find player data")
                            ).collect(Collectors.joining()));
            return false;
        }

        VirtualData virtualData = accountData.getVirtualCurrencyByName(args[2]);

        if(virtualData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().virtualCurrencyNotFound().stream().map(str -> str
                                    .replace("#currency#", args[2])
                            ).collect(Collectors.joining()));
            return false;
        }

        BigDecimal amount = LightNumbers.parseMoney(args[3]);

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
                            LightCoins.instance.getMessageConfig().noNegativ());
            return false;
        }

        VirtualResponse response = virtualData.addBalance(amount);

        if(!response.transactionSuccess()) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(str -> str
                                    .replace("#info#", response.errorMessage)
                            ).collect(Collectors.joining()));
            return false;
        }

        LightCore.instance.getMessageSender().sendPlayerMessage(
                player,
                LightCoins.instance.getMessageConfig().prefix() +
                        LightCoins.instance.getMessageConfig().virtualCurrencyAdd().stream().map(str -> str
                                .replace("#amount#", LightNumbers.formatForMessages(amount, virtualData.getDecimalPlaces()))
                                .replace("#currency#", virtualData.getFormattedCurrencySymbol())
                                .replace("#player#", virtualData.getPlayerName())
                        ).collect(Collectors.joining()));


        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
