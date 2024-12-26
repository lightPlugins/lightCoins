package io.lightstudios.coins.placeholder.virtual;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class VirtualCurrencyHolder implements LightPlaceholder {
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {
        // %lightcoins_virtual_raw_gems%
        if(!s.contains("virtual_currency_")) {
            return null;
        }

        String[] split = s.split("_");
        if(split.length < 3) {
            return "Out of Bounds -> " + s;
        }
        String currencyName = split[2];

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(offlinePlayer.getUniqueId());

        if(accountData == null) {
            return "Account not found";
        }

        VirtualData virtualData = accountData.getVirtualCurrencyByName(currencyName);

        if(virtualData == null) {
            return "Currency not found: " + currencyName;
        }

        return virtualData.getFormattedCurrencySymbol();
    }
}
