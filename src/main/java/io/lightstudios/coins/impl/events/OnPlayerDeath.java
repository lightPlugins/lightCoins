package io.lightstudios.coins.impl.events;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.impl.events.custom.LightCoinsLoseCoinsEvent;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

public class OnPlayerDeath implements Listener {

    @EventHandler
    public void loseMoneyOnDeath(EntityDeathEvent event) {

        if(event.getEntity() instanceof Player player) {

            // Check if the feature is enabled
            if(!LightCoins.instance.getSettingsConfig().enableLoseCoinsOnDeath()) {
                return;
            }

            // Check if the world is blacklisted
            String worldName = player.getWorld().getName();
            if(LightCoins.instance.getSettingsConfig().loseCoinsBlacklistWorlds().contains(worldName)) {
                return;
            }

            // Check if the player has the bypass permission
            // If the player has op, he has automatically the bypass permission
            if(player.hasPermission(LightCoins.instance.getSettingsConfig().loseCoinsBypassPermission())) {
                return;
            }

            AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(player.getUniqueId());

            // Check if the player has an account. This should never happen!
            if(accountData == null) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while trying to remove coins from player " + player.getName(),
                        "while lose coins system. This should never happen!",
                        "Error: The player account is null."
                ));
                return;
            }

            CoinsData coinsData = accountData.getCoinsData();

            // Check if the player has coins. This should never happen!
            if(coinsData == null) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while trying to remove coins from player " + player.getName(),
                        "while lose coins system. This should never happen!",
                        "Error: The player coins data is null."
                ));
                return;
            }

            double percentage = LightCoins.instance.getSettingsConfig().loseCoinsPercentage();

            // prevent unnecessary calculations
            if(percentage <= 0) {
                return;
            }

            double currentCoins = coinsData.getCurrentCoins().doubleValue();
            double coinsToLose = currentCoins * percentage;
            BigDecimal finalCoins = BigDecimal.valueOf(currentCoins - coinsToLose);

            // A custom event to allow other plugins to modify the amount of coins to remove (or cancel the event)
            LightCoinsLoseCoinsEvent loseCoinsEvent = new LightCoinsLoseCoinsEvent(player.getUniqueId(), finalCoins);
            finalCoins = loseCoinsEvent.getAmount();

            // Check if the event was cancelled by another plugin
            if(loseCoinsEvent.isCancelled()) {
                return;
            }


            if(finalCoins.doubleValue() < 0) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while trying to remove coins from player " + player.getName(),
                        "while lose coins system.",
                        "Error: The final amount of coins is less than 0.",
                        "Amount to remove: " + coinsToLose,
                        "New Amount: " + finalCoins.doubleValue()
                ));
                return;
            }

            EconomyResponse response = coinsData.setCoins(finalCoins);

            if(response.transactionSuccess()) {

                BigDecimal finalCoinsMessage = finalCoins;
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().loseCoinsOnDeath().stream().map(str -> str
                                        .replace("#coins#", LightNumbers.formatForMessages(finalCoinsMessage,
                                                LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()))
                                        .replace("#percentage#", String.valueOf(percentage))
                                        .replace("#currency#", coinsToLose == 1 ?
                                                coinsData.getNameSingular() : coinsData.getNamePlural())
                                ).collect(Collectors.joining()));

            } else {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while trying to remove coins from player " + player.getName(),
                        "while lose coins system.",
                        "Error: " + response.errorMessage
                ));
            }
        }
    }
}
