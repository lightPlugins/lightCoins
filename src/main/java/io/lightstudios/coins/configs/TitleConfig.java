package io.lightstudios.coins.configs;

import io.lightstudios.core.player.title.countupdown.AnimatedCountTitleSettings;
import io.lightstudios.core.util.files.FileManager;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class TitleConfig {

    private final FileConfiguration config;
    private Map<String, AnimatedCountTitleSettings> titleSettings = new HashMap<>();

    public TitleConfig(FileManager titleConfig) {
        this.config = titleConfig.getConfig();
        readKeys();
    }


    private void readKeys() {
        // Sichere Sicherstellen, dass der Abschnitt "animated" existiert
        if (!config.isConfigurationSection("animated")) {
            return;
        }

        // Iteriere über alle Schlüssel innerhalb des "animated"-Abschnitts
        for (String key : config.getConfigurationSection("animated").getKeys(false)) {
            // Ein neues AnimatedCountTitleSettings-Objekt erstellen
            AnimatedCountTitleSettings settings = new AnimatedCountTitleSettings();
            String path = "animated." + key;

            // Hauptwerte setzen
            settings.setEnable(config.getBoolean(path + ".enable", true));
            settings.setMinAmountTrigger(new BigDecimal(config.getInt(path + ".minAmountTrigger", -1)));

            // Animationseinstellungen erstellen
            AnimatedCountTitleSettings.AnimationSettings animationSettings = new AnimatedCountTitleSettings.AnimationSettings();

            // Sounds laden und setzen
            Map<Integer, AnimatedCountTitleSettings.SoundRangeSettings> sounds = new HashMap<>();
            if (config.isConfigurationSection(path + ".sounds")) {
                for (String soundKey : config.getConfigurationSection(path + ".sounds").getKeys(false)) {
                    if (soundKey.matches("\\d+")) {
                        String soundPath = path + ".sounds." + soundKey;

                        AnimatedCountTitleSettings.SoundRangeSettings soundSettings = new AnimatedCountTitleSettings.SoundRangeSettings();
                        soundSettings.setSound(config.getString(soundPath + ".sound", ""));
                        soundSettings.setVolume(config.getDouble(soundPath + ".volume", 1.0));
                        soundSettings.setStartPitch(config.getDouble(soundPath + ".startPitch", 0.0));
                        soundSettings.setEndPitch(config.getDouble(soundPath + ".endPitch", 1.0));

                        sounds.put(Integer.valueOf(soundKey), soundSettings);
                    }
                }
            }
            animationSettings.setSounds(sounds);

            // End-of-Animation-Einstellungen auslesen
            String endAnimationPath = path + ".endOfAnimation";
            if (config.isConfigurationSection(endAnimationPath)) {
                AnimatedCountTitleSettings.EndAnimationSettings endAnimationSettings = new AnimatedCountTitleSettings.EndAnimationSettings();

                endAnimationSettings.setStayTime(config.getLong(endAnimationPath + ".stayTime", 2000));
                endAnimationSettings.setFadeOutTime(config.getLong(endAnimationPath + ".fadeOutTime", 1000));

                // Sounds innerhalb der Endanimation laden
                Map<Integer, AnimatedCountTitleSettings.SimpleSoundSettings> endSounds = new HashMap<>();
                if (config.isConfigurationSection(endAnimationPath + ".sounds")) {
                    for (String soundKey : config.getConfigurationSection(endAnimationPath + ".sounds").getKeys(false)) {
                        if (soundKey.matches("\\d+")) {
                            String soundPath = endAnimationPath + ".sounds." + soundKey;

                            AnimatedCountTitleSettings.SimpleSoundSettings simpleSoundSettings = new AnimatedCountTitleSettings.SimpleSoundSettings();
                            simpleSoundSettings.setSound(config.getString(soundPath + ".sound", ""));
                            simpleSoundSettings.setVolume(config.getDouble(soundPath + ".volume", 1.0));
                            simpleSoundSettings.setPitch(config.getDouble(soundPath + ".pitch", 1.0));

                            endSounds.put(Integer.valueOf(soundKey), simpleSoundSettings);
                        }
                    }
                }
                endAnimationSettings.setSounds(endSounds);

                animationSettings.setEndAnimation(endAnimationSettings);
            }

            // Animationseinstellungen setzen
            settings.setAnimation(animationSettings);

            // In die Map speichern
            titleSettings.put(key, settings);
        }
    }
}
