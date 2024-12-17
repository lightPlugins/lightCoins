package io.lightstudios.coins.placeholder.coins;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class CoinsRawAmountHolder implements LightPlaceholder {
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {

        if(!s.equalsIgnoreCase("coins_raw")) {
            return null;
        }

        PlayerData playerData = LightCoins.instance.getLightCoinsAPI().getPlayerData(offlinePlayer.getUniqueId());
        if(playerData == null) {
            return "Player not found";
        }

        return playerData.getCoinsPlayer().getCoins().toPlainString();
    }
}
