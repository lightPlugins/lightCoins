package io.lightstudios.coins.commands.virtual.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.VirtualResponse;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class VirtualRemoveCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("remove");
    }

    @Override
    public String getDescription() {
        return "Remove virtual currency from a player";
    }

    @Override
    public String getSyntax() {
        return "/virtual remove <player> <amount>";
    }

    @Override
    public int maxArgs() {
        return 4;
    }

    @Override
    public String getPermission() {
        return LightPermissions.VIRTUAL_REMOVE_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {

            if(args.length == 1) {
                return getSubcommand();
            }

            if(args.length == 2) {
                if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql")) {
                    return Arrays.stream(Bukkit.getServer().getOfflinePlayers()).map(OfflinePlayer::getName).toList();
                } else {
                    return LightCoins.instance.getLightCoinsAPI().getAccountDataPlayerNames();
                }
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

        String targetName = args[1];
        String targetCurrency = args[2];

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

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql")) {

            OfflinePlayer target = Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                    .filter(offlinePlayer -> offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(targetName))
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

            List<VirtualData> virtualDataList = LightCoins.instance.getVirtualDataTable().readVirtualData().join();
            VirtualData virtualData = virtualDataList.stream()
                    .filter(data -> data.getPlayerUUID().equals(target.getUniqueId()) && data.getCurrencyName().equalsIgnoreCase(targetCurrency))
                    .findFirst()
                    .orElse(null);

            if(virtualData == null) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().virtualCurrencyNotFound().stream().map(str -> str
                                        .replace("#currency#", targetCurrency)
                                ).collect(Collectors.joining()));
                return false;
            }

            VirtualResponse response = virtualData.removeBalance(amount);

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
                            LightCoins.instance.getMessageConfig().virtualCurrencyRemove().stream().map(str -> str
                                    .replace("#amount#", LightNumbers.formatForMessages(amount, virtualData.getDecimalPlaces()))
                                    .replace("#currency#", virtualData.getFormattedCurrencySymbol())
                                    .replace("#player#", virtualData.getPlayerName())
                            ).collect(Collectors.joining()));


            return true;
        }

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(targetName);

        if(accountData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().playerNotFound().stream().map(str -> str
                                    .replace("#player#", targetName)
                            ).collect(Collectors.joining()));
            return false;
        }

        VirtualData virtualData = accountData.getVirtualCurrencyByName(targetCurrency);

        if(virtualData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().virtualCurrencyNotFound().stream().map(str -> str
                                    .replace("#currency#", args[2])
                            ).collect(Collectors.joining()));
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
                        LightCoins.instance.getMessageConfig().virtualCurrencyRemove().stream().map(str -> str
                                .replace("#amount#", LightNumbers.formatForMessages(amount, virtualData.getDecimalPlaces()))
                                .replace("#currency#", virtualData.getFormattedCurrencySymbol())
                                .replace("#player#", virtualData.getPlayerName())
                        ).collect(Collectors.joining()));


        return true;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] args) {
        if(args.length != 4) {
            LightCoins.instance.getConsolePrinter().printError("Wrong syntax. Use: " + getSyntax());
            return false;
        }

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql")) {

            OfflinePlayer target = Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                    .filter(offlinePlayer -> offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(args[1]))
                    .findFirst()
                    .orElse(null);

            if(target == null) {
                LightCoins.instance.getConsolePrinter().printError("Player not found: " + args[1]);
                return false;
            }

            List<VirtualData> virtualDataList = LightCoins.instance.getVirtualDataTable().readVirtualData().join();
            VirtualData virtualData = virtualDataList.stream()
                    .filter(data -> data.getPlayerUUID().equals(target.getUniqueId()) && data.getCurrencyName().equalsIgnoreCase(args[2]))
                    .findFirst()
                    .orElse(null);

            if(virtualData == null) {
                LightCoins.instance.getConsolePrinter().printError("Virtual currency not found: " + args[2]);
                return false;
            }

            BigDecimal amount = LightNumbers.parseMoney(args[3]);

            if(amount == null) {
                LightCoins.instance.getConsolePrinter().printError("Amount is not a number: " + args[3]);
                return false;
            }

            if(amount.compareTo(BigDecimal.ZERO) <= 0) {
                LightCoins.instance.getConsolePrinter().printError("Amount is negative: " + args[3]);
                return false;
            }

            VirtualResponse response = virtualData.addBalance(amount);

            if(!response.transactionSuccess()) {
                LightCoins.instance.getConsolePrinter().printError("Transaction failed: " + response.errorMessage);
                return false;
            }

            LightCoins.instance.getConsolePrinter().printInfo("Removed " + amount + " " + virtualData.getCurrencyName() + " to " + virtualData.getPlayerName());

            return false;
        }

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(args[1]);

        if(accountData == null) {
            LightCoins.instance.getConsolePrinter().printError("Player not found: " + args[1]);
            return false;
        }

        VirtualData virtualData = accountData.getVirtualCurrencyByName(args[2]);

        if(virtualData == null) {
            LightCoins.instance.getConsolePrinter().printError("Virtual currency not found: " + args[2]);
            return false;
        }

        BigDecimal amount = LightNumbers.parseMoney(args[3]);

        if(amount == null) {
            LightCoins.instance.getConsolePrinter().printError("Amount is not a number: " + args[3]);
            return false;
        }

        if(amount.compareTo(BigDecimal.ZERO) <= 0) {
            LightCoins.instance.getConsolePrinter().printError("Amount is negative: " + args[3]);
            return false;
        }

        VirtualResponse response = virtualData.removeBalance(amount);

        if(!response.transactionSuccess()) {
            LightCoins.instance.getConsolePrinter().printError("Transaction failed: " + response.errorMessage);
            return false;
        }

        LightCoins.instance.getConsolePrinter().printInfo("Removed " + amount + " " + virtualData.getCurrencyName() + " from " + virtualData.getPlayerName());

        return false;
    }
}
