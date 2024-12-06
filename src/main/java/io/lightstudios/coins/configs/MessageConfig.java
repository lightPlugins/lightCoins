package io.lightstudios.coins.configs;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.LightCoinsAPI;
import io.lightstudios.coins.api.VirtualResponse;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.coins.api.models.VirtualCurrency;
import net.milkbowl.vault.economy.EconomyResponse;

import java.math.BigDecimal;
import java.util.UUID;

public class MessageConfig {

    public void test() {

        LightCoins lightCoins = LightCoins.instance;
        LightCoinsAPI lightCoinsAPI = lightCoins.getLightCoinsAPI();

        PlayerData playerData = lightCoinsAPI.getPlayerData(UUID.randomUUID());

        if(playerData == null) {
            System.out.println("Player data is null!");
            return;
        }

        VirtualCurrency virtualCurrency = playerData.getVirtualCurrencyByName("coins");

        if(virtualCurrency == null) {
            System.out.println("Virtual currency is null!");
            return;
        }

        CoinsPlayer coinsPlayer = playerData.getCoinsPlayer();

        EconomyResponse vaultResponse = coinsPlayer.addCoins(new BigDecimal(100));
        VirtualResponse virtualResponse = virtualCurrency.addBalance(new BigDecimal(100));


        if(virtualResponse.transactionSuccess()) {
            System.out.println("Coins added successfully!");
        } else {
            System.out.println("Failed to add coins!");
        }

        if(vaultResponse.transactionSuccess()) {
            System.out.println("Coins added successfully!");
        } else {
            System.out.println("Failed to add coins!");
        }

    }
}
