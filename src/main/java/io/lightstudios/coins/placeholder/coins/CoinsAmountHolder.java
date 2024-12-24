package io.lightstudios.coins.placeholder.coins;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
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

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(offlinePlayer.getUniqueId());
        if(accountData == null) {
            return "Player not found";
        }

        String placeholder = LightCoins.instance.getSettingsConfig().coinsPlaceholder()
                .replace("#coins#", accountData.getCoinsData().getFormattedCoins())
                .replace("#currency#", accountData.getCoinsData().getFormattedCurrency());

        return LightCore.instance.getColorTranslation().adventureTranslator(placeholder, offlinePlayer.getPlayer());
    }
}
