package io.lightstudios.coins.placeholder.coins;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CoinsTopHolder implements LightPlaceholder {

    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {
        if (!s.contains("coins_top")) {
            return null;
        }

        String[] split = s.split("_");
        if (split.length != 3 || !split[1].equalsIgnoreCase("top")) {
            return "<red>Wrong placeholder format for top";
        }

        int place;
        try {
            place = Integer.parseInt(split[2]);
            if (place < 1) {
                return "<red>Place is less than 1";
            }
        } catch (NumberFormatException e) {
            return "<red>Place is not a number";
        }

        Map<Integer, CoinsData> coinsTop = LightCoins.instance.getLightCoinsAPI().getTop(10);
        if (coinsTop == null || coinsTop.isEmpty()) {
            return "<dark_red>No top data found";
        }

        ConfigurationSection customSection = LightCoins.instance.getSettingsConfig().topPlaceholderFormatCustom();
        ConfigurationSection defaultSection = LightCoins.instance.getSettingsConfig().topPlaceholderFormatDefault();
        if (defaultSection == null) {
            return "<dark_red>Top placeholder format not found";
        }

        String defaultFormatValid = defaultSection.getString("valid",
                "<dark_gray>● <yellow><bold>#place#<reset><gray># <dark_gray>● <yellow>#name# <gray>- <yellow>#amount# <gray>#currency#");
        String defaultFormatInvalid = defaultSection.getString("invalid",
                "<gold>#place#<gray># <dark_gray>●  <gray>-<red>x<gray>-");

        String customFormatValid = customSection != null ? customSection.getString(place + ".valid", defaultFormatValid) : defaultFormatValid;
        String customFormatInvalid = customSection != null ? customSection.getString(place + ".invalid", defaultFormatInvalid) : defaultFormatInvalid;

        CoinsData data = coinsTop.get(place);
        if (data == null) {
            return customFormatInvalid.replace("#place#", String.valueOf(place));
        }

        return customFormatValid
                .replace("#place#", String.valueOf(place))
                .replace("#name#", data.getName())
                .replace("#amount#", data.getFormattedCoins())
                .replace("#currency#", data.getFormattedCurrency());
    }
}