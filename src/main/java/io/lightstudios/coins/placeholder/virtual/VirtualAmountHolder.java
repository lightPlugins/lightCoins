package io.lightstudios.coins.placeholder.virtual;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class VirtualAmountHolder implements LightPlaceholder {
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {
        // %lightcoins_virtual_gems%
        if(!s.contains("virtual_amount_") ) {
            return null;
        }

        String[] split = s.split("_");
        if(split.length < 3) {
            return null;
        }
        String currencyName = split[2];

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {

            List<VirtualData> virtualDataList = LightCoins.instance.getVirtualDataTable().readVirtualData().join();
            VirtualData virtualData = virtualDataList.stream()
                    .filter(data -> data.getPlayerUUID().equals(offlinePlayer.getUniqueId()) && data.getCurrencyName().equalsIgnoreCase(currencyName))
                    .findFirst()
                    .orElse(null);

            if(virtualData == null) {
                return "Currency not found: " + currencyName;
            }

            String placeholder = virtualData.getPlaceholderFormat()
                    .replace("#amount#", virtualData.getFormattedBalance())
                    .replace("#currency#", virtualData.getFormattedCurrencySymbol());

            return LightCore.instance.getColorTranslation().adventureTranslator(placeholder, offlinePlayer.getPlayer());

        }

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(offlinePlayer.getUniqueId());

        if(accountData == null) {
            return "Account not found";
        }

        VirtualData virtualData = accountData.getVirtualCurrencyByName(currencyName);

        if(virtualData == null) {
            return "Currency not found: " + currencyName;
        }

        String placeholder = virtualData.getPlaceholderFormat()
                .replace("#amount#", virtualData.getFormattedBalance())
                .replace("#currency#", virtualData.getFormattedCurrencySymbol());

        return LightCore.instance.getColorTranslation().adventureTranslator(placeholder, offlinePlayer.getPlayer());
    }
}
