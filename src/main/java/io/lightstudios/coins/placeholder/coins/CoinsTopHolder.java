package io.lightstudios.coins.placeholder.coins;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CoinsTopHolder implements LightPlaceholder {


    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {

        // placeholder: %coins_top_<place>%
        // example: %coins_top_1%
        if(!s.contains("coins_top")) {
            return null;
        }

        String[] split = s.split("_");

        if(split.length != 3) {
            return "<red>Wrong placeholder format for top";
        }

        if(!split[1].equalsIgnoreCase("top")) {
            return null;
        }

        try {

            int place = Integer.parseInt(split[2]);

            if(place < 1) {
                return "<red>Place is less than 1";
            }

            Map<Integer, CoinsData> coinsTop = LightCoins.instance.getLightCoinsAPI().getTop(10);

            if(coinsTop == null) {
                return "<dark_red>No top data found";
            }

            if(coinsTop.get(place) == null) {
                return "#" + place + " <red>not found";
            }

            return LightCoins.instance.getSettingsConfig().placeholderFormat()
                    .replace("#coins#", coinsTop.get(place).getFormattedCoins())
                    .replace("#currency#", coinsTop.get(place).getFormattedCurrency());

        } catch (NumberFormatException e) {
            return "<red>Place is not a number";
        }
    }
}
