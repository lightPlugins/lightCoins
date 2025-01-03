package io.lightstudios.coins.api;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.coins.impl.vault.VaultImplementerSingle;
import lombok.Getter;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Getter
public class LightCoinsAPI {

    private final HashMap<UUID, AccountData> accountData = new HashMap<>();

    /**
     * Returns the VaultImplementer instance.
     * This instance is used to interact with the Vault API over the LightCoins plugin.
     * @return the VaultImplementer instance
     * @Author LightStudios
     */
    public VaultImplementerSingle getImplementer() {
        return LightCoins.instance.getVaultImplementer();
    }

    /**
     * Returns the account data for the player.
     * @param player the OfflinePlayer Object to get the account data for
     * @return the account data for the player
     * @Author LightStudios
     */
    @Nullable
    public AccountData getAccountData(OfflinePlayer player) {
        return accountData.get(player.getUniqueId());
    }

    /**
     * Returns the account data for the player.
     * @param player the Player Object to get the account data for
     * @return the account data for the player
     * @Author LightStudios
     */
    @Nullable
    public AccountData getAccountData(Player player) {
        return accountData.get(player.getUniqueId());
    }

    /**
     * Returns the account data for the player.
     * @param uuid the players UUID
     * @return the account data for the player
     * @Author LightStudios
     */
    @Nullable
    public AccountData getAccountData(UUID uuid) {
        return accountData.get(uuid);
    }

    /**
     * Returns the account data for the player.
     * @param playerName the players name
     * @return the account data for the player
     * @Author LightStudios
     */
    @Nullable
    public AccountData getAccountData(String playerName) {
        for(AccountData data : accountData.values()) {
            if(data.getName() != null && data.getName().equalsIgnoreCase(playerName)) {
                return data;
            }
        }
        return null;
    }

    /**
     * Returns a list of all player names that have account data.
     * @return a list of player names
     * @Author LightStudios
     */
    public List<String> getAccountDataPlayerNames() {
        List<String> names = new ArrayList<>();
        for(AccountData data : accountData.values()) {
            if(data.getName() != null && !data.getName().equalsIgnoreCase("nonplayer_account")) {
                names.add(data.getName());
            }
        }

        return names;

    }

    /**
     * Creates a new account data for the player async.
     * If the account data already exists, it will return the existing account data immediately.
     * @param instance the plugin instance, used for logging
     * @param uuid the accounts UUID
     * @param name the accounts name
     * @return a CompletableFuture that completes with the created AccountData
     * @Author LightStudios
     */
    public CompletableFuture<AccountData> createAccountDataAsync(JavaPlugin instance, UUID uuid, String name) {
        // Check if the account data is in the local storage (cache)
        if (accountData.containsKey(uuid)) {
            return CompletableFuture.completedFuture(accountData.get(uuid));
        }

        String calledBy = instance.getDescription().getName();
        AccountData data = new AccountData();
        data.setName(name);
        data.setUuid(uuid);
        CoinsData coinsData = new CoinsData(uuid);
        coinsData.setName(name);
        coinsData.setUuid(uuid);
        data.setCoinsData(coinsData);
        List<VirtualData> virtualDataList = new ArrayList<>();

        // Check if the account data is in the database
        return LightCoins.instance.getCoinsTable().findCoinsDataByUUID(uuid)
                .thenCompose(existingCoinsData -> {
                    if (existingCoinsData != null) {
                        // If coins data is found in the database, create AccountData from it
                        AccountData existingAccountData = new AccountData();
                        existingAccountData.setUuid(existingCoinsData.getUuid());
                        existingAccountData.setName(existingCoinsData.getName());
                        existingAccountData.setCoinsData(existingCoinsData);

                        // Read VirtualData from the database
                        return LightCoins.instance.getVirtualDataTable().findVirtualDataByUUID(uuid)
                                .thenApply(existingVirtualDataList -> {
                                    if (existingVirtualDataList != null) {
                                        existingAccountData.setVirtualCurrencies(existingVirtualDataList);
                                    }
                                    // Load it into the cache and return it
                                    accountData.put(uuid, existingAccountData);
                                    return existingAccountData;
                                });
                    } else {
                        // If account data is not found in the database, create new account data
                        List<CompletableFuture<Void>> virtualDataFutures = new ArrayList<>();
                        for (File file : LightCoins.instance.getVirtualCurrencyFiles().getYamlFiles()) {
                            VirtualData virtualData = new VirtualData(file, uuid);
                            CompletableFuture<Void> future = LightCoins.instance.getVirtualDataTable().writeVirtualData(virtualData)
                                    .thenAccept(result -> {
                                        if (result > 0) {
                                            LightCoins.instance.getConsolePrinter().printInfo("Successfully created via §c" + calledBy + "§r new virtual currency for " + name);
                                            virtualDataList.add(virtualData);
                                        } else {
                                            LightCoins.instance.getConsolePrinter().printError("Failed to create via §4" + calledBy + "§c new virtual currency for " + name);
                                        }
                                    });
                            virtualDataFutures.add(future);
                        }

                        return CompletableFuture.allOf(virtualDataFutures.toArray(new CompletableFuture[0]))
                                .thenCompose(v -> {
                                    data.setVirtualCurrencies(virtualDataList);
                                    return LightCoins.instance.getCoinsTable().writeCoinsData(coinsData);
                                })
                                .thenApply(result -> {
                                    if (result > 0) {
                                        LightCoins.instance.getConsolePrinter().printInfo("Successfully created via §c" + calledBy + "§r new account data for " + name);
                                        accountData.put(uuid, data);
                                        return data;
                                    } else {
                                        LightCoins.instance.getConsolePrinter().printError("Failed to create via §4" + calledBy + "§c new account data for " + name);
                                        throw new CompletionException(new Exception("Failed to write coins data to the database"));
                                    }
                                });
                    }
                })
                .exceptionally(ex -> {
                    LightCoins.instance.getConsolePrinter().printError("An error occurred while creating account data: " + ex.getMessage());
                    return null;
                });
    }
}
