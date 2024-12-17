package io.lightstudios.coins.impl.events;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.core.LightCore;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.math.BigDecimal;
import java.util.Map;
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

        LightCoins.instance.getCoinsTable().readCoins(uuid.toString()).thenAcceptAsync(result -> {
            if (result.isEmpty()) {
                if (!LightCoins.instance.getVaultImplementer().createPlayerAccount(uuid.toString())) {
                    LightCoins.instance.getConsolePrinter().printError("Failed to create player account for " + uuid);
                }

                BigDecimal starterCoins = LightCoins.instance.getSettingsConfig().defaultCurrencyStartBalance();
                PlayerData playerData = new PlayerData();
                CoinsPlayer coinsPlayer = new CoinsPlayer(uuid);
                OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(uuid);
                coinsPlayer.setCoins(starterCoins);
                playerData.setCoinsPlayer(coinsPlayer);
                playerData.setPlayerName(offlinePlayer.getName());
                playerData.setOfflinePlayer(offlinePlayer);
                LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);

                EconomyResponse response = coinsPlayer.addCoins(starterCoins);
                if (response.transactionSuccess()) {
                    LightCore.instance.getMessageSender().sendPlayerMessage(
                            event.getPlayer(),
                            LightCoins.instance.getMessageConfig().prefix() +
                                    LightCoins.instance.getMessageConfig().starterCoins().stream().map(str -> str
                                            .replace("#coins#", String.valueOf(starterCoins))
                                            .replace("#currency#", starterCoins.compareTo(BigDecimal.ONE) == 0
                                                    ? coinsPlayer.getNameSingular()
                                                    : coinsPlayer.getNamePlural())
                                    ).collect(Collectors.joining()));
                    LightCoins.instance.getConsolePrinter().printDebug("message sent");
                } else {
                    LightCoins.instance.getConsolePrinter().printError("Transaction failed: " + response.errorMessage);
                }

            } else {
                PlayerData playerData = new PlayerData();
                CoinsPlayer coinsPlayer = new CoinsPlayer(uuid);
                BigDecimal coins = result.get(uuid);
                if (coins != null) {
                    coinsPlayer.setCoins(coins);
                    playerData.setPlayerName(event.getPlayer().getName());
                }
                playerData.setCoinsPlayer(coinsPlayer);
                LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);
                LightCoins.instance.getConsolePrinter().printInfo("Player data loaded for " + uuid);
            }
        });
    }
}
