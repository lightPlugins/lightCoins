package io.lightstudios.coins.configs;

import io.lightstudios.core.player.title.countupdown.AnimatedCountTitle;
import io.lightstudios.core.player.title.countupdown.AnimatedCountTitleSettings;
import io.lightstudios.core.util.files.FileManager;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Getter
public class TitleConfig {

    private final FileConfiguration config;
    private final FileConfiguration language;
    private Map<String, AnimatedCountTitleSettings> titleSettings = new HashMap<>();

    public TitleConfig(FileManager titleConfig, FileManager languageConfig) {
        this.config = titleConfig.getConfig();
        this.language = languageConfig.getConfig();
        readKeys();
    }

    public AnimatedCountTitle getAnimatedCountTitle() { return new AnimatedCountTitle(); }

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

            Component upperTitle = Component.text(language.getString("titles.transactions.animated." + path + ".start.upperTitle", ""));
            Component lowerTitle = Component.text(language.getString("titles.transactions.animated." + path + ".start.lowerTitle", ""));

            animationSettings.setLowerTitle(lowerTitle);
            animationSettings.setUpperTitle(upperTitle);

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

                Component upperTitleEnd = Component.text(language.getString("titles.transactions.animated." + path + ".end.upperTitle",
                        "titles.transactions.animated." + path + ".end.upperTitle"));
                Component lowerTitleEnd = Component.text(language.getString("titles.transactions.animated." + path + ".end.lowerTitle",
                        "titles.transactions.animated." + path + ".end.lowerTitle"));

                // title components
                endAnimationSettings.setLowerTitle(lowerTitleEnd);
                endAnimationSettings.setUpperTitle(upperTitleEnd);

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
