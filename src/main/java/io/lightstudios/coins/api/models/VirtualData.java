package io.lightstudios.coins.api.models;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.VirtualResponse;
import io.lightstudios.coins.synchronisation.TransactionVirtual;
import io.lightstudios.core.util.LightNumbers;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class VirtualData {

    private File file;
    private String currencyName;
    private String displayName;
    private BigDecimal startingBalance;
    private int decimalPlaces;
    private String currencySymbolPlural;
    private String currencySymbolSingular;
    private BigDecimal maxBalance;

    private UUID playerUUID;
    private String playerName;
    private BigDecimal balance;

    private static final TransactionVirtual transactionVirtual = new TransactionVirtual();

    public VirtualData(File file, UUID uuid) {
        this.file = file;
        this.playerUUID = uuid;
        this.balance = new BigDecimal(0);

        transactionVirtual.setDelay(LightCoins.instance.getSettingsConfig().syncDelay());
        transactionVirtual.setPeriod(LightCoins.instance.getSettingsConfig().syncPeriod());
        transactionVirtual.startTransactions();

        readCurrencyFile();
    }

    private void readCurrencyFile() {
        // Read the currency file and set the values
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.currencyName = file.getName().replace(".yml", "");
        this.displayName = config.getString("displayName");
        this.startingBalance = BigDecimal.valueOf(config.getDouble("startBalance"));
        this.decimalPlaces = config.getInt("decimalPlaces");
        this.currencySymbolPlural = config.getString("currencyNamePlural");
        this.currencySymbolSingular = config.getString("currencyNameSingular");
        this.maxBalance = BigDecimal.valueOf(config.getDouble("maxBalance"));

    }


    /**
     * Add the balance of the player
     * @param amount the balance to set
     * @return VirtualResponse of the transaction
     */
    public VirtualResponse addBalance(BigDecimal amount) {
        VirtualResponse defaultResponse = checkDefaults(amount);

        if(!defaultResponse.transactionSuccess()) {
            return new VirtualResponse(amount, this.balance, defaultResponse.type, defaultResponse.errorMessage);
        }

        if(this.balance.add(amount).compareTo(this.maxBalance) > 0) {
            return new VirtualResponse(amount, this.balance,
                    VirtualResponse.VirtualResponseType.MAX_BALANCE_EXCEED, "Max balance exceeded");
        }

        this.balance = this.balance.add(amount);
        transactionVirtual.addTransaction(this);

        return new VirtualResponse(amount, this.balance, defaultResponse.type, defaultResponse.errorMessage);
    }

    /**
     * Subtract the balance of the player
     * @param amount the balance to set
     * @return VirtualResponse of the transaction
     */
    public VirtualResponse removeBalance(BigDecimal amount) {
        VirtualResponse defaultResponse = checkDefaults(amount);

        if(!defaultResponse.transactionSuccess()) {
            return new VirtualResponse(amount, this.balance, defaultResponse.type, defaultResponse.errorMessage);
        }

        if(!hasEnough(amount)) {
            return new VirtualResponse(amount, this.balance,
                    VirtualResponse.VirtualResponseType.NOT_ENOUGH, "Not enough balance");
        }

        this.balance = this.balance.subtract(amount);
        transactionVirtual.addTransaction(this);

        return new VirtualResponse(amount, this.balance, defaultResponse.type, defaultResponse.errorMessage);
    }

    public boolean hasEnough(BigDecimal amount) {
        return this.balance.compareTo(amount) >= 0;
    }


    /**
     * Check the default values of the transaction
     * @param amount the balance to add/subtract
     * @return VirtualResponse of the transaction
     */
    private VirtualResponse checkDefaults(BigDecimal amount) {

        if(amount.compareTo(BigDecimal.ZERO) < 0) {
            return new VirtualResponse(amount, this.balance,
                    VirtualResponse.VirtualResponseType.NOT_NEGATIVE, "Amount cannot be negative");
        }

        return new VirtualResponse(amount, this.balance,
                VirtualResponse.VirtualResponseType.SUCCESS, "");
    }

    public String getFormattedBalance() {
        return LightNumbers.formatForMessages(balance, decimalPlaces);
    }
    public String getFormattedCurrencySymbol() {
        return balance.compareTo(BigDecimal.ONE) == 0 ? currencySymbolSingular : currencySymbolPlural;
    }

}
