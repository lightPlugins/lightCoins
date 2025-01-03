package io.lightstudios.coins.placeholder.coins;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CoinsAmountHolder implements LightPlaceholder {
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {

        if(!s.equalsIgnoreCase("coins")) {
            return null;
        }

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql")) {

            CoinsData coinsData = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(offlinePlayer.getUniqueId()).join();

            if(coinsData != null) {
                String placeholder = LightCoins.instance.getSettingsConfig().placeholderFormat()
                        .replace("#coins#", coinsData.getFormattedCoins())
                        .replace("#currency#", coinsData.getFormattedCurrency());

                return LightCore.instance.getColorTranslation().adventureTranslator(placeholder, offlinePlayer.getPlayer());
            }
            return "Player not found";
        }

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(offlinePlayer.getUniqueId());
        if(accountData == null) {
            return "Player not found";
        }

        String placeholder = LightCoins.instance.getSettingsConfig().placeholderFormat()
                .replace("#coins#", accountData.getCoinsData().getFormattedCoins())
                .replace("#currency#", accountData.getCoinsData().getFormattedCurrency());

        return LightCore.instance.getColorTranslation().adventureTranslator(placeholder, offlinePlayer.getPlayer());
    }
}
