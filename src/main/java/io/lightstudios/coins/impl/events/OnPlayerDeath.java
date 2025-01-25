package io.lightstudios.coins.impl.events;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.impl.events.custom.LightCoinsLoseCoinsEvent;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
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

            if(player.getLastDamageCause() == null) {
                return;
            }

            List<String> deathCauses = LightCoins.instance.getSettingsConfig().loseCoinsDeathCause();

            if(deathCauses.isEmpty()) {
                LightCoins.instance.getConsolePrinter().printConfigError(List.of(
                        "An error occurred while trying to remove coins from player " + player.getName(),
                        "while lose coins system.",
                        "Error: The death cause list is empty in settings.yml",
                        "       Please check your settings.yml file for at least one valid death cause.",
                        "       You can use the wildcard '*' to remove coins from all death causes."
                ));
                return;
            }

            // check if the deathCause is a wildcard -> '*'
            if(!deathCauses.contains("*")) {
                boolean contains = false;
                for(String deathCause : deathCauses) {

                    EntityDamageEvent.DamageCause damageCause;
                    try {
                        // Try to get the damage cause from the string
                        damageCause = EntityDamageEvent.DamageCause.valueOf(deathCause.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        // Handle the case where the damageCause is not valid
                        LightCoins.instance.getConsolePrinter().printConfigError(List.of(
                                "An error occurred while trying to remove coins from player " + player.getName(),
                                "Invalid damage cause in settings.yml: §4" + deathCause,
                                "See: https://jd.papermc.io/paper/1.21.4/org/bukkit/event/entity/PlayerDeathEvent.html"
                        ));
                        return;
                    }

                    // Check if the player died by the damage cause
                    if(player.getLastDamageCause().getCause().equals(damageCause)) {
                        contains = true;
                        break;
                    }
                }

                // If the death cause is not in the list, return
                if(!contains) {
                    return;
                }
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
                        "Error: §4The player account is null."
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
            double minAmount = LightCoins.instance.getSettingsConfig().loseCoinsMinAmount();
            double maxAmount = LightCoins.instance.getSettingsConfig().loseCoinsMaxAmount();

            // prevent unnecessary calculations
            if(percentage <= 0) {
                return;
            }

            double currentCoins = coinsData.getCurrentCoins().doubleValue();
            double coinsToLose = currentCoins * percentage;
            BigDecimal finalCoins = BigDecimal.valueOf(currentCoins - coinsToLose);

            // check if the min or max amount is valid
            if(minAmount > maxAmount) {
                LightCoins.instance.getConsolePrinter().printConfigError(List.of(
                        "An error occurred while trying to remove coins from player " + player.getName(),
                        "while lose coins system.",
                        "Error: The min amount is greater than the max amount in your config.",
                        "       Please check your settings.yml file.",
                        "Min Amount: §4" + minAmount,
                        "Max Amount: §4" + maxAmount
                ));
                return;

            }
            // check if the minAmount is reached to lose coins
            if(coinsToLose < minAmount) {
                return;
            }

            // check if the maxAmount is reached to lose coins
            // if the maxAmount is reached, the player will lose the 'maxAmount' value of coins
            if(coinsToLose >= maxAmount) {
                coinsToLose = maxAmount;
            }

            // A custom event to allow other plugins to modify the amount of coins to remove (or cancel the event)
            LightCoinsLoseCoinsEvent loseCoinsEvent = new LightCoinsLoseCoinsEvent(player.getUniqueId(), finalCoins);
            finalCoins = loseCoinsEvent.getAmount();

            // Check if the event was cancelled by another plugin
            if(loseCoinsEvent.isCancelled()) {
                return;
            }

            if(percentage > 1 || !LightNumbers.isPositiveNumber(percentage)) {
                LightCoins.instance.getConsolePrinter().printConfigError(List.of(
                        "An error occurred while trying to remove coins from player " + player.getName(),
                        "while lose coins system.",
                        "Error: The percentage is greater than 1 or less than 0 in your config.",
                        "       Please check your settings.yml file.",
                        "Current Percentage: §4" + percentage * 100 + "§c%",
                        "Raw Percentage: §4" + percentage
                ));
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

            EconomyResponse response = coinsData.removeCoins(BigDecimal.valueOf(coinsToLose));

            if(response.transactionSuccess()) {

                double finalCoinsToLose = coinsToLose;
                LightCore.instance.getMessageSender().sendPlayerMessage(
                        player,
                        LightCoins.instance.getMessageConfig().prefix() +
                                LightCoins.instance.getMessageConfig().loseCoinsOnDeath().stream().map(str -> str
                                        .replace("#coins#", LightNumbers.formatForMessages(BigDecimal.valueOf(finalCoinsToLose),
                                                LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces()))
                                        .replace("#percentage#", String.valueOf(percentage))
                                        .replace("#currency#", finalCoinsToLose == 1 ?
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
