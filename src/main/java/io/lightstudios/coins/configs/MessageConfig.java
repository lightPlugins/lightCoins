package io.lightstudios.coins.configs;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.LightCoinsAPI;
import io.lightstudios.coins.api.VirtualResponse;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.coins.api.models.VirtualCurrency;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.files.FileManager;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class MessageConfig {

    private final FileConfiguration config;

    public MessageConfig(FileManager selectedLanguage) {
        this.config = selectedLanguage.getConfig();
    }

    public int version() { return config.getInt("version"); }

    public String prefix() { return config.getString("prefix"); }
    public List<String> noPermission() { return toStringList(config.get("noPermission")); }
    public List<String> wrongSyntax() { return toStringList(config.get("wrongSyntax")); }
    public List<String> noNumber() { return toStringList(config.get("noNumber")); }
    public List<String> noNegativ() { return toStringList(config.get("noNegativ")); }
    public List<String> playerNotFound() { return toStringList(config.get("playerNotFound")); }
    public List<String> somethingWentWrong() { return toStringList(config.get("somethingWentWrong")); }
    public List<String> coinsShow() { return toStringList(config.get("coinsShow")); }
    public List<String> coinsShowTarget() { return toStringList(config.get("coinsShowTarget")); }
    public List<String> coinsAdd() { return toStringList(config.get("coinsAdd")); }
    public List<String> coinsRemove() { return toStringList(config.get("coinsRemove")); }



    private List<String> toStringList(Object input) {
        if (input instanceof String str) {
            return Collections.singletonList(str);
        } else if (input instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } else {
            LightCore.instance.getConsolePrinter().printError(List.of(
                    "Error in your message file at " + input,
                    "Input must be a String or a List of Strings",
                    "example as String: test: 'Test message'",
                    "example as List: test: - 'Test message'"
            ));
            throw new IllegalArgumentException("Input must be a String or a List of Strings");
        }
    }

}
