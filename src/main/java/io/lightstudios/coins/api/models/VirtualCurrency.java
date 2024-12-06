package io.lightstudios.coins.api.models;

import io.lightstudios.coins.api.VirtualResponse;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class VirtualCurrency {

    private File file;
    private String currencyName;
    private String displayName;
    private BigDecimal startingBalance;
    private int decimalPlaces;
    private String currencySymbolPlural;
    private String currencySymbolSingular;
    private BigDecimal maxBalance;

    private UUID uuid;
    private BigDecimal balance;

    public VirtualCurrency(File file) {
        this.file = file;
        readCurrencyFile();
    }

    private void readCurrencyFile() {
        // Read the currency file and set the values
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.currencyName = file.getName().replace(".yml", "");
        this.displayName = config.getString("display-name");
        this.startingBalance = BigDecimal.valueOf(config.getDouble("start-balance"));
        this.decimalPlaces = config.getInt("decimal-places");
        this.currencySymbolPlural = config.getString("currency-name-plural");
        this.currencySymbolSingular = config.getString("currency-name-singular");
        this.maxBalance = BigDecimal.valueOf(config.getDouble("max-balance"));

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

}
