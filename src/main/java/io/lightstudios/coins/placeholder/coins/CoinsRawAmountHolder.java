package io.lightstudios.coins.placeholder.coins;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CoinsRawAmountHolder implements LightPlaceholder {
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {

        if(!s.equalsIgnoreCase("coins_raw")) {
            return null;
        }

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql")) {

            CoinsData coinsData = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(offlinePlayer.getUniqueId()).join();

            if(coinsData != null) {
                return coinsData.getCurrentCoins().toPlainString();
            }
            return "Player not found";
        }

        AccountData playerData = LightCoins.instance.getLightCoinsAPI().getAccountData(offlinePlayer.getUniqueId());
        if(playerData == null) {
            return "Player not found";
        }

        return playerData.getCoinsData().getCurrentCoins().toPlainString();
    }
}
