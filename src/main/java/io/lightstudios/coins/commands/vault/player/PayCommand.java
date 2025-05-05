package io.lightstudios.coins.commands.vault.player;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.types.EconomyReason;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.coins.title.EconomyTitle;
import io.lightstudios.coins.title.EconomyTitleType;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.proxy.messaging.sender.SendProxyRequest;
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
import java.util.*;
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
                return LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                        LightCore.instance.getSettings().multiServerEnabled()
                        ? Arrays.stream(Bukkit.getServer().getOfflinePlayers()).map(OfflinePlayer::getName).toList()
                        : LightCoins.instance.getLightCoinsAPI().getAccountDataPlayerNames();
            }
            return null;
        };
    }

    @Override
    public boolean performAsPlayer(Player player, String[] args) {
        if (!validateArguments(player, args)) return false;

        String targetName = args[0];
        BigDecimal amount = LightNumbers.parseMoney(args[1]);

        if (!validateTransaction(player, targetName, amount)) return false;

        if (isMultiServerEnabled()) {
            return handleMultiServerTransaction(player, targetName, amount);
        } else {
            return handleSingleServerTransaction(player, targetName, amount);
        }
    }

    private boolean validateArguments(Player player, String[] args) {
        if (args.length != 2) {
            sendMessage(player, LightCoins.instance.getMessageConfig().wrongSyntax(), "#syntax#", getSyntax());
            return false;
        }
        return true;
    }

    private boolean validateTransaction(Player player, String targetName, BigDecimal amount) {
        if (targetName.equalsIgnoreCase(player.getName())) {
            sendMessage(player, LightCoins.instance.getMessageConfig().payNotYourself());
            return false;
        }

        if (cooldown.contains(player)) {
            sendMessage(player, LightCoins.instance.getMessageConfig().payCooldown(), "#time#", "5");
            return false;
        }

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            sendMessage(player, amount == null
                    ? LightCoins.instance.getMessageConfig().noNumber()
                    : LightCoins.instance.getMessageConfig().noNegativ());
            return false;
        }
        return true;
    }

    private boolean isMultiServerEnabled() {
        return LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled();
    }

    private boolean handleMultiServerTransaction(Player player, String targetName, BigDecimal amount) {
        CoinsData coinsPlayer = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(player.getUniqueId()).join();
        OfflinePlayer target = findOfflinePlayer(targetName);

        if (target == null || coinsPlayer == null) {
            sendMessage(player, LightCoins.instance.getMessageConfig().payOnlyOnlinePlayer(), "#target#", targetName);
            return false;
        }

        CoinsData coinsTarget = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(target.getUniqueId()).join();
        return processTransaction(player, target, coinsPlayer, coinsTarget, amount);
    }

    private boolean handleSingleServerTransaction(Player player, String targetName, BigDecimal amount) {
        List<String> availableAccounts = LightCoins.instance.getLightCoinsAPI().getAccountDataPlayerNames();

        if (!availableAccounts.contains(targetName)) {
            sendMessage(player, LightCoins.instance.getMessageConfig().playerNotFound(), "#player#", targetName);
            return false;
        }

        AccountData playerData = LightCoins.instance.getLightCoinsAPI().getAccountData(player);
        AccountData targetData = LightCoins.instance.getLightCoinsAPI().getAccountData(targetName);

        OfflinePlayer target = findOfflinePlayer(targetName);

        if (playerData == null || targetData == null || target == null) {
            sendMessage(player, LightCoins.instance.getMessageConfig().somethingWentWrong(),
                    "#target#", targetName,
                    "#info#", "Could not find player data for " + targetName
                    );
            return false;
        }

        CoinsData coinsPlayer = playerData.getCoinsData();
        CoinsData coinsTarget = targetData.getCoinsData();
        return processTransaction(player, target, coinsPlayer, coinsTarget, amount);
    }

    private boolean processTransaction(Player player, OfflinePlayer target, CoinsData coinsPlayer, CoinsData coinsTarget, BigDecimal amount) {
        EconomyResponse playerResponse = coinsPlayer.removeCoins(amount, EconomyReason.PAY_COMMAND);

        if (!playerResponse.transactionSuccess()) {
            sendMessage(player, LightCoins.instance.getMessageConfig().somethingWentWrong(), "#info#", playerResponse.errorMessage);
            return false;
        }

        EconomyResponse targetResponse = coinsTarget.addCoins(amount, EconomyReason.PAY_COMMAND);

        if (targetResponse.transactionSuccess()) {
            applyCooldown(player);
            notifyPlayers(player, target, coinsPlayer, amount);
            return true;
        } else {
            rollbackTransaction(player, coinsPlayer, amount, targetResponse.errorMessage);
            return false;
        }
    }

    private void applyCooldown(Player player) {
        int cooldownTime = LightCoins.instance.getSettingsConfig().payCommandCooldown();
        if (cooldownTime != -1) {
            cooldown.add(player);
            LightTimers.doSync(task -> cooldown.remove(player), cooldownTime * 20L);
        }
    }

    private void notifyPlayers(Player player, OfflinePlayer target, CoinsData coinsPlayer, BigDecimal amount) {
        sendMessage(player, LightCoins.instance.getMessageConfig().pay(),
                "#coins#", LightNumbers.formatForMessages(amount, LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()),
                "#currency#", coinsPlayer.getFormattedCurrency(),
                "#target#", target.getName()
        );
        EconomyTitle.sendEconomyTitle(player.getUniqueId(), EconomyTitleType.PAY_SENDER_COINS, amount, coinsPlayer.getFormattedCurrency(), player.getName(), target.getName());

        if (target.isOnline()) {
            sendMessage(target.getPlayer(), LightCoins.instance.getMessageConfig().payTarget(),
                    "#coins#", LightNumbers.formatForMessages(amount, LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()),
                    "#currency#", coinsPlayer.getFormattedCurrency(),
                    "#target#", player.getName()
            );
            EconomyTitle.sendEconomyTitle(target.getUniqueId(), EconomyTitleType.PAY_TARGET_COINS, amount, coinsPlayer.getFormattedCurrency(), player.getName(), target.getName());
        } else {
            SendProxyRequest.sendMessageToPlayer(player, target.getUniqueId(),
                    formatMessage(LightCoins.instance.getMessageConfig().payTarget(),
                            "#coins#", LightNumbers.formatForMessages(amount, LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()),
                            "#currency#", coinsPlayer.getFormattedCurrency(),
                            "#target#", player.getName()
                    ));
        }
    }

    private void rollbackTransaction(Player player, CoinsData coinsPlayer, BigDecimal amount, String errorMessage) {
        EconomyResponse rollbackResponse = coinsPlayer.addCoins(amount, EconomyReason.PAY_COMMAND);
        if (rollbackResponse.transactionSuccess()) {
            sendMessage(player, LightCoins.instance.getMessageConfig().somethingWentWrong(), errorMessage);
        } else {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "Could not transfer the coins back to the player after a failed transaction",
                    "via /pay command!",
                    "Player: " + player.getName(),
                    "Amount: " + amount,
                    "Error: " + errorMessage
            ));
        }
    }

    private OfflinePlayer findOfflinePlayer(String targetName) {
        return Arrays.stream(Bukkit.getServer().getOfflinePlayers())
                .filter(offlinePlayer -> offlinePlayer.getName() != null && offlinePlayer.getName().equalsIgnoreCase(targetName))
                .findFirst()
                .orElse(null);
    }

    private void sendMessage(Player player, List<String> messages, Object... replacements) {
        String message = formatMessage(messages, replacements);
        LightCore.instance.getMessageSender().sendPlayerMessage(player, LightCoins.instance.getMessageConfig().prefix() + message);
    }

    private String formatMessage(List<String> messages, Object... replacements) {
        return messages.stream()
                .map(message -> replacePlaceholders(message, replacements))
                .collect(Collectors.joining("\n"));
    }

    private String replacePlaceholders(String message, Object... replacements) {
        for (int i = 0; i < replacements.length; i += 2) {
            if (i + 1 >= replacements.length) {
                throw new IllegalArgumentException("Ungültige Anzahl an Ersatzwerten. Jeder Platzhalter benötigt einen Wert.");
            }
            String placeholder = String.valueOf(replacements[i]);
            String value = String.valueOf(replacements[i + 1]);
            message = message.replace(placeholder, value);
        }
        return message;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        LightCoins.instance.getConsolePrinter().printError("This command can only be executed by a player");
        return false;
    }
}