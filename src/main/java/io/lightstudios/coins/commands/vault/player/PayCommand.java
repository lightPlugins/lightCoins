package io.lightstudios.coins.commands.vault.player;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.proxy.messaging.SendProxyRequest;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.LightTimers;
import io.lightstudios.core.util.interfaces.LightCommand;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class PayCommand implements LightCommand {

    private final List<Player> cooldown = new ArrayList<>();

    @Override
    public List<String> getSubcommand() {
        return List.of();
    }

    @Override
    public String getDescription() {
        return "Send coins to another player";
    }

    @Override
    public String getSyntax() {
        return "/pay <player> <amount>";
    }

    @Override
    public int maxArgs() {
        return 2;
    }

    @Override
    public String getPermission() {
        return LightPermissions.PAY_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (commandSender, command, alias, args) -> {
            if (args.length == 1) {
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

        if(args.length != 2) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().wrongSyntax().stream().map(s ->
                                    s.replace("#syntax#", getSyntax())
                            ).collect(Collectors.joining()));
            return false;
        }

        String targetName = args[0];

        if(targetName.equalsIgnoreCase(player.getName())) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().payNotYourself().stream().map(s ->
                                    s.replace("#player#", player.getName())
                            ).collect(Collectors.joining()));
            return false;
        }

        if (cooldown.contains(player)) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().payCooldown().stream().map(s ->
                                    s.replace("#time#", "5")
                            ).collect(Collectors.joining()));
            return false;
        }

        BigDecimal amount = LightNumbers.parseMoney(args[1]);

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

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {

            CoinsData coinsPlayer = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(player.getUniqueId()).join();

            OfflinePlayer target = Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                    .filter(offlinePlayer -> offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(targetName))
                    .findFirst()
                    .orElse(null);

            if(target == null) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().payOnlyOnlinePlayer().stream().map(s ->
                                        s.replace("#target#", targetName)
                                ).collect(Collectors.joining()));
                return false;
            }

            CoinsData coinsTarget = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(target.getUniqueId()).join();

            if(coinsPlayer == null) {
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(s ->
                                        s.replace("#info#", "Could not find account data for: " + targetName)
                                ).collect(Collectors.joining()));
                return false;
            }

            EconomyResponse playerResponse = coinsPlayer.removeCoins(amount);
            EconomyResponse targetResponse = coinsTarget.addCoins(amount);

            if(playerResponse.transactionSuccess() && targetResponse.transactionSuccess()) {

                int cooldownTime = LightCoins.instance.getSettingsConfig().payCommandCooldown();

                if(cooldownTime != -1) {
                    cooldown.add(player);
                    LightTimers.doSync((task) -> cooldown.remove(player), cooldownTime * 20L);
                }

                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().pay().stream().map(s -> s
                                        .replace("#coins#", LightNumbers.formatForMessages(amount,
                                                LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()))
                                        .replace("#currency#", amount.compareTo(BigDecimal.ONE) == 0 ?
                                                coinsPlayer.getNameSingular() : coinsPlayer.getNamePlural())
                                        .replace("#target#", targetName)
                                ).collect(Collectors.joining()));


                if(target.isOnline()) {
                    LightCore.instance.getMessageSender().sendPlayerMessage(
                            target.getPlayer(),
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().payTarget().stream().map(s -> s
                                            .replace("#coins#", LightNumbers.formatForMessages(amount,
                                                    LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()))
                                            .replace("#currency#", amount.compareTo(BigDecimal.ONE) == 0 ?
                                                    coinsPlayer.getNameSingular() : coinsPlayer.getNamePlural())
                                            .replace("#target#", player.getName())
                                    ).collect(Collectors.joining()));

                    return false;
                } else {
                    SendProxyRequest.sendMessageToPlayer(player, target.getUniqueId(), LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().payTarget().stream().map(s -> s
                                    .replace("#coins#", LightNumbers.formatForMessages(amount, 2))
                                    .replace("#currency#", amount.compareTo(BigDecimal.ONE) == 0 ?
                                            coinsPlayer.getNameSingular() : coinsPlayer.getNamePlural())
                                    .replace("#target#", player.getName())
                            ).collect(Collectors.joining()));
                }
                return false;
            }

            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(s ->
                                    s.replace("#info#", playerResponse.errorMessage + " " + targetResponse.errorMessage)
                            ).collect(Collectors.joining()));

            return false;
        }

        List<String> availableAccounts = LightCoins.instance.getLightCoinsAPI().getAccountDataPlayerNames();

        if(!availableAccounts.contains(targetName)) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().playerNotFound().stream().map(s ->
                                    s.replace("#player#", targetName)
                            ).collect(Collectors.joining()));
        }

        AccountData playerData = LightCoins.instance.getLightCoinsAPI().getAccountData(player);
        AccountData targetData = LightCoins.instance.getLightCoinsAPI().getAccountData(targetName);

        if(playerData == null || targetData == null) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(s ->
                                    s.replace("#info#", "Could not find account data for target: " + targetName)
                            ).collect(Collectors.joining()));
            return false;
        }

        CoinsData coinsPlayer = playerData.getCoinsData();
        CoinsData targetCoinsPlayer = targetData.getCoinsData();

        EconomyResponse playerResponse = coinsPlayer.removeCoins(amount);


        Player target = Bukkit.getServer().getPlayer(targetName);
        UUID targetUUID = target != null ? target.getUniqueId() : targetData.getUuid();

        if(playerResponse.transactionSuccess()) {
            // first remove the coins from the player, then add them to the target
            EconomyResponse targetResponse = targetCoinsPlayer.addCoins(amount);

            if(targetResponse.transactionSuccess()) {

                int cooldownTime = LightCoins.instance.getSettingsConfig().payCommandCooldown();

                if(cooldownTime != -1) {
                    cooldown.add(player);
                    LightTimers.doSync((task) -> cooldown.remove(player), cooldownTime * 20L);
                }

                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().pay().stream().map(s -> s
                                        .replace("#coins#", LightNumbers.formatForMessages(amount,
                                                LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()))
                                        .replace("#currency#", amount.compareTo(BigDecimal.ONE) == 0 ?
                                                coinsPlayer.getNameSingular() : coinsPlayer.getNamePlural())
                                        .replace("#target#", targetName)
                                ).collect(Collectors.joining()));


                if(target != null) {
                    LightCore.instance.getMessageSender().sendPlayerMessage(
                            target,
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().payTarget().stream().map(s -> s
                                            .replace("#coins#", LightNumbers.formatForMessages(amount,
                                                    LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()))
                                            .replace("#currency#", amount.compareTo(BigDecimal.ONE) == 0 ?
                                                    coinsPlayer.getNameSingular() : coinsPlayer.getNamePlural())
                                            .replace("#target#", player.getName())
                                    ).collect(Collectors.joining()));
                    return true;
                } else {
                    // check for Velocity compatibility
                    // search for the Proxy player and send them a message
                    SendProxyRequest.sendMessageToPlayer(player, targetUUID, LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().payTarget().stream().map(s -> s
                                    .replace("#coins#", LightNumbers.formatForMessages(amount, 2))
                                    .replace("#currency#", amount.compareTo(BigDecimal.ONE) == 0 ?
                                            coinsPlayer.getNameSingular() : coinsPlayer.getNamePlural())
                                    .replace("#target#", player.getName())
                            ).collect(Collectors.joining()));
                }
                return false;
            } else {

                EconomyResponse transferBack = coinsPlayer.addCoins(amount);

                if(transferBack.transactionSuccess()) {
                    LightCore.instance.getMessageSender().sendPlayerMessage(
                            player,
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(s ->
                                            s.replace("#info#", targetResponse.errorMessage)
                                    ).collect(Collectors.joining()));

                    return false;
                }

                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "Could not transfer the coins back to the player after a failed transaction",
                        "via /pay command !",
                        "Player: §4" + player.getName(),
                        "Amount: §4" + amount,
                        "Error: §4" + targetResponse.errorMessage
                ));
            }
        } else {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().somethingWentWrong().stream().map(s ->
                                    s.replace("#info#", playerResponse.errorMessage)
                            ).collect(Collectors.joining()));
        }

        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        LightCoins.instance.getConsolePrinter().printError("This command can only be executed by a player");
        return false;
    }
}
