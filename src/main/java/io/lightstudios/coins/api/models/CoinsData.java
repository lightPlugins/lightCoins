package io.lightstudios.coins.api.models;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.synchronisation.TransactionCoins;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import lombok.Getter;
import lombok.Setter;
import net.milkbowl.vault.economy.EconomyResponse;

import java.math.BigDecimal;
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

    public EconomyResponse setCoins(BigDecimal coins) {
        EconomyResponse defaultResponse = checkDefaults(coins);
        if(!defaultResponse.transactionSuccess()) {
            return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                    defaultResponse.type, defaultResponse.errorMessage);
        }

        this.currentCoins = coins;
        transactionManager.addTransaction(this);
        return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                EconomyResponse.ResponseType.SUCCESS, "");
    }


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
        transactionManager.addTransaction(this);
        return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                EconomyResponse.ResponseType.SUCCESS, "");
    }

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
        transactionManager.addTransaction(this);
        return new EconomyResponse(coins.doubleValue(), this.currentCoins.doubleValue(),
                EconomyResponse.ResponseType.SUCCESS, "");
    }

    public boolean isTownyAccount() {
        if(LightCore.instance.getHookManager().isExistTowny()) {
            return LightCore.instance.getHookManager().getTownyInterface().isTownyUUID(uuid);
        }
        return false;
    }

    public String getFormattedCoins() {
        return LightNumbers.formatForMessages(currentCoins, LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces());
    }
    public String getFormattedCurrency() {
        return currentCoins.compareTo(BigDecimal.ONE) == 0 ? nameSingular : namePlural;
    }

    public boolean hasEnough(BigDecimal coins) {
        return this.currentCoins.compareTo(coins) >= 0;
    }

    private EconomyResponse checkDefaults(BigDecimal coinsToAdd) {
        if (coinsToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            return new EconomyResponse(coinsToAdd.doubleValue(), currentCoins.doubleValue(),
                    EconomyResponse.ResponseType.FAILURE, "Cannot add negative or zero coins.");
        }
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "");
    }
}
