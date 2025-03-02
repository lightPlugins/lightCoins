package io.lightstudios.coins.commands.vault.admin;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.interfaces.LightCommand;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class AddAllCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of("giveall", "addall");
    }

    @Override
    public String getDescription() {
        return "Give all players in the Database x coins";
    }

    @Override
    public String getSyntax() {
        return "/coins addall <amount>";
    }

    @Override
    public int maxArgs() {
        return 2;
    }

    @Override
    public String getPermission() {
        return LightPermissions.ADD_ALL_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return (sender, command, alias, args) -> {

            if (args.length == 1) {
                if(sender.hasPermission(getPermission())) {
                    return getSubcommand();
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
                            LightCoins.instance.getMessageConfig().wrongSyntax().stream().map(str -> str
                                    .replace("#syntax#", getSyntax())
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

        HashMap<UUID, AccountData> accounts = LightCoins.instance.getLightCoinsAPI().getAccountData();

        int failedTransactions = 0;
        int successfulTransactions = 0;

        for (AccountData account : accounts.values()) {

            if(account.getName() != null) {
                if(account.getName().equalsIgnoreCase("nonplayer_account")) {
                    continue;
                }
            }

            EconomyResponse response = account.getCoinsData().addCoins(amount);
            if(!response.transactionSuccess()) {
                failedTransactions++;
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "Failed to add coins to player: " + account.getCoinsData().getName(),
                        "Error: " + response.errorMessage
                ));
                continue;
            }
            successfulTransactions++;
        }

        int finalSuccessfulTransactions = successfulTransactions;

        if(failedTransactions == 0) {
            LightCoins.instance.getConsolePrinter().printInfo(List.of(
                    "Added " + amount + " to " + finalSuccessfulTransactions + " players",
                    "Failed to add coins to " + failedTransactions + " players"
            ));
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    List.of(
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().coinsAddAll().stream().map(str -> str
                                            .replace("#coins#", LightNumbers.formatForMessages(amount, 2))
                                            .replace("#currency#", LightCoins.instance.getSettingsConfig().defaultCurrencyNamePlural())
                                            .replace("#amount#", String.valueOf(finalSuccessfulTransactions))
                                    ).collect(Collectors.joining())
                    )
            );
        } else {
            int finalFailedTransactions = failedTransactions;
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    player,
                    List.of(
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().coinsAddAllFailed().stream().map(str -> str
                                            .replace("#coins#", LightNumbers.formatForMessages(amount, 2))
                                            .replace("#currency#", LightCoins.instance.getSettingsConfig().defaultCurrencyNamePlural())
                                            .replace("#amount#", String.valueOf(finalFailedTransactions))
                                    ).collect(Collectors.joining())
                    )
            );
        }

        return false;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
