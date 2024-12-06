package io.lightstudios.coins;

import io.lightstudios.coins.api.LightCoinsAPI;
import io.lightstudios.coins.api.models.VirtualCurrency;
import io.lightstudios.coins.configs.MessageConfig;
import io.lightstudios.coins.configs.SettingsConfig;
import io.lightstudios.core.LightCore;
import io.lightstudios.core.util.files.FileManager;
import io.lightstudios.core.util.files.MultiFileManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

@Getter
public final class LightCoins extends JavaPlugin {

    public static LightCoins instance;
    private LightCoinsAPI lightCoinsAPI;

    private MessageConfig messageConfig;
    private SettingsConfig settingsConfig;

    private MultiFileManager virtualCurrencyFiles;

    private FileManager settings;

    @Override
    public void onLoad() {
        instance = this;

    }

    @Override
    public void onEnable() {
        this.lightCoinsAPI = new LightCoinsAPI();
    }

    @Override
    public void onDisable() {

    }

    private void readAndWriteConfigs() {

        this.settings = new FileManager(this, "settings.yml", true);
        this.settingsConfig = new SettingsConfig(this.settings);

    }

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
}
