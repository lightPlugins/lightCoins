package io.lightstudios.coins.configs;

import io.lightstudios.core.util.files.FileManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;

public class SettingsConfig {

    private FileConfiguration config;

    public SettingsConfig(FileManager fileManager) {
        this.config = fileManager.getConfig();
    }

    public String defaultCurrencyName() {return config.getString("defaultCurrency.displayName");}
    public BigDecimal defaultCurrencyStartBalance() {
        return BigDecimal.valueOf(config.getDouble("defaultCurrency.startBalance"));
    }
    public int defaultCurrencyDecimalPlaces() {return config.getInt("defaultCurrency.decimalPlaces");}
    public String defaultCurrencyNamePlural() {return config.getString("defaultCurrency.currencyNamePlural");}
    public String defaultCurrencyNameSingular() {return config.getString("defaultCurrency.currencyNameSingular");}
    public BigDecimal defaultCurrencyMaxBalance() {
        return BigDecimal.valueOf(config.getDouble("defaultCurrency.maxBalance"));
    }
}
