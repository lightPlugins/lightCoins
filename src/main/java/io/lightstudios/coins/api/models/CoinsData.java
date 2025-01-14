package io.lightstudios.coins.api.models;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.synchronisation.TransactionCoins;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import io.lightstudios.core.util.relocations.jedis.Jedis;
import lombok.Getter;
import lombok.Setter;
import net.milkbowl.vault.economy.EconomyResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CoinsData {

    private UUID uuid;
    private String name;
    private BigDecimal currentCoins;
    private BigDecimal maxCoins;
    private String namePlural;
    private String nameSingular;
    private int decimalPlaces;

    private static final String REDIS_CHANNEL = "coinsDataUpdates";

    private static final TransactionCoins transactionManager = new TransactionCoins();

    public CoinsData(UUID uuid) {
        this.uuid = uuid;
        this.name = "unknown";
        this.currentCoins = new BigDecimal(0);
        this.maxCoins = LightCoins.instance.getSettingsConfig().defaultCurrencyMaxBalance();
        this.namePlural = LightCoins.instance.getSettingsConfig().defaultCurrencyNamePlural();
        this.nameSingular = LightCoins.instance.getSettingsConfig().defaultCurrencyNameSingular();
        this.decimalPlaces = LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces();

        transactionManager.setDelay(LightCoins.instance.getSettingsConfig().syncDelay());
        transactionManager.setPeriod(LightCoins.instance.getSettingsConfig().syncPeriod());
        transactionManager.startTransactions();

    }

    /**
     * Sets the coins for the player.
     * @param coins The amount of coins to set.
     * @return The response of the transaction.
     */
    public EconomyResponse setCoins(BigDecimal coins) {
        EconomyResponse defaultResponse = checkDefaults(coins);
        if(!defaultResponse.transactionSuccess()) {
            return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                    defaultResponse.type, defaultResponse.errorMessage);
        }

        this.currentCoins = coins;
        // Update the data in the database directly or through the transaction manager (redis)
        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
        LightCore.instance.getSettings().multiServerEnabled()) {
            LightCoins.instance.getCoinsTable().writeCoinsData(this).join();
        } else {
            if(LightCore.instance.isRedis) { sendUpdateToRedis(); }
            transactionManager.addTransaction(this);
        }

        return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                EconomyResponse.ResponseType.SUCCESS, "");
    }

    /**
     * Adds coins to the player's balance.
     * @param coins The amount of coins to add.
     * @return The response of the transaction.
     */
    public EconomyResponse addCoins(BigDecimal coins) {

        EconomyResponse defaultResponse = checkDefaults(coins);
        if(!defaultResponse.transactionSuccess()) {
            return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                    defaultResponse.type, defaultResponse.errorMessage);
        }

        if(this.currentCoins.add(coins).compareTo(this.maxCoins) > 0) {
            return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                    EconomyResponse.ResponseType.FAILURE, "Max coins reached: " + coins + " > " + this.maxCoins);
        }

        this.currentCoins = this.currentCoins.add(coins);
        // Update the data in the database directly or through the transaction manager (redis)
        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            LightCoins.instance.getCoinsTable().writeCoinsData(this).join();
        } else {
            if(LightCore.instance.isRedis) { sendUpdateToRedis(); }
            transactionManager.addTransaction(this);
        }

        return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                EconomyResponse.ResponseType.SUCCESS, "");
    }

    /**
     * Removes coins from the player's balance.
     * @param coins The amount of coins to remove.
     * @return The response of the transaction.
     */
    public EconomyResponse removeCoins(BigDecimal coins) {

        EconomyResponse defaultResponse = checkDefaults(coins);

        if(!defaultResponse.transactionSuccess()) {
            return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                    defaultResponse.type, defaultResponse.errorMessage);
        }

        if(!hasEnough(coins)) {
            return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                    EconomyResponse.ResponseType.FAILURE, "Not enough coins.");
        }

        this.currentCoins = this.currentCoins.subtract(coins);
        // Update the data in the database directly or through the transaction manager (redis)
        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            LightCoins.instance.getCoinsTable().writeCoinsData(this).join();
        } else {
            if(LightCore.instance.isRedis) { sendUpdateToRedis(); }
            transactionManager.addTransaction(this);
        }

        return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                EconomyResponse.ResponseType.SUCCESS, "");
    }

    /**
     * Get the formatted coins for messages.
     * @return The response of the transaction.
     */
    public String getFormattedCoins() {
        return LightNumbers.formatForMessages(currentCoins, LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces());
    }
    /**
     * Get the formatted currency (plural/singular) for messages.
     * @return The response of the transaction.
     */
    public String getFormattedCurrency() {
        return currentCoins.compareTo(BigDecimal.ONE) == 0 ? nameSingular : namePlural;
    }

    /**
     * Check if the player has enough coins.
     * @param coins The amount of coins to check.
     * @return The response of the transaction.
     */
    public boolean hasEnough(BigDecimal coins) {
        // Read the data in the database directly or through the locale cache manager (redis)
        if(LightCore.instance.getSettings().syncType().equalsIgnoreCase("mysql") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            CoinsData result = LightCoins.instance.getCoinsTable().findCoinsDataByUUID(uuid).join();
            return result.getCurrentCoins().compareTo(coins) >= 0;
        } else {
            return this.currentCoins.compareTo(coins) >= 0;
        }
    }

    private EconomyResponse checkDefaults(BigDecimal coinsToAdd) {
        if (coinsToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            return new EconomyResponse(coinsToAdd.doubleValue(), currentCoins.doubleValue(),
                    EconomyResponse.ResponseType.FAILURE, "Cannot add negative or zero coins.");
        }
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "");
    }

    /**
     * Sends the current coins data to the Redis server and
     * synchronizes the data with the other servers.
     */
    private void sendUpdateToRedis() {
        try (Jedis jedis = LightCore.instance.getRedisManager().getJedisPool().getResource()) {
            if (uuid == null || currentCoins == null) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "UUID, amount, or currentBalance cannot be null in CoinsData!",
                        "UUID: " + uuid,
                        "Current Coins: " + currentCoins,
                        "Could not send update to Redis. This behavior is unexpected",
                        "and you should report this to the plugin developer!"
                ));
                throw new IllegalArgumentException("UUID, amount, or currentCoins cannot be null");
            }
            String message = String.format("%s:%s:%s", this.uuid, this.name, this.currentCoins);
            jedis.publish(REDIS_CHANNEL, message);
        } catch (Exception e) {
            // Log the exception or handle it accordingly
            e.printStackTrace();

        }
    }
}
