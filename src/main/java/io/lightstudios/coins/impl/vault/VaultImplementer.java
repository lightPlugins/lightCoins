package io.lightstudios.coins.impl.vault;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.hooks.towny.TownyInterface;
import io.lightstudios.core.util.LightNumbers;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class VaultImplementer implements Economy {

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public String getName() {
        return "LightCoins";
    }

    @Override
    public boolean hasBankSupport() {
        return false;
    }

    @Override
    public int fractionalDigits() {
        return LightCoins.instance.getSettingsConfig().defaultCurrencyDecimalPlaces();
    }

    @Override
    public String format(double v) {
        return LightNumbers.formatForMessages(BigDecimal.valueOf(v), fractionalDigits());
    }

    @Override
    public String currencyNamePlural() {
        return LightCoins.instance.getSettingsConfig().defaultCurrencyNamePlural();
    }

    @Override
    public String currencyNameSingular() {
        return LightCoins.instance.getSettingsConfig().defaultCurrencyNameSingular();
    }

    @Override
    public boolean hasAccount(String s) {
        return false;
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer) {

        CoinsPlayer coinsPlayer = LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(offlinePlayer.getUniqueId()).getCoinsPlayer();

        return coinsPlayer != null;
    }

    @Override
    public boolean hasAccount(String s, String s1) {
        return false;
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer, String s) {
        return false;
    }

    @Override
    public double getBalance(String s) {
        return 0;
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer) {
        CoinsPlayer coinsPlayer = LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(offlinePlayer.getUniqueId()).getCoinsPlayer();
        return coinsPlayer.getCoins().doubleValue();
    }

    @Override
    public double getBalance(String s, String s1) {
        return 0;
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer, String s) {
        return 0;
    }

    @Override
    public boolean has(String s, double v) {
        return false;
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, double v) {
        CoinsPlayer coinsPlayer = LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(offlinePlayer.getUniqueId()).getCoinsPlayer();
        return coinsPlayer.hasEnough(BigDecimal.valueOf(v));
    }

    @Override
    public boolean has(String s, String s1, double v) {
        return false;
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, String s, double v) {
        return false;
    }

    @Override
    public EconomyResponse withdrawPlayer(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double v) {
        CoinsPlayer coinsPlayer = LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(offlinePlayer.getUniqueId()).getCoinsPlayer();
        return coinsPlayer.removeCoins(BigDecimal.valueOf(v));
    }

    @Override
    public EconomyResponse withdrawPlayer(String s, String s1, double v) {
        return null;
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse depositPlayer(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double v) {
        CoinsPlayer coinsPlayer = LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(offlinePlayer.getUniqueId()).getCoinsPlayer();
        return coinsPlayer.addCoins(BigDecimal.valueOf(v));
    }

    @Override
    public EconomyResponse depositPlayer(String s, String s1, double v) {
        return null;
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse createBank(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse createBank(String s, OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public EconomyResponse deleteBank(String s) {
        return null;
    }

    @Override
    public EconomyResponse bankBalance(String s) {
        return null;
    }

    @Override
    public EconomyResponse bankHas(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse bankWithdraw(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse bankDeposit(String s, double v) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse isBankOwner(String s, OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String s, String s1) {
        return null;
    }

    @Override
    public EconomyResponse isBankMember(String s, OfflinePlayer offlinePlayer) {
        return null;
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    @Override
    public boolean createPlayerAccount(String s) {
        UUID uuid;

        if (LightCore.instance.getHookManager().isExistTowny()) {
            TownyInterface townyInterface = LightCore.instance.getHookManager().getTownyInterface();
            UUID townyObjectUUID = townyInterface.getTownyObjectUUID(s);

            if (townyInterface.isTownyUUID(townyObjectUUID)) {
                uuid = townyObjectUUID;
            } else {
                try {
                    uuid = UUID.fromString(s);
                } catch (IllegalArgumentException e) {
                    LightCore.instance.getConsolePrinter().printError(List.of(
                            "Failed to create player account for " + s,
                            "Invalid UUID format.",
                            "A third-party plugin is trying to create a player account with an invalid UUID.",
                            "Please report this to the target plugin developer. NOT LightCoins!"
                    ));
                    return false;
                }
            }
        } else {
            try {
                uuid = UUID.fromString(s);
            } catch (IllegalArgumentException e) {
                LightCore.instance.getConsolePrinter().printError(List.of(
                        "Failed to create player account for " + s,
                        "Invalid UUID format.",
                        "A third-party plugin is trying to create a player account with an invalid UUID.",
                        "Please report this to the target plugin developer. NOT LightCoins!"
                ));
                return false;
            }
        }

        if (LightCoins.instance.getLightCoinsAPI().getPlayerData().containsKey(uuid)) {
            LightCore.instance.getConsolePrinter().printInfo("Player data already exists for " + uuid);
            return false;
        }
        PlayerData playerData = new PlayerData();
        CoinsPlayer coinsPlayer = new CoinsPlayer(uuid);
        playerData.setUuid(uuid);
        coinsPlayer.setCoins(LightCoins.instance.getSettingsConfig().defaultCurrencyStartBalance());
        playerData.setCoinsPlayer(coinsPlayer);


        LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);
        LightCoins.instance.getCoinsTable().writeCoins(uuid.toString(), new BigDecimal(10))
                .thenAccept(result -> {
                    if (result == 1) {
                        LightCore.instance.getConsolePrinter().printInfo("New Player data created for " + uuid);
                    } else {
                        LightCore.instance.getConsolePrinter().printError("Failed to create player account for " + uuid);
                    }
                }).exceptionally(throwable -> {
                    LightCore.instance.getConsolePrinter().printError(List.of(
                            "An error occurred while writing player data to the database!",
                            "Please check the error logs for more information."
                    ));
                    throwable.printStackTrace();
                    return null;
                });

        return true;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
        return createPlayerAccount(offlinePlayer.getUniqueId().toString());
    }

    @Override
    public boolean createPlayerAccount(String s, String s1) {
        createPlayerAccount(s);
        return false;
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String s) {
        createPlayerAccount(offlinePlayer);
        return false;
    }
}
