package io.lightstudios.coins;

import io.lightstudios.coins.api.LightCoinsAPI;
import io.lightstudios.coins.api.models.CoinsPlayer;
import io.lightstudios.coins.api.models.PlayerData;
import io.lightstudios.coins.api.models.VirtualCurrency;
import io.lightstudios.coins.commands.admin.AddCoinsCommand;
import io.lightstudios.coins.commands.admin.ShowCoinsCommand;
import io.lightstudios.coins.commands.admin.RemoveCoinsCommand;
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
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

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
        this.vaultImplementer = new VaultImplementer();
        // register the vault provider
        registerVaultProvider();
        readAndWriteConfigs();
        selectLanguage();
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
            LightCore.instance.getConsolePrinter().printError("Failed to load virtual currencies.");
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
    private void readPlayerData() {
        LightCore.instance.getConsolePrinter().printInfo("Reading existing player data from the database...");
        float startTime = System.currentTimeMillis();
        try {
            // Call createExistingPlayerData and get the result synchronously
            HashMap<UUID, CoinsPlayer> playerDataMap = coinsTable.readExistingCoinsPlayer().get();

            // Process the result and populate the playerData map in LightCoinsAPI
            playerDataMap.forEach((uuid, coinsPlayer) -> {
                PlayerData playerData = new PlayerData();
                playerData.setCoinsPlayer(coinsPlayer);
                lightCoinsAPI.getPlayerData().put(uuid, playerData);
            });

            float endTime = System.currentTimeMillis();
            LightCore.instance.getConsolePrinter().printInfo("Found " + playerDataMap.size()
                    + " player data entries in " + (endTime - startTime) + "ms!");
        } catch (Exception e) {
            LightCore.instance.getConsolePrinter().printError(List.of(
                    "An error occurred while reading player data from the database!",
                    "Please check the error logs for more information."
            ));
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
            LightCore.instance.getConsolePrinter().printInfo("Successfully registered Vault provider " + rsp.getProvider().getName());
        }
    }

    private void registerCommands() {

        new CommandManager(new ArrayList<>(List.of(
                new ShowCoinsCommand(),
                new AddCoinsCommand(),
                new RemoveCoinsCommand()
        )), "coins");
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
