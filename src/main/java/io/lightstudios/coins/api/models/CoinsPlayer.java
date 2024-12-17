package io.lightstudios.coins.api.models;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.impl.custom.LightCoinsDepositEvent;
import io.lightstudios.coins.impl.custom.LightCoinsWithdrawEvent;
import io.lightstudios.coins.synchronisation.TransactionCoins;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.LightNumbers;
import lombok.Getter;
import lombok.Setter;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

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

    private static final TransactionCoins transactionManager = new TransactionCoins();

    public CoinsPlayer(UUID uuid) {
        this.uuid = uuid;
        this.coins = new BigDecimal(0);
        this.maxCoins = LightCoins.instance.getSettingsConfig().defaultCurrencyMaxBalance();
        this.namePlural = LightCoins.instance.getSettingsConfig().defaultCurrencyNamePlural();
        this.nameSingular = LightCoins.instance.getSettingsConfig().defaultCurrencyNameSingular();
        this.decimalPlaces = LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces();
    }


    public EconomyResponse addCoins(BigDecimal coins) {

        EconomyResponse defaultResponse = checkDefaults(coins);
        if(!defaultResponse.transactionSuccess()) {
            return new EconomyResponse(coins.doubleValue(), this.coins.doubleValue(),
                    defaultResponse.type, defaultResponse.errorMessage);
        }

        if(this.coins.add(coins).compareTo(this.maxCoins) > 0) {
            return new EconomyResponse(coins.doubleValue(), this.coins.doubleValue(),
                    EconomyResponse.ResponseType.FAILURE, "Max coins reached > " + this.maxCoins);
        }

        OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(this.uuid);

        this.coins = this.coins.add(coins);
        transactionManager.addTransaction(
                this.uuid,
                offlinePlayer.getPlayer() == null ? null : offlinePlayer.getName(),
                this.coins);
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

        OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(this.uuid);

        this.coins = this.coins.subtract(coins);
        transactionManager.addTransaction(
                this.uuid,
                offlinePlayer.getPlayer() == null ? null : offlinePlayer.getName(),
                this.coins);
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
