package io.lightstudios.coins.configs;

import io.lightstudios.core.util.files.FileManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;

public class SettingsConfig {

    private FileConfiguration config;

    public SettingsConfig(FileManager fileManager) {
        this.config = fileManager.getConfig();
    }

    public String language() { return config.getString("language");}
    public int baltopCommandAmount() { return config.getInt("defaultCurrency.baltopCommandAmount");}
    public String commandsCoins() { return config.getString("commands.coins");}
    public String commandsVirtual() { return config.getString("commands.virtual");}
    public String commandsBaltop() { return config.getString("commands.baltop");}
    public String commandsPay() { return config.getString("commands.pay");}
    public String coinsPlaceholder() { return config.getString("defaultCurrency.coinsPlaceholder");}
    public String defaultCurrencyName() { return config.getString("defaultCurrency.displayName");}
    public long syncDelay() { return config.getLong("multiTransactionSync.delay");}
    public long syncPeriod() { return config.getLong("multiTransactionSync.period");}
    public BigDecimal defaultCurrencyStartBalance() {
        return BigDecimal.valueOf(config.getDouble("defaultCurrency.startBalance"));
    }
    public int defaultCurrencyDecimalPlaces() { return config.getInt("defaultCurrency.decimalPlaces");}
    public String defaultCurrencyNamePlural() { return config.getString("defaultCurrency.currencyNamePlural");}
    public String defaultCurrencyNameSingular() { return config.getString("defaultCurrency.currencyNameSingular");}
    public BigDecimal defaultCurrencyMaxBalance() {
        return BigDecimal.valueOf(config.getDouble("defaultCurrency.maxBalance"));
    }
}
