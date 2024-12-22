package io.lightstudios.coins.impl.vault;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.impl.events.custom.LightCoinsDepositEvent;
import io.lightstudios.coins.impl.events.custom.LightCoinsWithdrawEvent;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.hooks.towny.TownyInterface;
import io.lightstudios.core.util.LightNumbers;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

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
    public boolean hasAccount(String input) {
        UUID uuid = checkUUID(input);
        LightCoins.instance.getConsolePrinter().printError("Checking Account if exist: " + input + " -> " + uuid);

        if(uuid == null) {
            LightCoins.instance.getConsolePrinter().printError("UUID is null: " + input);
            return false;
        }
        return LightCoins.instance.getLightCoinsAPI().getPlayerData().get(uuid) != null;
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer) {
        return hasAccount(offlinePlayer.getUniqueId().toString());
    }

    @Override
    public boolean hasAccount(String input, String input1) {
        return hasAccount(input);
    }

    @Override
    public boolean hasAccount(OfflinePlayer offlinePlayer, String input) {
        return hasAccount(offlinePlayer);
    }

    @Override
    public double getBalance(String input) {
        UUID uuid = checkUUID(input);
        if (uuid == null) {
            return 0;
        }

        AtomicReference<CoinsData> coinsDataRef = new AtomicReference<>(LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(uuid).getCoinsData());

        if (coinsDataRef.get() == null) {
            LightCoins.instance.getConsolePrinter().printInfo(List.of(
                    "Could not find player data in cache for " + uuid,
                    "Attempting to retrieve player data from the database..."
            ));
            AccountData accountData = new AccountData();
            CoinsData newCoinsData = new CoinsData(uuid);
            CompletableFuture<CoinsData> future = LightCoins.instance.getCoinsTable()
                    .findCoinsDataByUUID(uuid);

            return future.thenApply(result -> {
                if (result != null) {
                    newCoinsData.setName(result.getName());
                    newCoinsData.setCoins(result.getCoins());
                    accountData.setCoinsData(newCoinsData);
                    LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, accountData);
                    LightCoins.instance.getConsolePrinter().printInfo("Successfully retrieved Player data from the database.");
                    return newCoinsData.getCoins().doubleValue();
                } else {
                    LightCoins.instance.getConsolePrinter().printError(
                            "Failed to retrieve account data from the database.");
                    return 0.0;
                }
            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while reading account data from the database,",
                        "because CoinsData Object in cache is null and can't retrieve data from database.",
                        "-> We can't find any related account data in the database from uuid " + uuid
                ));
                throwable.printStackTrace();
                return 0.0;
            }).join();
        }

        return coinsDataRef.get().getCoins().doubleValue();
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer) {
        return getBalance(offlinePlayer.getUniqueId().toString());
    }

    @Override
    public double getBalance(String input, String input1) {
        return getBalance(input);
    }

    @Override
    public double getBalance(OfflinePlayer offlinePlayer, String input) {
        return getBalance(offlinePlayer);
    }

    @Override
    public boolean has(String input, double v) {

        UUID uuid = checkUUID(input);
        if(uuid == null) {
            return false;
        }

        return getBalance(uuid.toString()) >= v;
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, double v) {
        return has(offlinePlayer.getUniqueId().toString(), v);
    }

    @Override
    public boolean has(String input, String input1, double v) {
        return has(input, v);
    }

    @Override
    public boolean has(OfflinePlayer offlinePlayer, String input, double v) {
        return has(offlinePlayer, v);
    }

    @Override
    public EconomyResponse withdrawPlayer(String input, double v) {

        UUID uuid = checkUUID(input);
        if (uuid == null) {
            return new EconomyResponse(v, v, EconomyResponse.ResponseType.FAILURE,
                    "Failed to withdraw coins. Invalid UUID format: " + input);
        }

        LightCoinsWithdrawEvent withdrawEvent = new LightCoinsWithdrawEvent(input, new BigDecimal(v));
        v = withdrawEvent.getAmount().doubleValue();

        if (withdrawEvent.isCancelled()) {
            return new EconomyResponse(v, v, EconomyResponse.ResponseType.FAILURE,
                    "Withdraw cancelled by LightCoinsWithdrawEvent.");
        }

        final BigDecimal formatted = LightNumbers.formatBigDecimal(BigDecimal.valueOf(v));
        AtomicReference<CoinsData> coinsPlayerRef = new AtomicReference<>(LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(uuid).getCoinsData());

        if (coinsPlayerRef.get() == null) {
            LightCoins.instance.getConsolePrinter().printInfo(List.of(
                    "Could not find account data in cache for " + uuid,
                    "Attempting to retrieve account data from the database..."
            ));
            AccountData playerData = new AccountData();
            CoinsData newCoinsPlayer = new CoinsData(uuid);
            CompletableFuture<CoinsData> future = LightCoins.instance.getCoinsTable()
                    .findCoinsDataByUUID(uuid);

            final double finalV = v;
            return future.thenApply(result -> {
                if (result != null) {
                    newCoinsPlayer.setName(result.getName());
                    newCoinsPlayer.setCoins(result.getCoins());
                    playerData.setCoinsData(newCoinsPlayer);
                    LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);
                    LightCoins.instance.getConsolePrinter().printInfo("Successfully retrieved account data from the database.");

                    return newCoinsPlayer.removeCoins(formatted);
                } else {
                    LightCoins.instance.getConsolePrinter().printError(
                            "Failed to retrieve account data from the database.");
                    return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                            "Failed to retrieve account data from the database.");
                }
            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while reading account data from the database,",
                        "because CoinsData Object in cache is null and can't retrieve data from database.",
                        "-> We can't find any related account data in the database from uuid " + uuid
                ));
                throwable.printStackTrace();
                return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                        "An error occurred while reading account data from the database.");
            }).join();
        }

        return coinsPlayerRef.get().removeCoins(formatted);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double v) {
        return withdrawPlayer(offlinePlayer.getUniqueId().toString(), v);
    }

    @Override
    public EconomyResponse withdrawPlayer(String input, String input1, double v) {
        return withdrawPlayer(input, v);
    }

    @Override
    public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, String input, double v) {
        return withdrawPlayer(offlinePlayer, v);
    }

    /**
     * Deposit coins to the player's account.
     * This method is called by LightCoins when a plugin tries to deposit coins to a player.
     * Can decide if the provided String input is a UUID or a TownyName.
     * <p>IMPORTANT: If the provided String is not a valid UUID, it will be treated as a TownyName.
     * If we cant read the TownUUID from the String input, we will return a failure response.</p>
     * <p>IMPORTANT: If the CoinsPlayer is null in cache, try to find them in the database and create new CoinsPlayer.
     * This methode must be synchronous, because we need the CoinsPlayer object to be created before we can add coins!</p>
     * @param input The uuid/townName to deposit coins to.
     * @param v The amount of coins to deposit.
     * @return The response of the deposit.
     */
    @Override
    public EconomyResponse depositPlayer(String input, double v) {
        UUID uuid = checkUUID(input);

        if (uuid == null) {
            return new EconomyResponse(v, v, EconomyResponse.ResponseType.FAILURE,
                    "Failed to deposit coins. Invalid UUID format: " + input);
        }

        final BigDecimal formatted = LightNumbers.formatBigDecimal(BigDecimal.valueOf(v));
        LightCoinsDepositEvent depositEvent = new LightCoinsDepositEvent(uuid.toString(), formatted);

        if (depositEvent.isCancelled()) {
            return new EconomyResponse(v, v, EconomyResponse.ResponseType.FAILURE,
                    "Deposit cancelled by LightCoinsDepositEvent.");
        }

        v = depositEvent.getAmount().doubleValue();

        AtomicReference<CoinsData> coinsPlayerRef = new AtomicReference<>(LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(uuid).getCoinsData());

        final double finalV = v;
        if (coinsPlayerRef.get() == null) {
            LightCoins.instance.getConsolePrinter().printInfo(List.of(
                    "Could not find account data in cache for " + uuid,
                    "Attempting to retrieve account data from the database..."
            ));
            AccountData playerData = new AccountData();
            CoinsData newCoinsPlayer = new CoinsData(uuid);
            CompletableFuture<CoinsData> future = LightCoins.instance.getCoinsTable()
                    .findCoinsDataByUUID(uuid);

            return future.thenApply(result -> {
                if (result != null) {
                    newCoinsPlayer.setName(result.getName());
                    newCoinsPlayer.setCoins(result.getCoins());
                    playerData.setCoinsData(newCoinsPlayer);
                    LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);
                    LightCoins.instance.getConsolePrinter().printInfo("Successfully retrieved account data from the database.");

                    return newCoinsPlayer.addCoins(formatted);
                } else {
                    LightCoins.instance.getConsolePrinter().printError(
                            "Failed to retrieve account data from the database.");
                    return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                            "Failed to retrieve account data from the database.");
                }
            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while reading account data from the database,",
                        "because CoinsData Object in cache is null and can't retrieve data from database.",
                        "-> We can't find any related account data in the database from uuid " + uuid
                ));
                throwable.printStackTrace();
                return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                        "An error occurred while reading account data from the database.");
            }).join();
        }

        return coinsPlayerRef.get().addCoins(formatted);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double v) {
        return depositPlayer(offlinePlayer.getUniqueId().toString(), v);
    }

    @Override
    public EconomyResponse depositPlayer(String input, String input1, double v) {
        return depositPlayer(input, v);
    }

    @Override
    public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, String input, double v) {
        return depositPlayer(offlinePlayer, v);
    }

    @Override
    public List<String> getBanks() {
        return List.of();
    }

    /**
     * Create a new player account with the provided UUID.
     * <p>IMPORTANT: This methode must be sync do third party plugins
     * need the information. Otherwise, the main thread will be freezing.</p>
     * @param input The UUID of the player to create an account for.
     * @return true if the account was created successfully.
     **/
    @Override
    public boolean createPlayerAccount(String input) {

        LightCoins.instance.getConsolePrinter().printError("Creating Account: " + input);
        UUID uuid = checkUUID(input);

        if (uuid == null) {
            LightCoins.instance.getConsolePrinter().printError("UUID is null");
            return false;
        }

        if (LightCoins.instance.getLightCoinsAPI().getPlayerData().containsKey(uuid)) {
            LightCoins.instance.getConsolePrinter().printInfo("Coins data already exists for " + uuid);
            return false;
        }

        boolean isTownyAccount;
        if(LightCore.instance.getHookManager().isExistTowny()) {
            TownyInterface townyInterface = LightCore.instance.getHookManager().getTownyInterface();
            isTownyAccount = townyInterface.isTownyUUID(uuid);
            LightCoins.instance.getConsolePrinter().printInfo("Account creation for Towny with uuid: " + uuid);
        } else {
            isTownyAccount = false;
        }
        AccountData accountData = new AccountData();
        OfflinePlayer offlinePlayer = Bukkit.getServer().getOfflinePlayer(uuid);
        CoinsData coinsData = new CoinsData(uuid);
        accountData.setUuid(uuid);
        accountData.setName(isTownyAccount ? "towny_account" : offlinePlayer.getName());
        accountData.setOfflinePlayer(offlinePlayer);
        coinsData.setName(isTownyAccount ? "towny_account" : offlinePlayer.getName());
        accountData.setCoinsData(coinsData);
        try {
            int result = LightCoins.instance.getCoinsTable().writeCoinsData(coinsData).join();
            if (result == 1) {
                LightCoins.instance.getConsolePrinter().printInfo("New account data created for " + uuid);
                LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, accountData);
                return true;
            } else {
                LightCoins.instance.getConsolePrinter().printError("Failed to create account account for " + uuid);
                return false;
            }
        } catch (Exception e) {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while writing account data to the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer) {
        return createPlayerAccount(offlinePlayer.getUniqueId().toString());
    }

    @Override
    public boolean createPlayerAccount(String input, String input1) {
        return createPlayerAccount(input);
    }

    @Override
    public boolean createPlayerAccount(OfflinePlayer offlinePlayer, String input) {
        return createPlayerAccount(offlinePlayer);
    }

    // ########################### BANK METHODS ############################
    // #####################################################################

    @Override
    public EconomyResponse createBank(String input, String input1) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse createBank(String input, OfflinePlayer offlinePlayer) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse deleteBank(String input) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse bankBalance(String input) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse bankHas(String input, double v) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse bankWithdraw(String input, double v) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse bankDeposit(String input, double v) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse isBankOwner(String input, String input1) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse isBankOwner(String input, OfflinePlayer offlinePlayer) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse isBankMember(String input, String input1) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    @Override
    public EconomyResponse isBankMember(String input, OfflinePlayer offlinePlayer) {
        return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
                "Bank creation is not supported by LightCoins.");
    }

    /**
     * Check if the provided String is a valid UUID.
     * Inclusive check if the provided String is a valid TownyName
     * and convert it to a UUID.
     * @param input The String to check.
     * @return The UUID if the String is a valid UUID or TownyName.
     */
    @Nullable
    private UUID checkUUID(String input) {
        try {
            return UUID.fromString(input);
        } catch (IllegalArgumentException ignored) {
            // Problem with TownyInterface -> AccountHolder returns null
            if (LightCore.instance.getHookManager().isExistTowny()) {
                LightCoins.instance.getConsolePrinter().printInfo("Input is not a valid UUID, try to check if this is a town name: " + input);
                TownyInterface townyInterface = LightCore.instance.getHookManager().getTownyInterface();
                UUID townyObjectUUID = townyInterface.getTownyObjectUUID(input);
                if (townyInterface.isTownyUUID(townyObjectUUID)) {
                    LightCoins.instance.getConsolePrinter().printInfo("Found town uuid: " + townyObjectUUID + " provided by " + input);
                    return townyObjectUUID;
                }
            }
        }

        return null;
    }

}
