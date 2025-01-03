package io.lightstudios.coins.placeholder.coins;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CoinsCurrencyHolder implements LightPlaceholder {
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {

        if(s.equalsIgnoreCase("currency_name_singular")) {
            return LightCoins.instance.getSettingsConfig().defaultCurrencyNameSingular();
        }

        if(s.equalsIgnoreCase("currency_name_plural")) {
            return LightCoins.instance.getSettingsConfig().defaultCurrencyNamePlural();
        }

        return null;

    }
}
