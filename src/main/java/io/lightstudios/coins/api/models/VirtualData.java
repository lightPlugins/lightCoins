package io.lightstudios.coins.api.models;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.VirtualResponse;
import io.lightstudios.coins.synchronisation.TransactionVirtual;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.libs.jedis.Jedis;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class VirtualData {

    private File file;
    private String currencyName;
    private String displayName;
    private String placeholderFormat;
    private BigDecimal startingBalance;
    private int decimalPlaces;
    private String currencySymbolPlural;
    private String currencySymbolSingular;
    private BigDecimal maxBalance;

    private UUID playerUUID;
    private String playerName;
    private BigDecimal currentBalance;

    private static final String REDIS_CHANNEL = "virtualDataUpdates";

    private static final TransactionVirtual transactionVirtual = new TransactionVirtual();

    public VirtualData(File file, UUID uuid) {
        this.file = file;
        this.playerUUID = uuid;
        this.currentBalance = new BigDecimal(0);

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
        this.placeholderFormat = config.getString("placeholderFormat");

    }

    /**
     * Sets the balance of the player
     * @param amount the balance to set
     * @return VirtualResponse of the transaction
     */
    public VirtualResponse setBalance(BigDecimal amount) {
        VirtualResponse defaultResponse = checkDefaults(amount);

        if(!defaultResponse.transactionSuccess()) {
            return new VirtualResponse(amount, this.currentBalance, defaultResponse.type, defaultResponse.errorMessage);
        }

        if(amount.compareTo(this.maxBalance) > 0) {
            return new VirtualResponse(amount, this.currentBalance,
                    VirtualResponse.VirtualResponseType.MAX_BALANCE_EXCEED, "Max balance exceeded");
        }

        this.currentBalance = amount;

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            LightCoins.instance.getVirtualDataTable().writeVirtualData(this).join();
        } else {
            if(LightCore.instance.isRedis) { sendUpdateToRedis(); }
            transactionVirtual.addTransaction(this);
        }

        return new VirtualResponse(amount, this.currentBalance, defaultResponse.type, defaultResponse.errorMessage);
    }

    /**
     * Add the balance of the player
     * @param amount the balance to set
     * @return VirtualResponse of the transaction
     */
    public VirtualResponse addBalance(BigDecimal amount) {
        VirtualResponse defaultResponse = checkDefaults(amount);

        if(!defaultResponse.transactionSuccess()) {
            return new VirtualResponse(amount, this.currentBalance, defaultResponse.type, defaultResponse.errorMessage);
        }

        if(this.currentBalance.add(amount).compareTo(this.maxBalance) > 0) {
            return new VirtualResponse(amount, this.currentBalance,
                    VirtualResponse.VirtualResponseType.MAX_BALANCE_EXCEED, "Max balance exceeded");
        }

        this.currentBalance = this.currentBalance.add(amount);

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            LightCoins.instance.getVirtualDataTable().writeVirtualData(this).join();
        } else {
            if(LightCore.instance.isRedis) { sendUpdateToRedis(); }
            transactionVirtual.addTransaction(this);
        }

        return new VirtualResponse(amount, this.currentBalance, defaultResponse.type, defaultResponse.errorMessage);
    }

    /**
     * Subtract the balance of the player
     * @param amount the balance to set
     * @return VirtualResponse of the transaction
     */
    public VirtualResponse removeBalance(BigDecimal amount) {
        VirtualResponse defaultResponse = checkDefaults(amount);

        if(!defaultResponse.transactionSuccess()) {
            return new VirtualResponse(amount, this.currentBalance, defaultResponse.type, defaultResponse.errorMessage);
        }

        if(!hasEnough(amount)) {
            return new VirtualResponse(amount, this.currentBalance,
                    VirtualResponse.VirtualResponseType.NOT_ENOUGH, "Not enough balance");
        }

        this.currentBalance = this.currentBalance.subtract(amount);

        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            LightCoins.instance.getVirtualDataTable().writeVirtualData(this).join();
        } else {
            if(LightCore.instance.isRedis) { sendUpdateToRedis(); }
            transactionVirtual.addTransaction(this);
        }

        return new VirtualResponse(amount, this.currentBalance, defaultResponse.type, defaultResponse.errorMessage);
    }

    /**
     * Check if the player has enough balance
     * @param amount the balance to check
     * @return boolean if the player has enough balance
     */
    public boolean hasEnough(BigDecimal amount) {
        if (LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            List<VirtualData> result = LightCoins.instance.getVirtualDataTable().findVirtualDataByUUID(this.playerUUID).join();

            return result.stream()
                    .filter(virtualData -> virtualData.getCurrencyName().equals(this.currencyName))
                    .anyMatch(virtualData -> virtualData.getCurrentBalance().compareTo(amount) >= 0);
        } else {
            return this.currentBalance.compareTo(amount) >= 0;
        }
    }

    /**
     * Check the default values of the transaction
     * @param amount the balance to add/subtract
     * @return VirtualResponse of the transaction
     */
    private VirtualResponse checkDefaults(BigDecimal amount) {

        if(amount.compareTo(BigDecimal.ZERO) < 0) {
            return new VirtualResponse(amount, this.currentBalance,
                    VirtualResponse.VirtualResponseType.NOT_NEGATIVE, "Amount cannot be negative");
        }

        return new VirtualResponse(amount, this.currentBalance,
                VirtualResponse.VirtualResponseType.SUCCESS, "");
    }

    /**
     * Get the formatted balance
     * @return String of the formatted balance
     */
    public String getFormattedBalance() {
        return LightNumbers.formatForMessages(currentBalance, decimalPlaces);
    }

    /**
     * Get the formatted currency (plural/singular) symbol
     * @return String of the formatted currency symbol
     */
    public String getFormattedCurrencySymbol() {
        return currentBalance.compareTo(BigDecimal.ONE) == 0 ? currencySymbolSingular : currencySymbolPlural;
    }

    /**
     * Sends the current balance data to the Redis server and
     * synchronizes the data with the other servers.
     */
    private void sendUpdateToRedis() {
        try (Jedis jedis = LightCore.instance.getRedisManager().getJedisPool().getResource()) {
            if (playerUUID == null || currentBalance == null) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "UUID, amount, or currentBalance cannot be null in VirtualData!",
                        "UUID: " + playerUUID,
                        "Current Balance: " + currentBalance,
                        "Could not send update to Redis. This behavior is unexpected",
                        "and you should report this to the plugin developer!"
                ));
                throw new IllegalArgumentException("UUID, amount, or currentBalance cannot be null");
            }
            // message format: uuid:name:currency:balance
            String message = String.format(
                    "%s:%s:%s:%s",
                    this.playerUUID, this.playerName, this.currencyName, this.currentBalance);
            jedis.publish(REDIS_CHANNEL, message);
        } catch (Exception e) {
            // Log the exception or handle it accordingly
            e.printStackTrace();

        }
    }
}
