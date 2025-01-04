package io.lightstudios.coins.placeholder.virtual;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class VirtualAmountFormattedHolder implements LightPlaceholder {

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {
        if (!s.contains("virtual_formatted_")) {
            return null;
        }

        String[] split = s.split("_");
        if (split.length < 3) {
            return null;
        }
        String currencyName = split[2];

        BigDecimal balance = getBalance(offlinePlayer, currencyName);
        if (balance == null) {
            return "Currency/Player not found: " + currencyName + " - " + offlinePlayer.getName();
        }

        int decimalPlaces = getDecimalPlaces(offlinePlayer, currencyName);
        return formatBalance(balance, decimalPlaces);
    }

    private BigDecimal getBalance(OfflinePlayer offlinePlayer, String currencyName) {
        if (LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {

            List<VirtualData> virtualDataList = LightCoins.instance.getVirtualDataTable().readVirtualData().join();
            VirtualData virtualData = virtualDataList.stream()
                    .filter(data -> data.getPlayerUUID().equals(offlinePlayer.getUniqueId()) && data.getCurrencyName().equalsIgnoreCase(currencyName))
                    .findFirst()
                    .orElse(null);

            if (virtualData == null) {
                return null;
            }

            return virtualData.getCurrentBalance();
        }

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(offlinePlayer.getUniqueId());

        if (accountData == null) {
            return null;
        }

        VirtualData virtualData = accountData.getVirtualCurrencyByName(currencyName);

        if (virtualData == null) {
            return null;
        }

        return virtualData.getCurrentBalance();
    }

    private int getDecimalPlaces(OfflinePlayer offlinePlayer, String currencyName) {
        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(offlinePlayer.getUniqueId());
        if (accountData == null) {
            return 0;
        }

        VirtualData virtualData = accountData.getVirtualCurrencyByName(currencyName);
        if (virtualData == null) {
            return 0;
        }

        return virtualData.getDecimalPlaces();
    }

    private String formatBalance(BigDecimal balance, int decimalPlaces) {
        BigDecimal thousand = new BigDecimal(1000);
        BigDecimal million = new BigDecimal(1000000);
        BigDecimal billion = new BigDecimal(1000000000);
        BigDecimal trillion = new BigDecimal(1000000000000L);

        if (balance.compareTo(trillion) >= 0) {
            return balance.divide(trillion, decimalPlaces, RoundingMode.DOWN) + "t";
        } else if (balance.compareTo(billion) >= 0) {
            return balance.divide(billion, decimalPlaces, RoundingMode.DOWN) + "b";
        } else if (balance.compareTo(million) >= 0) {
            return balance.divide(million, decimalPlaces, RoundingMode.DOWN) + "m";
        } else if (balance.compareTo(thousand) >= 0) {
            return balance.divide(thousand, decimalPlaces, RoundingMode.DOWN) + "k";
        } else {
            return balance.setScale(decimalPlaces, RoundingMode.DOWN).toPlainString();
        }
    }
}