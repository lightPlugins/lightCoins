package io.lightstudios.coins;

import io.lightstudios.coins.api.LightCoinsAPI;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.coins.api.models.VirtualCurrency;
import io.lightstudios.coins.commands.admin.*;
import io.lightstudios.coins.commands.defaults.BalTopCommand;
import io.lightstudios.coins.commands.defaults.PayCommand;
import io.lightstudios.coins.configs.MessageConfig;
import io.lightstudios.coins.configs.SettingsConfig;
import io.lightstudios.coins.impl.events.OnPlayerJoin;
import io.lightstudios.coins.impl.vault.VaultImplementer;
import io.lightstudios.coins.storage.CoinsTable;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.commands.manager.CommandManager;
import io.lightstudios.core.util.ConsolePrinter;
import io.lightstudios.core.util.files.FileManager;
import io.lightstudios.core.util.files.MultiFileManager;
import lombok.Getter;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.checkerframework.checker.units.qual.C;

import java.io.File;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Getter
public final class LightCoins extends JavaPlugin {

    public static LightCoins instance;
    private LightCoinsAPI lightCoinsAPI;
    private CoinsTable coinsTable;
    private VaultImplementer vaultImplementer;
    private ConsolePrinter consolePrinter;

    private MessageConfig messageConfig;
    private SettingsConfig settingsConfig;

    private MultiFileManager virtualCurrencyFiles;

    private FileManager settings;
    private FileManager message;

    @Override
    public void onLoad() {

        instance = this;
        this.consolePrinter = new ConsolePrinter("§7[§rLight§eCoins§7] §r");
        consolePrinter.printInfo("Starting LightCoins...");
        this.vaultImplementer = new VaultImplementer();
        // register the vault provider
        consolePrinter.printInfo("Registering Vault Provider...");
        registerVaultProvider();
        consolePrinter.printInfo("Read and Write Configs...");
        readAndWriteConfigs();
        consolePrinter.printInfo("Select Language file...");
        selectLanguage();
        consolePrinter.printInfo("Read and Write virtual currencies...");
        readVirtualCurrencies();
    }

    @Override
    public void onEnable() {
        this.lightCoinsAPI = new LightCoinsAPI();
        this.coinsTable = new CoinsTable();
        // read existing player data from the database and populate the playerData map in LightCoinsAPI
        readPlayerData();
        registerEvents();
        registerCommands();
    }

    @Override
    public void onDisable() {

    }

    public void loadDefaults() {
        readAndWriteConfigs();
        selectLanguage();
        readVirtualCurrencies();
        registerCommands();
    }

    /**
     * Initializes the plugin by reading and writing configs
     */
    private void readAndWriteConfigs() {
        this.settings = new FileManager(this, "settings.yml", true);
        this.settingsConfig = new SettingsConfig(this.settings);
    }
    /**
     * Reads the virtual currencies from the virtual-currency folder and
     * adds them to the virtualCurrencies list in LightCoinsAPI
     */
    private void readVirtualCurrencies() {
        try {
            this.virtualCurrencyFiles = new MultiFileManager("/virtual-currency/virtual-currencies");
        } catch (Exception e) {
            LightCoins.instance.getConsolePrinter().printError("Failed to load virtual currencies.");
            throw new RuntimeException("Failed to load virtual currencies.");
        }

        for(File file : this.virtualCurrencyFiles.getYamlFiles()) {
            getLightCoinsAPI().getVirtualCurrencies().add(new VirtualCurrency(file));
        }
    }

    /**
     * Registers events in the plugin for listening to Bukkit events
     */
    private void registerEvents() {
        // creates a new player data object for a player when they join the server
        getServer().getPluginManager().registerEvents(new OnPlayerJoin(), this);
    }

    /**
     * Reads existing player data from the database and populates the playerData map in LightCoinsAPI
     */
    public void readPlayerData() {
        LightCoins.instance.getConsolePrinter().printInfo("Reading existing player data from the database...");
        long startTime = System.currentTimeMillis();
        try {
            // Fetch all UUIDs from the database
            String query = "SELECT uuid FROM " + coinsTable.getTableName();
            CompletableFuture<List<String>> futureUuids = LightCore.instance.getSqlDatabase().querySqlFuture(query, "uuid")
                    .thenApply(result -> {
                        if (result == null || result.isEmpty()) {
                            LightCoins.instance.getConsolePrinter().printError("No UUIDs found in the database.");
                            return Collections.emptyList();
                        }
                        LightCoins.instance.getConsolePrinter().printError("UUIDs found in the database: " + result);
                        return result.stream().map(Object::toString).collect(Collectors.toList());
                    });

            // Process each UUID and populate the playerData map in LightCoinsAPI
            List<CompletableFuture<Void>> futures = futureUuids.thenApply(uuids -> uuids.stream()
                    .map(uuid -> coinsTable.readCoins(uuid).thenAccept(result -> {
                        if (result != null) {
                            result.forEach((playerUUID, coins) -> {
                                OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(playerUUID);
                                CoinsPlayer coinsPlayer = new CoinsPlayer(playerUUID);
                                coinsPlayer.setCoins(coins);

                                PlayerData playerData = new PlayerData();
                                playerData.setCoinsPlayer(coinsPlayer);
                                playerData.setPlayerName(offlinePlayer.getName());
                                playerData.setOfflinePlayer(offlinePlayer);

                                lightCoinsAPI.getPlayerData().put(playerUUID, playerData);
                            });
                        } else {
                            LightCoins.instance.getConsolePrinter().printError("No coins data found for UUID: " + uuid);
                        }
                    }))
                    .collect(Collectors.toList())
            ).join();

            // Wait for all futures to complete
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            long endTime = System.currentTimeMillis();
            LightCoins.instance.getConsolePrinter().printInfo("Found " + lightCoinsAPI.getPlayerData().size()
                    + " player data entries in " + (endTime - startTime) + "ms!");
        } catch (Exception e) {
            LightCoins.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading player data from the database!",
                    "Please check the error logs for more information."
            ));
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    /**
     * Registers the Vault provider for LightCoins
     */
    private void registerVaultProvider() {
        Economy vaultProvider = this.vaultImplementer;
        Bukkit.getServicesManager().register(Economy.class, vaultProvider, this, ServicePriority.Highest);
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp != null) {
            LightCoins.instance.getConsolePrinter().printInfo("Successfully registered Vault provider " + rsp.getProvider().getName());
        }
    }

    private void registerCommands() {

        new CommandManager(new ArrayList<>(List.of(
                new ShowCoinsCommand(),
                new AddCoinsCommand(),
                new RemoveCoinsCommand(),
                new ReloadCommand(),
                new DeleteAccountCommand()
        )), "coins");

        new CommandManager(new ArrayList<>(List.of(
                new PayCommand()
        )), "pay");

        new CommandManager(new ArrayList<>(List.of(
                new BalTopCommand()
        )), "baltop");
    }

    private void selectLanguage() {
        String language = settingsConfig.language();

        switch (language) {
            case "de":
                this.message = new FileManager(this, "language/" + "de" + ".yml", true);
                break;
            case "pl":
                this.message = new FileManager(this, "language/" + "pl" + ".yml", true);
                break;
            default:
                this.message = new FileManager(this, "language/" + "en" + ".yml", true);
                break;
        }

        this.messageConfig = new MessageConfig(this.message);
    }
}
