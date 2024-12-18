package io.lightstudios.coins.impl.events;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.core.LightCore;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Collectors;

public class OnPlayerJoin implements Listener {

    // ensure the player data is loaded before the player joins the server
    // and other plugins can access the player data from the LightCoinsAPI
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        if (LightCoins.instance.getLightCoinsAPI().getPlayerData().containsKey(uuid)) {
            LightCoins.instance.getConsolePrinter().printInfo("Player data already exists for " + uuid);
            return;
        }

        LightCoins.instance.getCoinsTable().findCoinsDataByUUID(uuid).thenAcceptAsync(coinsData -> {
            if (coinsData == null) {
                if (!LightCoins.instance.getVaultImplementer().createPlayerAccount(uuid.toString())) {
                    LightCoins.instance.getConsolePrinter().printError("Failed to create player account for " + uuid);
                }

                BigDecimal starterCoins = LightCoins.instance.getSettingsConfig().defaultCurrencyStartBalance();
                AccountData playerData = new AccountData();
                CoinsData newCoinsData = new CoinsData(uuid);
                newCoinsData.setName(event.getPlayer().getName());
                playerData.setCoinsData(newCoinsData);
                playerData.setName(event.getPlayer().getName());
                playerData.setOfflinePlayer(event.getPlayer());
                LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);

                EconomyResponse response = newCoinsData.addCoins(starterCoins);
                if (response.transactionSuccess()) {
                    LightCore.instance.getMessageSender().sendPlayerMessage(
                            event.getPlayer(),
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().starterCoins().stream().map(str -> str
                                            .replace("#coins#", String.valueOf(starterCoins))
                                            .replace("#currency#", starterCoins.compareTo(BigDecimal.ONE) == 0
                                                    ? newCoinsData.getNameSingular()
                                                    : newCoinsData.getNamePlural())
                                    ).collect(Collectors.joining()));
                    LightCoins.instance.getConsolePrinter().printDebug("message sent");
                } else {
                    LightCoins.instance.getConsolePrinter().printError("Transaction failed: " + response.errorMessage);
                }

            } else {
                AccountData playerData = new AccountData();
                playerData.setCoinsData(coinsData);
                playerData.setName(event.getPlayer().getName());
                LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);
                LightCoins.instance.getConsolePrinter().printInfo("Player data loaded for " + uuid);
            }
        });
    }
}
