package io.lightstudios.coins.placeholder.coins;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CurrencyNameHolder implements LightPlaceholder {
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {

        if(!s.equalsIgnoreCase("currency_name")) {
            return null;
        }

        AccountData playerData = LightCoins.instance.getLightCoinsAPI().getPlayerData(offlinePlayer.getUniqueId());
        if(playerData == null) {
            return "Player not found";
        }

        return playerData.getCoinsData().getFormattedCurrency();
    }
}
