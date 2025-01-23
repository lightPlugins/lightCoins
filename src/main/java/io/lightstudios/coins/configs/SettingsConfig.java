package io.lightstudios.coins.configs;

import io.lightstudios.core.util.files.FileManager;
import io.lightstudios.core.util.libs.jedis.gears.resps.StreamTriggerInfo;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.List;

public class SettingsConfig {

    private FileConfiguration config;

    public SettingsConfig(FileManager fileManager) {
        this.config = fileManager.getConfig();
    }

    public String language() { return config.getString("language");}
    public int baltopCommandAmount() { return config.getInt("defaultCurrency.baltopCommandAmount");}
    public int payCommandCooldown() { return config.getInt("defaultCurrency.payCommandCooldown");}
    public String commandsCoins() { return config.getString("commands.coins");}
    public String commandsVirtual() { return config.getString("commands.virtual");}
    public String commandsBaltop() { return config.getString("commands.baltop");}
    public String commandsPay() { return config.getString("commands.pay");}
    public String placeholderFormat() { return config.getString("defaultCurrency.placeholderFormat");}
    public String defaultCurrencyName() { return config.getString("defaultCurrency.displayName");}
    public long syncDelay() { return config.getLong("multiTransactionSync.delay");}
    public long syncPeriod() { return config.getLong("multiTransactionSync.period");}
    public boolean enableDebugMultiSync() { return config.getBoolean("multiTransactionSync.enableDebug");}
    public BigDecimal defaultCurrencyStartBalance() {
        return BigDecimal.valueOf(config.getDouble("defaultCurrency.startBalance"));
    }
    public int defaultCurrencyDecimalPlaces() { return config.getInt("defaultCurrency.decimalPlaces");}
    public String defaultCurrencyNamePlural() { return config.getString("defaultCurrency.currencyNamePlural");}
    public String defaultCurrencyNameSingular() { return config.getString("defaultCurrency.currencyNameSingular");}
    public BigDecimal defaultCurrencyMaxBalance() {
        return BigDecimal.valueOf(config.getDouble("defaultCurrency.maxBalance"));
    }
    public boolean enableLoseCoinsOnDeath() { return config.getBoolean("loseCoinsOnDeath.enable");}
    public String loseCoinsBypassPermission() { return config.getString("loseCoinsOnDeath.bypassPermission");}
    public double loseCoinsPercentage() { return config.getDouble("loseCoinsOnDeath.percentage");}
    public double loseCoinsMinAmount() { return config.getDouble("loseCoinsOnDeath.minAmount");}
    public double loseCoinsMaxAmount() { return config.getDouble("loseCoinsOnDeath.maxAmount");}
    public List<String> loseCoinsBlacklistWorlds() { return config.getStringList("loseCoinsOnDeath.blacklist.worlds");}
}
