package io.lightstudios.coins.configs;

import io.lightstudios.core.util.files.FileManager;
import org.bukkit.configuration.file.FileConfiguration;

public class TitleConfig {

    private final FileConfiguration config;

    public TitleConfig(FileManager titleConfig) {
        this.config = titleConfig.getConfig();
    }

}
