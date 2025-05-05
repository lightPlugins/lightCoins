package io.lightstudios.coins.title;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class EconomyTitle {

    public static void sendEconomyTitle(UUID uuid, EconomyTitleType type, BigDecimal amount, String currency, String sender, String receiver) {

        ConfigurationSection section = LightCoins.instance.getSettingsConfig().titleEconomyStatic();

        if(section == null) {
            LightCoins.instance.getConsolePrinter().printConfigError(List.of(
                    "An error occurred while trying to send a title to player " + uuid,
                    "Please check if the title economy static section is present in the settings.yml file."
            ));
            return;
        }

        boolean enabled = section.getBoolean("enabled", true);
        int fadeIn = section.getInt("fadeIn", 20);
        int stay = section.getInt("stay", 80);
        int fadeOut = section.getInt("fadeOut", 20);

        Player player = Bukkit.getPlayer(uuid);

        if(player == null) {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while trying to send a title to player " + uuid,
                    "Failed to send the title, because the player is not online/valid.",
                    "Please contact the developer!"
            ));
            return;
        }

        int decimalPlaces = LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces();

        if(type.getPath() == null) {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while trying to send a title to player " + uuid,
                    "Failed to split TitleType with '.' on type: " + type.getType(),
                    "Please contact the developer!"
            ));
            return;
        }

        String upperTitle = section.getString(type.getType() + ".upper", "Upper Title")
                .replace("#sender#", sender)
                .replace("#target#", receiver)
                .replace("#currency#", currency)
                .replace("#amount#", LightNumbers.formatForMessages(amount, decimalPlaces));
        String lowerTitle = section.getString(type.getType() + ".lower", "Lower Title")
                .replace("#sender#", sender)
                .replace("#target#", receiver)
                .replace("#currency#", currency)
                .replace("#amount#", LightNumbers.formatForMessages(amount, decimalPlaces));;

        Component upperTitleComponent = Component.text(upperTitle);
        Component lowerTitleComponent = Component.text(lowerTitle);

        if(!enabled) {
            return;
        }

        LightCore.instance.getTitleSender().sendTitle(
                player,
                upperTitleComponent,
                lowerTitleComponent,
                fadeIn,
                stay,
                fadeOut);
        LightCore.instance.getConsolePrinter().printWarning("Sent title to player " + player.getName() + " with UUID " + player.getUniqueId() + " with type: " + type.getType());
    }
}
