package io.lightstudios.coins;

import io.lightstudios.coins.api.LightCoinsAPI;
import io.lightstudios.coins.api.models.AccountData;
import io.lightstudios.coins.api.models.CoinsData;
import io.lightstudios.coins.api.models.VirtualData;
import io.lightstudios.coins.commands.overall.admin.DefaultHelpCommand;
import io.lightstudios.coins.commands.overall.admin.ReloadCommand;
import io.lightstudios.coins.commands.transfer.TransferCommand;
import io.lightstudios.coins.commands.vault.admin.*;
import io.lightstudios.coins.commands.vault.player.BalTopCommand;
import io.lightstudios.coins.commands.vault.player.HelpCommand;
import io.lightstudios.coins.commands.vault.player.PayCommand;
import io.lightstudios.coins.commands.virtual.admin.VirtualAddCommand;
import io.lightstudios.coins.commands.virtual.admin.VirtualHelpCommand;
import io.lightstudios.coins.commands.virtual.admin.VirtualRemoveCommand;
import io.lightstudios.coins.commands.virtual.admin.VirtualSetCommand;
import io.lightstudios.coins.commands.virtual.defaults.VirtualShowCommand;
import io.lightstudios.coins.configs.MessageConfig;
import io.lightstudios.coins.configs.SettingsConfig;
import io.lightstudios.coins.impl.events.OnPlayerJoin;
import io.lightstudios.coins.impl.vault.VaultImplementerSQL;
import io.lightstudios.coins.impl.vault.VaultImplementerSingle;
import io.lightstudios.coins.placeholder.PlaceholderManager;
import io.lightstudios.coins.storage.CoinsDataTable;
import io.lightstudios.coins.storage.VirtualDataTable;
import io.lightstudios.coins.synchronisation.subscriber.UpdateCoinsBalance;
import io.lightstudios.coins.synchronisation.subscriber.UpdateVirtualBalance;
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

import java.util.*;

@Getter
public final class LightCoins extends JavaPlugin {

    public static LightCoins instance;
    private LightCoinsAPI lightCoinsAPI;
    private CoinsDataTable coinsTable;
    private VirtualDataTable virtualDataTable;
    private VaultImplementerSingle vaultImplementer;
    private VaultImplementerSQL vaultImplementerSQL;
    private ConsolePrinter consolePrinter;
    private PlaceholderManager placeholderManager;

    private MessageConfig messageConfig;
    private SettingsConfig settingsConfig;

    private MultiFileManager virtualCurrencyFiles;

    private FileManager settings;
    private FileManager message;

    private CommandManager vaultCommands;
    private CommandManager virtualCommands;
    private CommandManager balTopCommands;
    private CommandManager payCommands;

    @Override
    public void onLoad() {

        instance = this;
        this.consolePrinter = new ConsolePrinter("§7[§rLight§eCoins§7] §r");
        consolePrinter.printInfo("Starting LightCoins...");
        this.vaultImplementer = new VaultImplementerSingle();
        // register the vault provider
        consolePrinter.printInfo("Registering Vault Provider...");

        this.coinsTable = new CoinsDataTable();
        this.virtualDataTable = new VirtualDataTable();

        this.lightCoinsAPI = new LightCoinsAPI();

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

        // read existing player data from the database and populate the playerData map in LightCoinsAPI
        readPlayerData();
        readVirtualData();
        registerEvents();
        registerCommands();
        // Redis synchronisation subscribers
        new UpdateCoinsBalance();
        new UpdateVirtualBalance();

        if(LightCore.instance.getHookManager().isExistPlaceholderAPI()) {
            consolePrinter.printInfo("Registering placeholder for LightCoins...");
            this.placeholderManager = new PlaceholderManager();
        } else {
            consolePrinter.printError("PlaceholderAPI not found. Placeholder will not be registered and cant be used.");
        }
    }

    @Override
    public void onDisable() {

    }

