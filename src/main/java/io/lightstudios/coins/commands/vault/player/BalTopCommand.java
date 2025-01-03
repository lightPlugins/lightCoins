package io.lightstudios.coins.commands.vault.player;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.permissions.LightPermissions;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.interfaces.LightCommand;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BalTopCommand implements LightCommand {
    @Override
    public List<String> getSubcommand() {
        return List.of();
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public String getSyntax() {
        return "";
    }

    @Override
    public int maxArgs() {
        return -1;
    }

    @Override
    public String getPermission() {
        return LightPermissions.BALTOP_COMMAND.getPerm();
    }

    @Override
    public TabCompleter registerTabCompleter() {
        return null;
    }

    @Override
    public boolean performAsPlayer(Player player, String[] strings) {
        int x = LightCoins.instance.getSettingsConfig().baltopCommandAmount();
        int decimalPlaces = LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces();

        List<AccountData> allPlayers;

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {

            List<CoinsData> allCoinsData = LightCoins.instance.getCoinsTable().readCoinsData().join();
            List<AccountData> tempAccountList = new ArrayList<>();
            for(CoinsData coinsData : allCoinsData) {
                AccountData accountData = new AccountData();
                accountData.setName(coinsData.getName());
                accountData.setUuid(coinsData.getUuid());
                accountData.setCoinsData(coinsData);
                tempAccountList.add(accountData);
            }
            allPlayers = tempAccountList;
        } else {
            allPlayers = new ArrayList<>(LightCoins.instance.getLightCoinsAPI().getAccountData().values());
        }

        List<AccountData> sortedPlayers = allPlayers.stream()
                .filter(p -> p.getName() != null && !p.getName().equalsIgnoreCase("nonplayer_account"))
                .sorted((p1, p2) -> p2.getCoinsData().getCurrentCoins().compareTo(p1.getCoinsData().getCurrentCoins()))
                .limit(x)
                .toList();

        BigDecimal overallCoins = sortedPlayers.stream()
                .filter(p -> p.getName() != null && !p.getName().equalsIgnoreCase("nonplayer_account"))
                .map(AccountData::getCoinsData)
                .map(CoinsData::getCurrentCoins)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<String> messages = LightCoins.instance.getMessageConfig().baltopHeader()
                .stream().map(s -> s.replace("#overall#", overallCoins.toString())
                        .replace("#overall#", LightNumbers.formatForMessages(overallCoins, decimalPlaces))
                        .replace("#currency#", LightCoins.instance.getSettingsConfig().defaultCurrencyNamePlural()))
                .collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < sortedPlayers.size(); i++) {
            AccountData playerData = sortedPlayers.get(i);

            if (playerData.getName() == null) {
                LightCoins.instance.getConsolePrinter().printDebug("Player name is null for player with UUID: " + playerData.getUuid());
                continue;
            }

            messages.add(LightCoins.instance.getMessageConfig().baltopContent()
                    .replace("#number#", String.valueOf(i + 1))
                    .replace("#name#", playerData.getName())
                    .replace("#amount#", playerData.getCoinsData().getFormattedCoins())
                    .replace("#currency#", playerData.getCoinsData().getFormattedCurrency()));
        }
        messages.addAll(LightCoins.instance.getMessageConfig().baltopFooter());

        LightCore.instance.getMessageSender().sendPlayerMessage(player, messages);
        return true;
    }

    @Override
    public boolean performAsConsole(ConsoleCommandSender consoleCommandSender, String[] strings) {
        return false;
    }
}
