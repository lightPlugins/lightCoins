package io.lightstudios.coins.api.models;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import lombok.Getter;
import lombok.Setter;
import net.milkbowl.vault.economy.EconomyResponse;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
public class CoinsPlayer {

    private UUID uuid;
    private BigDecimal coins;
    private BigDecimal maxCoins;
    private String namePlural;
    private String nameSingular;
    private int decimalPlaces;

    public CoinsPlayer(UUID uuid) {
        this.uuid = uuid;
        this.maxCoins = LightCoins.instance.getSettingsConfig().defaultCurrencyMaxBalance();
        this.namePlural = LightCoins.instance.getSettingsConfig().defaultCurrencyNamePlural();
        this.nameSingular = LightCoins.instance.getSettingsConfig().defaultCurrencyNameSingular();
        this.decimalPlaces = LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces();
    }


    public EconomyResponse addCoins(BigDecimal coins) {

        LightCoins.instance.getConsolePrinter().printInfo("Adding coins ...");

        EconomyResponse defaultResponse = checkDefaults(coins);
        if(!defaultResponse.transactionSuccess()) {
            return new EconomyResponse(coins.doubleValue(), this.coins.doubleValue(),
                    defaultResponse.type, defaultResponse.errorMessage);
        }

        if(this.coins.add(coins).compareTo(this.maxCoins) > 0) {
            return new EconomyResponse(coins.doubleValue(), this.coins.doubleValue(),
                    EconomyResponse.ResponseType.FAILURE, "Max coins reached > " + this.maxCoins);
        }

        this.coins = this.coins.add(coins);
        return new EconomyResponse(coins.doubleValue(), this.coins.doubleValue(),
                EconomyResponse.ResponseType.SUCCESS, "");
    }

    public EconomyResponse removeCoins(BigDecimal coins) {

        EconomyResponse defaultResponse = checkDefaults(coins);

        if(!defaultResponse.transactionSuccess()) {
            return new EconomyResponse(coins.doubleValue(), this.coins.doubleValue(),
                    defaultResponse.type, defaultResponse.errorMessage);
        }

        if(!hasEnough(coins)) {
            return new EconomyResponse(coins.doubleValue(), this.coins.doubleValue(),
                    EconomyResponse.ResponseType.FAILURE, "Not enough coins.");
        }

        this.coins = this.coins.subtract(coins);
        return new EconomyResponse(coins.doubleValue(), this.coins.doubleValue(),
                EconomyResponse.ResponseType.SUCCESS, "");
    }

    public boolean isTownyAccount() {
        if(LightCore.instance.getHookManager().isExistTowny()) {
            return LightCore.instance.getHookManager().getTownyInterface().isTownyUUID(uuid);
        }
        return false;
    }

    public String getFormattedCoins() {
        return LightNumbers.formatForMessages(coins, decimalPlaces);
    }
    public String getFormattedCurrency() {
        return coins.compareTo(BigDecimal.ONE) == 0 ? nameSingular : namePlural;
    }
    public String getRawCoins() {
        return LightNumbers.formatForMessages(coins);
    }

    public boolean hasEnough(BigDecimal coins) {
        return this.coins.compareTo(coins) >= 0;
    }

    private EconomyResponse checkDefaults(BigDecimal coinsToAdd) {
        if (coinsToAdd.compareTo(BigDecimal.ZERO) <= 0) {
            return new EconomyResponse(coinsToAdd.doubleValue(), coins.doubleValue(),
                    EconomyResponse.ResponseType.FAILURE, "Cannot add negative or zero coins.");
        }
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.SUCCESS, "");
    }

}
