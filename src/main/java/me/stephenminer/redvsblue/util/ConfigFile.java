package me.stephenminer.redvsblue.util;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import me.stephenminer.redvsblue.RedVsBlue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ConfigFile {
    private final RedVsBlue plugin;
    private final String name;

    public ConfigFile(RedVsBlue plugin, String name) {
        this.plugin = plugin;
        this.name = name;
        saveDefaultConfig();
    }

    private File configFile = null;
    private FileConfiguration dataConfig = null;

    public void reloadConfig() {
        if (this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), name + ".yml");
        this.dataConfig = YamlConfiguration.loadConfiguration(this.configFile);

        InputStream defaultStream = this.plugin.getResource(name + ".yml");
        if (defaultStream != null) {
            YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defaultStream));
            this.dataConfig.setDefaults(defaultConfig);
        }
    }

    public FileConfiguration getConfig() {
        if (this.dataConfig == null)
            reloadConfig();

        return this.dataConfig;
    }

    public void saveConfig() {
        if (this.dataConfig == null || this.configFile == null)
            return;
        try {
            this.dataConfig.save(this.configFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Couldn't save configuration '" + name + "' to '" + this.configFile + "'.");
        }
    }

    public void saveDefaultConfig() {
        if (this.configFile == null)
            this.configFile = new File(this.plugin.getDataFolder(), name + ".yml");
        if (!this.configFile.exists()) {
            this.plugin.saveResource(name + ".yml", false);
        }
    }
}
