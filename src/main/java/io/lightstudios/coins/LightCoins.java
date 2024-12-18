package io.lightstudios.coins;

import io.lightstudios.coins.api.LightCoinsAPI;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.VirtualCurrency;
import io.lightstudios.coins.commands.admin.*;
import io.lightstudios.coins.commands.defaults.BalTopCommand;
import io.lightstudios.coins.commands.defaults.PayCommand;
import io.lightstudios.coins.configs.MessageConfig;
import io.lightstudios.coins.configs.SettingsConfig;
import io.lightstudios.coins.impl.events.OnPlayerJoin;
import io.lightstudios.coins.impl.vault.VaultImplementer;
import io.lightstudios.coins.storage.CoinsDataTable;
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
import java.util.*;

@Getter
public final class LightCoins extends JavaPlugin {

    public static LightCoins instance;
    private LightCoinsAPI lightCoinsAPI;
    private CoinsDataTable coinsTable;
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
        this.coinsTable = new CoinsDataTable();
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
     * SYNCHRONOUS
     */
    public void readPlayerData() {
        float start = System.currentTimeMillis();
        List<CoinsData> coinsDataList = getCoinsTable().readCoinsData().join();
        for (CoinsData coinsData : coinsDataList) {
            AccountData accountData = new AccountData();
            accountData.setCoinsData(coinsData);
            accountData.setUuid(coinsData.getUuid());
            accountData.setName(coinsData.getName());
            accountData.setOfflinePlayer(Bukkit.getServer().getOfflinePlayer(coinsData.getUuid()));
            getLightCoinsAPI().getPlayerData().put(coinsData.getUuid(), accountData);
        }
        float end = System.currentTimeMillis();
        int size = coinsDataList.size();
        LightCoins.instance.getConsolePrinter().printInfo("Read " + size + " player data from the database in " + (end - start) + "ms");
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
