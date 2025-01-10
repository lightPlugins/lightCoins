package io.lightstudios.coins.api.models;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;

public class VoucherData {

    private File file;
    private boolean enabled;
    private ItemStack voucherItem;
    private String currencyName;
    private double minValue;
    private double maxValue;
    private int commandCooldown;
    private boolean vaultEconomy;


    public VoucherData(File file) {
        this.file = file;
        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        this.enabled = config.getBoolean("enabled");

    }

}
