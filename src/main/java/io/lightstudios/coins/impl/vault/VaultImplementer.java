package io.lightstudios.coins.impl.vault;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.coins.impl.custom.LightCoinsDepositEvent;
import io.lightstudios.coins.impl.custom.LightCoinsWithdrawEvent;
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
import java.util.Map;
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

        if(uuid == null) {
            return false;
        }

        CoinsPlayer coinsPlayer = LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(uuid).getCoinsPlayer();

        return coinsPlayer != null;
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

        AtomicReference<CoinsPlayer> coinsPlayerRef = new AtomicReference<>(LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(uuid).getCoinsPlayer());

        if (coinsPlayerRef.get() == null) {
            LightCoins.instance.getConsolePrinter().printInfo(List.of(
                    "Could not find player data in cache for " + uuid,
                    "Attempting to retrieve player data from the database..."
            ));
            PlayerData playerData = new PlayerData();
            CoinsPlayer newCoinsPlayer = new CoinsPlayer(uuid);
            CompletableFuture<Map<UUID, BigDecimal>> future = LightCoins.instance.getCoinsTable()
                    .readCoins(uuid.toString());

            return future.thenApply(result -> {
                if (result != null) {
                    BigDecimal coins = result.get(uuid);
                    if (coins != null) {
                        newCoinsPlayer.setCoins(coins);
                        playerData.setCoinsPlayer(newCoinsPlayer);
                        LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);
                        LightCoins.instance.getConsolePrinter().printInfo("Successfully retrieved Player data from the database.");
                        return newCoinsPlayer.getCoins().doubleValue();
                    }
                } else {
                    LightCoins.instance.getConsolePrinter().printError(
                            "Failed to retrieve player data from the database.");
                    return 0.0;
                }
                return 0.0;
            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while reading player data from the database,",
                        "because CoinsPlayer Object in cache is null and can't retrieve data from database.",
                        "-> We can't find any related player data in the database from uuid " + uuid
                ));
                throwable.printStackTrace();
                return 0.0;
            }).join();
        }

        return coinsPlayerRef.get().getCoins().doubleValue();
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
        LightCoinsWithdrawEvent withdrawEvent = new LightCoinsWithdrawEvent(input, new BigDecimal(v));
        v = withdrawEvent.getAmount().doubleValue();

        if (withdrawEvent.isCancelled()) {
            return new EconomyResponse(v, v, EconomyResponse.ResponseType.FAILURE,
                    "Withdraw cancelled by LightVaultWithdrawEvent.");
        }

        UUID uuid = checkUUID(input);
        if (uuid == null) {
            return new EconomyResponse(v, v, EconomyResponse.ResponseType.FAILURE,
                    "Failed to withdraw coins. Invalid UUID format.");
        }

        final BigDecimal formatted = LightNumbers.formatBigDecimal(BigDecimal.valueOf(v));
        AtomicReference<CoinsPlayer> coinsPlayerRef = new AtomicReference<>(LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(uuid).getCoinsPlayer());

        if (coinsPlayerRef.get() == null) {
            LightCoins.instance.getConsolePrinter().printInfo(List.of(
                    "Could not find player data in cache for " + uuid,
                    "Attempting to retrieve player data from the database..."
            ));
            PlayerData playerData = new PlayerData();
            CoinsPlayer newCoinsPlayer = new CoinsPlayer(uuid);
            CompletableFuture<Map<UUID, BigDecimal>> future = LightCoins.instance.getCoinsTable()
                    .readCoins(uuid.toString());

            final double finalV = v;
            return future.thenApply(result -> {
                if (result != null) {
                    BigDecimal coins = result.get(uuid);
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    if (coins != null) {
                        newCoinsPlayer.setCoins(coins);
                        newCoinsPlayer.removeCoins(formatted);
                        playerData.setCoinsPlayer(newCoinsPlayer);
                        playerData.setPlayerName(offlinePlayer.getName());
                        playerData.setOfflinePlayer(offlinePlayer);
                        LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);
                        LightCoins.instance.getConsolePrinter().printInfo("Successfully retrieved Player data from the database.");

                        return new EconomyResponse(finalV, newCoinsPlayer.getCoins().doubleValue(),
                                EconomyResponse.ResponseType.SUCCESS, "Withdraw processed successfully.");
                    }
                } else {
                    LightCoins.instance.getConsolePrinter().printError(
                            "Failed to retrieve player data from the database.");
                    return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                            "Failed to retrieve player data from the database.");
                }
                return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                        "Failed to retrieve player data from the database.");
            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while reading player data from the database,",
                        "because CoinsPlayer Object in cache is null and can't retrieve data from database.",
                        "-> We can't find any related player data in the database from uuid " + uuid
                ));
                throwable.printStackTrace();
                return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                        "An error occurred while reading player data from the database.");
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
                    "Failed to deposit coins. Invalid UUID format.");
        }

        final BigDecimal formatted = LightNumbers.formatBigDecimal(BigDecimal.valueOf(v));
        LightCoinsDepositEvent depositEvent = new LightCoinsDepositEvent(uuid.toString(), formatted);

        if (depositEvent.isCancelled()) {
            return new EconomyResponse(v, v, EconomyResponse.ResponseType.FAILURE,
                    "Deposit cancelled by LightVaultDepositEvent.");
        }

        v = depositEvent.getAmount().doubleValue();

        AtomicReference<CoinsPlayer> coinsPlayerRef = new AtomicReference<>(LightCoins.instance.getLightCoinsAPI()
                .getPlayerData().get(uuid).getCoinsPlayer());

        final double finalV = v;
        if (coinsPlayerRef.get() == null) {
            LightCoins.instance.getConsolePrinter().printInfo(List.of(
                    "Could not find player data in cache for " + uuid,
                    "Attempting to retrieve player data from the database..."
            ));
            PlayerData playerData = new PlayerData();
            CoinsPlayer newCoinsPlayer = new CoinsPlayer(uuid);
            CompletableFuture<Map<UUID, BigDecimal>> future = LightCoins.instance.getCoinsTable()
                    .readCoins(uuid.toString());

            return future.thenApply(result -> {
                if (result != null) {
                    BigDecimal coins = result.get(uuid);
                    OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
                    if (coins != null) {
                        newCoinsPlayer.setCoins(coins);
                        newCoinsPlayer.addCoins(formatted);
                        playerData.setCoinsPlayer(newCoinsPlayer);
                        playerData.setPlayerName(offlinePlayer.getName());
                        playerData.setOfflinePlayer(offlinePlayer);
                        LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);
                        LightCoins.instance.getConsolePrinter().printInfo("Successfully retrieved Player data from the database.");

                        return new EconomyResponse(finalV, newCoinsPlayer.getCoins().doubleValue(),
                                EconomyResponse.ResponseType.SUCCESS, "Deposit processed successfully.");
                    }
                } else {
                    LightCoins.instance.getConsolePrinter().printError(
                            "Failed to retrieve player data from the database.");
                    return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                            "Failed to retrieve player data from the database.");
                }
                return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                        "Failed to retrieve player data from the database.");
            }).exceptionally(throwable -> {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while reading player data from the database,",
                        "because CoinsPlayer Object in cache is null and can't retrieve data from database.",
                        "-> We can't find any related player data in the database from uuid " + uuid
                ));
                throwable.printStackTrace();
                return new EconomyResponse(finalV, 0, EconomyResponse.ResponseType.FAILURE,
                        "An error occurred while reading player data from the database.");
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
        UUID uuid = checkUUID(input);

        if (uuid == null) {
            return false;
        }

        if (LightCoins.instance.getLightCoinsAPI().getPlayerData().containsKey(uuid)) {
            LightCoins.instance.getConsolePrinter().printInfo("Player data already exists for " + uuid);
            return false;
        }

        PlayerData playerData = new PlayerData();
        OfflinePlayer offlinePlayer = LightCore.instance.getServer().getOfflinePlayer(uuid);
        CoinsPlayer coinsPlayer = new CoinsPlayer(uuid);
        playerData.setUuid(uuid);
        playerData.setPlayerName(offlinePlayer.getPlayer() == null ? null : offlinePlayer.getName());
        playerData.setCoinsPlayer(coinsPlayer);

        LightCoins.instance.getLightCoinsAPI().getPlayerData().put(uuid, playerData);

        try {
            int result = LightCoins.instance.getCoinsTable().writeCoins(
                    uuid.toString(),
                    offlinePlayer.getPlayer() == null ? null : offlinePlayer.getName(),
                    new BigDecimal(0)).join();
            if (result == 1) {
                LightCoins.instance.getConsolePrinter().printInfo("New Player data created for " + uuid);
                return true;
            } else {
                LightCoins.instance.getConsolePrinter().printError("Failed to create player account for " + uuid);
                return false;
            }
        } catch (Exception e) {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while writing player data to the database!",
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

    // ########################### BANK METHODS ###########################
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
        UUID uuid;

        if (LightCore.instance.getHookManager().isExistTowny()) {
            TownyInterface townyInterface = LightCore.instance.getHookManager().getTownyInterface();
            UUID townyObjectUUID = townyInterface.getTownyObjectUUID(input);

            if (townyInterface.isTownyUUID(townyObjectUUID)) {
                uuid = townyObjectUUID;
            } else {
                try {
                    uuid = UUID.fromString(input);
                } catch (IllegalArgumentException e) {
                    LightCoins.instance.getConsolePrinter().printError(List.of(
                            "Failed to create player account for " + input,
                            "Invalid UUID format.",
                            "A third-party plugin is trying to create a player account with an invalid UUID.",
                            "Please report this to the target plugin developer. NOT LightCoins!"
                    ));
                    return null;
                }
            }
        } else {
            try {
                uuid = UUID.fromString(input);
            } catch (IllegalArgumentException e) {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "Failed to create player account for " + input,
                        "Invalid UUID format.",
                        "A third-party plugin is trying to create a player account with an invalid UUID.",
                        "Please report this to the target plugin developer. NOT LightCoins!"
                ));
                return null;
            }
        }

        return uuid;
    }

}
