package io.lightstudios.coins.placeholder.virtual;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.core.placeholder.LightPlaceholder;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class VirtualTopHolder implements LightPlaceholder {
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, @NotNull String s) {

        // %lightcoins_virtual_top_<currency>_<place>%
        if (!s.contains("virtual_top")) {
            return null;
        }

        String[] split = s.split("_");
        if (split.length != 4 || !split[1].equalsIgnoreCase("top")) {
            return "<red>Wrong placeholder format for top";
        }

        String currencyName = split[2];

        int place;
        try {
            place = Integer.parseInt(split[3]);
            if (place < 1) {
                return "<red>Place is less than 1";
            }
        } catch (NumberFormatException e) {
            return "<red>Place is not a number";
        }

        Map<Integer, VirtualData> virtualTop = LightCoins.instance.getLightCoinsAPI().getTopVirtual(currencyName, 10);
        if (virtualTop == null || virtualTop.isEmpty()) {
            return "<dark_red>No top virtual data found for " + currencyName;
        }

        ConfigurationSection defaultSection =
                LightCoins.instance.getSettingsConfig().topPlaceholderFormatVirtualDefault().getConfigurationSection(
                        currencyName + ".default");
        ConfigurationSection customSection =
                LightCoins.instance.getSettingsConfig().topPlaceholderFormatVirtualDefault().getConfigurationSection(
                        currencyName + ".custom");

        if(defaultSection == null) {
            LightCoins.instance.getConsolePrinter().printError(
                    "Could not find placeholder format section for: " + currencyName + " in settings.yml ");
            return "<dark_red>Virtual currency format not found";
        }

        String defaultFormatValid = defaultSection.getString("valid",
                "<dark_gray>● <yellow><bold>#place#<reset><gray># <dark_gray>● <yellow>#name# <gray>- <yellow>#amount# <gray>#currency#");
        String defaultFormatInvalid = defaultSection.getString("invalid",
                "<gold>#place#<gray># <dark_gray>●  <gray>-<red>x<gray>-");

        String customFormatValid = customSection != null ? customSection.getString(place + ".valid", defaultFormatValid) : defaultFormatValid;
        String customFormatInvalid = customSection != null ? customSection.getString(place + ".invalid", defaultFormatInvalid) : defaultFormatInvalid;

        VirtualData data = virtualTop.get(place);
        if (data == null) {
            return customFormatInvalid.replace("#place#", String.valueOf(place));
        }


        return customFormatValid
                .replace("#place#", String.valueOf(place))
                .replace("#name#", data.getPlayerName())
                .replace("#amount#", data.getFormattedBalance())
                .replace("#currency#", data.getDisplayName());
    }
}
