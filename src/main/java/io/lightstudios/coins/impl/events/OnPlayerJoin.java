package io.lightstudios.coins.impl.events;

import io.lightstudios.coins.LightCoins;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.core.LightCore;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class OnPlayerJoin implements Listener {

    // ensure the player data is loaded before the player joins the server
    // and other plugins can access the player data from the LightCoinsAPI
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLogin(PlayerJoinEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        AccountData accountData = LightCoins.instance.getLightCoinsAPI().getAccountData().get(uuid);

        if (accountData != null) {
            LightCoins.instance.getConsolePrinter().printInfo("Player data already exists for " + uuid);
            LightCoins.instance.getConsolePrinter().printInfo("Handling virtual currencies for " + uuid);
            handleVirtualCurrencies(uuid, accountData);
            return;
        }

        LightCoins.instance.getCoinsTable().findCoinsDataByUUID(uuid).thenAcceptAsync(coinsData -> {
            AccountData playerData = new AccountData();
            if (coinsData == null) {
                LightCoins.instance.getConsolePrinter().printInfo("Creating new player account for " + uuid);
                createNewPlayerAccount(event, uuid, playerData);
            } else {
                LightCoins.instance.getConsolePrinter().printInfo("Loading existing player data for " + uuid);
                loadExistingPlayerData(event, uuid, coinsData, playerData);
            }
            handleVirtualCurrencies(uuid, playerData);
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while handling player data for UUID: " + uuid,
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            return null;
        });
    }

    private void createNewPlayerAccount(PlayerJoinEvent event, UUID uuid, AccountData playerData) {
        if (!LightCoins.instance.getVaultImplementer().createPlayerAccount(uuid.toString())) {
            LightCoins.instance.getConsolePrinter().printError("Failed to create player account for " + uuid);
        }

        BigDecimal starterCoins = LightCoins.instance.getSettingsConfig().defaultCurrencyStartBalance();
        CoinsData newCoinsData = new CoinsData(uuid);
        newCoinsData.setName(event.getPlayer().getName());
        playerData.setCoinsData(newCoinsData);
        playerData.setName(event.getPlayer().getName());
        playerData.setOfflinePlayer(event.getPlayer());
        LightCoins.instance.getLightCoinsAPI().getAccountData().put(uuid, playerData);

        EconomyResponse response = newCoinsData.addCoins(starterCoins);
        if (response.transactionSuccess()) {
            LightCore.instance.getMessageSender().sendPlayerMessage(
                    event.getPlayer(),
                    LightCoins.instance.getMessageConfig().prefix() +
                            LightCoins.instance.getMessageConfig().starterCoins().stream().map(str -> str
                                    .replace("#coins#", String.valueOf(starterCoins))
                                    .replace("#currency#", starterCoins.compareTo(BigDecimal.ONE) == 0
                                            ? newCoinsData.getNameSingular()
                                            : newCoinsData.getNamePlural())
                            ).collect(Collectors.joining()));
        } else {
            LightCoins.instance.getConsolePrinter().printError("Transaction failed: " + response.errorMessage);
        }
    }

    private void loadExistingPlayerData(PlayerJoinEvent event, UUID uuid, CoinsData coinsData, AccountData playerData) {
        playerData.setCoinsData(coinsData);
        playerData.setName(event.getPlayer().getName());
        LightCoins.instance.getLightCoinsAPI().getAccountData().put(uuid, playerData);
        LightCoins.instance.getConsolePrinter().printInfo("Player data loaded for " + uuid);
    }

    private void handleVirtualCurrencies(UUID uuid, AccountData playerData) {
        LightCoins.instance.getConsolePrinter().printInfo("Loading virtual currencies for " + uuid);
        LightCoins.instance.getVirtualDataTable().findVirtualDataByUUID(uuid).thenComposeAsync(virtualDataList -> {
            List<File> virtualCurrencyFiles = LightCoins.instance.getVirtualCurrencyFiles().getYamlFiles();
            AtomicInteger amount = new AtomicInteger();
            for (File currencyFile : virtualCurrencyFiles) {
                String currencyName = currencyFile.getName().replace(".yml", "");
                boolean currencyExists = virtualDataList.stream()
                        .anyMatch(virtualData -> virtualData.getCurrencyName().equals(currencyName));

                if (!currencyExists) {
                    LightCoins.instance.getConsolePrinter().printInfo("Currency not found for: " + currencyName + ", creating new entry for: " + uuid);
                    VirtualData newVirtualData = new VirtualData(currencyFile, uuid);
                    newVirtualData.setPlayerName(playerData.getName());
                    newVirtualData.setPlayerUUID(uuid);
                    playerData.getVirtualCurrencies().add(newVirtualData);
                    amount.getAndIncrement();
                    LightCoins.instance.getVirtualDataTable().writeVirtualData(newVirtualData).join();
                } else {
                    virtualDataList.stream()
                            .filter(virtualData -> virtualData.getCurrencyName().equals(currencyName))
                            .forEach(virtualData -> {
                                virtualData.setFile(currencyFile);
                                playerData.getVirtualCurrencies().add(virtualData);
                                amount.getAndIncrement();
                            });
                }
            }

            LightCoins.instance.getConsolePrinter().printInfo("Loaded " + amount.get() + " virtual currencies for user " + uuid);
            return CompletableFuture.completedFuture(null);
        }).exceptionally(e -> {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while handling virtual currencies for UUID: " + uuid,
                    "Please check the error logs for more information."
            ));
            throw new RuntimeException(e);
        });
    }

    private File createDefaultCurrencyFile(String currencyName) {
        File currencyFile = new File("/plugin/virtual-currency/", currencyName + ".yml");
        try {
            if (currencyFile.createNewFile()) {
                FileConfiguration config = YamlConfiguration.loadConfiguration(currencyFile);
                config.set("display-name", currencyName);
                config.set("start-balance", 0.0);
                config.set("decimal-places", 2);
                config.set("currency-name-plural", currencyName + "s");
                config.set("currency-name-singular", currencyName);
                config.set("max-balance", 1000000.0);
                config.save(currencyFile);
            } else {
                LightCoins.instance.getConsolePrinter().printError(List.of(
                        "An error occurred while creating default currency file: " + currencyFile.getName(),
                        "Found existing file with the same name, but not match with the database."
                ));
            }
        } catch (Exception e) {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while creating virtual currency file: " + currencyFile.getName(),
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
        }
        return currencyFile;
    }
}
