package io.lightstudios.coins.configs;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.core.util.files.FileManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.Collections;
import java.util.List;
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
    public List<String> reloadSuccess() { return toStringList(config.get("reloadSuccess")); }
    public List<String> starterCoins() { return toStringList(config.get("starterCoins")); }
    public List<String> playerDeleted() { return toStringList(config.get("playerDeleted")); }
    public List<String> coinsShow() { return toStringList(config.get("coinsShow")); }
    public List<String> coinsShowTarget() { return toStringList(config.get("coinsShowTarget")); }
    public List<String> coinsAdd() { return toStringList(config.get("coinsAdd")); }
    public List<String> coinsAddAll() { return toStringList(config.get("coinsAddAll")); }
    public List<String> coinsAddAllFailed() { return toStringList(config.get("coinsAddAllFailed")); }
    public List<String> coinsSet() { return toStringList(config.get("coinsSet")); }
    public List<String> coinsRemove() { return toStringList(config.get("coinsRemove")); }
    public List<String> pay() { return toStringList(config.get("pay")); }
    public List<String> payTarget() { return toStringList(config.get("payTarget")); }
    public List<String> payCooldown() { return toStringList(config.get("payCooldown")); }
    public List<String> payOnlyOnlinePlayer() { return toStringList(config.get("payOnlyOnlinePlayer")); }
    public List<String> payNotYourself() { return toStringList(config.get("payNotYourself")); }

    public List<String> baltopHeader() { return config.getStringList("baltop.header"); }
    public String baltopContent() { return config.getString("baltop.content"); }
    public List<String> baltopFooter() { return config.getStringList("baltop.footer"); }

    public List<String> virtualShow() { return toStringList(config.get("virtualShow")); }
    public List<String> virtualCurrencyNotFound() { return toStringList(config.get("virtualCurrencyNotFound")); }
    public List<String> virtualCurrencyAdd() { return toStringList(config.get("virtualCurrencyAdd")); }
    public List<String> virtualCurrencySet() { return toStringList(config.get("virtualCurrencySet")); }
    public List<String> virtualCurrencyRemove() { return toStringList(config.get("virtualCurrencyRemove")); }

    public List<String> helpCommandCoins() { return config.getStringList("helpCommand.coins"); }
    public List<String> helpCommandVirtual() { return config.getStringList("helpCommand.virtual"); }
    public List<String> helpCommandDefault() { return config.getStringList("helpCommand.default"); }
    public List<String> helpCommandPlayer() { return config.getStringList("helpCommand.player"); }




    private List<String> toStringList(Object input) {
        if (input instanceof String str) {
            return Collections.singletonList(str);
        } else if (input instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        } else {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "Error in your message file at " + input,
                    "Input must be a String or a List of Strings",
                    "example as String: test: 'Test message'",
                    "example as List: test: - 'Test message'"
            ));
            throw new IllegalArgumentException("Input must be a String or a List of Strings");
        }
    }

}
