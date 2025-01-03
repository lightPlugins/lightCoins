package io.lightstudios.coins.placeholder.coins;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class CoinsAmountFormattedHolder implements LightPlaceholder {

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {
        if (!s.contains("coins_formatted")) {
            return null;
        }

        BigDecimal balance = getBalance(offlinePlayer);
        if (balance == null) {
            return "Player not found: " + offlinePlayer.getName();
        }

        return formatBalance(balance);
    }

    private BigDecimal getBalance(OfflinePlayer offlinePlayer) {
        if (LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {

            CoinsData coinsData = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(offlinePlayer.getUniqueId()).join();
            if (coinsData == null) {
                return null;
            }

            return coinsData.getCurrentCoins();
        }

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData(offlinePlayer.getUniqueId());
        if (accountData == null) {
            return null;
        }

        return accountData.getCoinsData().getCurrentCoins();
    }

    private String formatBalance(BigDecimal balance) {
        BigDecimal thousand = new BigDecimal(1000);
        BigDecimal million = new BigDecimal(1000000);
        BigDecimal billion = new BigDecimal(1000000000);
        BigDecimal trillion = new BigDecimal(1000000000000L);

        if (balance.compareTo(trillion) >= 0) {
            return balance.divide(trillion, RoundingMode.DOWN).setScale(1, RoundingMode.DOWN) + "t";
        } else if (balance.compareTo(billion) >= 0) {
            return balance.divide(billion, RoundingMode.DOWN).setScale(1, RoundingMode.DOWN) + "b";
        } else if (balance.compareTo(million) >= 0) {
            return balance.divide(million, RoundingMode.DOWN).setScale(1, RoundingMode.DOWN) + "m";
        } else if (balance.compareTo(thousand) >= 0) {
            return balance.divide(thousand, RoundingMode.DOWN).setScale(1, RoundingMode.DOWN) + "k";
        } else {
            return balance.toPlainString();
        }
    }
}