    public void loadDefaults() {
        readAndWriteConfigs();
        selectLanguage();
        readVirtualCurrencies();
        unregisterCommands();
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
            new FileManager(this, "virtual-currency/_example.yml", true);
            new FileManager(this, "virtual-currency/gems.yml", true);
            this.virtualCurrencyFiles = new MultiFileManager("plugins/" + getName() + "/virtual-currency/");
        } catch (Exception e) {
            LightCoins.instance.getConsolePrinter().printError("Failed to load virtual currencies.");
            throw new RuntimeException("Failed to load virtual currencies.");
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
            getLightCoinsAPI().getAccountData().put(coinsData.getUuid(), accountData);
        }
        float end = System.currentTimeMillis();
        int size = coinsDataList.size();
        LightCoins.instance.getConsolePrinter().printInfo(
                "Read " + size + " player data from the database in " + (end - start) + "ms");
    }

    private void readVirtualData() {
        float start = System.currentTimeMillis();
        List<VirtualData> virtualDataList = getVirtualDataTable().readVirtualData().join();
        for (VirtualData virtualData : virtualDataList) {
            UUID uuid = virtualData.getPlayerUUID();

            if(uuid == null) {
                LightCoins.instance.getConsolePrinter().printError(
                        "An error occurred while reading virtual data uuid from the database!");
                continue;
            }

            for(AccountData accountData : LightCoins.instance.getLightCoinsAPI().getAccountData().values()) {
                if(accountData.getUuid().equals(uuid)) {
                    if(!accountData.getVirtualCurrencies().isEmpty()) {
                        accountData.getVirtualCurrencies().clear();
                    }
                    accountData.getVirtualCurrencies().add(virtualData);
                    consolePrinter.printInfo("Loaded virtual data " + virtualData.getCurrencyName() +
                            " for player: " + accountData.getName());
                    continue;
                }
            }
        }
        float end = System.currentTimeMillis();
        int size = virtualDataList.size();
        LightCoins.instance.getConsolePrinter().printInfo(
                "Read " + size + " virtual data from the database in " + (end - start) + "ms");
    }

    /**
     * Registers the Vault provider for LightCoins
     * The Vault provider is used to interact with other plugins that use Vault
     * If Redis is not enabled, vaultImplementerSQL is used (direct Database access in sync -> not recommended)
     */
    private void registerVaultProvider() {

        Economy vaultProvider;

        if(!LightCore.instance.getSettings().syncType().equalsIgnoreCase("redis") &&
                LightCore.instance.getSettings().multiServerEnabled()) {
            getConsolePrinter().printInfo(List.of(
                    "Redis is §cnot enabled. §rUsing direct database access for Vault provider.",
                    "This is not recommended and can cause issues with performance, if you have a lot of players."
            ));
            vaultProvider = this.vaultImplementerSQL;
        } else {
            getConsolePrinter().printInfo(List.of(
                    "Redis is §aenabled. §rUsing Redis for Vault provider.",
                    "This is recommended and should be used if you have a lot of players."
            ));
            vaultProvider = this.vaultImplementer;
        }
        Bukkit.getServicesManager().register(Economy.class, vaultProvider, this, ServicePriority.Highest);
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServer().getServicesManager().getRegistration(Economy.class);
        if(rsp != null) {
            LightCoins.instance.getConsolePrinter().printInfo("Successfully registered Vault provider " + rsp.getProvider().getName());
        }
    }

    private void registerCommands() {

        vaultCommands = new CommandManager(new ArrayList<>(List.of(
                new ShowCoinsCommand(),
                new AddCoinsCommand(),
                new RemoveCoinsCommand(),
                new DeleteAccountCommand(),
                new AddAllCommand(),
                new SetCoinsCommand(),
                new HelpCommand()
        )), settingsConfig.commandsCoins());

        payCommands = new CommandManager(new ArrayList<>(List.of(
                new PayCommand()
        )), settingsConfig.commandsPay());

        balTopCommands = new CommandManager(new ArrayList<>(List.of(
                new BalTopCommand()
        )), settingsConfig.commandsBaltop());

        virtualCommands = new CommandManager(new ArrayList<>(List.of(
                new VirtualShowCommand(),
                new VirtualAddCommand(),
                new VirtualSetCommand(),
                new VirtualRemoveCommand(),
                new VirtualHelpCommand()
        )), settingsConfig.commandsVirtual());

        new CommandManager(new ArrayList<>(List.of(
                new ReloadCommand(),
                new DefaultHelpCommand(),
                new TransferCommand()
        )), "lightcoins");
    }

    private void unregisterCommands() {

        if(vaultCommands == null || payCommands == null || balTopCommands == null || virtualCommands == null) {
            return;
        }

        vaultCommands.unregisterCommand();
        payCommands.unregisterCommand();
        balTopCommands.unregisterCommand();
        virtualCommands.unregisterCommand();
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
