package org.example.enotab.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.example.enotab.EnoTab;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TabConfig {

    private final EnoTab plugin;
    private File configFile;
    private FileConfiguration config;

    private String header;
    private String footer;
    private String playerFormat;
    private boolean animatedHeader;
    private boolean animatedFooter;
    private List<String> headerFrames;
    private List<String> footerFrames;
    private int animationInterval;
    private int updateInterval;
    private boolean showPing;
    private boolean showHealth;

    public TabConfig(EnoTab plugin) {
        this.plugin = plugin;
        this.headerFrames = new ArrayList<>();
        this.footerFrames = new ArrayList<>();
    }

    public void load() {
        configFile = new File(plugin.getDataFolder(), "tab.yml");
        if (!configFile.exists()) {
            plugin.saveResource("tab.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        loadValues();
    }

    private void loadValues() {
        header = config.getString("header.text", "&6&lEnoTab &7- &eSunucu");
        footer = config.getString("footer.text", "&7Oyuncular: &e%online%&7/&e%max%");
        playerFormat = config.getString("player.format", "&f%player%");

        animatedHeader = config.getBoolean("header.animated", false);
        animatedFooter = config.getBoolean("footer.animated", false);

        headerFrames = config.getStringList("header.frames");
        if (headerFrames.isEmpty()) {
            headerFrames.add("&6&lEnoTab &7- &eFrame 1");
            headerFrames.add("&e&lEnoTab &7- &6Frame 2");
            headerFrames.add("&f&lEnoTab &7- &eFrame 3");
        }

        footerFrames = config.getStringList("footer.frames");
        if (footerFrames.isEmpty()) {
            footerFrames.add("&7TPS: &a%tps%");
            footerFrames.add("&7Ping: &a%ping%ms");
        }

        animationInterval = config.getInt("animation.interval", 20);
        updateInterval = config.getInt("update.interval", 40);

        showPing = config.getBoolean("display.ping", true);
        showHealth = config.getBoolean("display.health", false);
    }

    public void save() {
        config.set("header.text", header);
        config.set("footer.text", footer);
        config.set("player.format", playerFormat);
        config.set("header.animated", animatedHeader);
        config.set("footer.animated", animatedFooter);
        config.set("header.frames", headerFrames);
        config.set("footer.frames", footerFrames);
        config.set("animation.interval", animationInterval);
        config.set("update.interval", updateInterval);
        config.set("display.ping", showPing);
        config.set("display.health", showHealth);

        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Tab config kaydedilemedi: " + e.getMessage());
        }
    }

    public void reload() {
        config = YamlConfiguration.loadConfiguration(configFile);
        loadValues();
    }

    public String getHeader() { return header; }
    public void setHeader(String header) { this.header = header; }

    public String getFooter() { return footer; }
    public void setFooter(String footer) { this.footer = footer; }

    public String getPlayerFormat() { return playerFormat; }
    public void setPlayerFormat(String playerFormat) { this.playerFormat = playerFormat; }

    public boolean isAnimatedHeader() { return animatedHeader; }
    public void setAnimatedHeader(boolean animatedHeader) { this.animatedHeader = animatedHeader; }

    public boolean isAnimatedFooter() { return animatedFooter; }
    public void setAnimatedFooter(boolean animatedFooter) { this.animatedFooter = animatedFooter; }

    public List<String> getHeaderFrames() { return headerFrames; }
    public void setHeaderFrames(List<String> headerFrames) { this.headerFrames = headerFrames; }

    public List<String> getFooterFrames() { return footerFrames; }
    public void setFooterFrames(List<String> footerFrames) { this.footerFrames = footerFrames; }

    public int getAnimationInterval() { return animationInterval; }
    public void setAnimationInterval(int animationInterval) { this.animationInterval = animationInterval; }

    public int getUpdateInterval() { return updateInterval; }
    public void setUpdateInterval(int updateInterval) { this.updateInterval = updateInterval; }

    public boolean isShowPing() { return showPing; }
    public void setShowPing(boolean showPing) { this.showPing = showPing; }

    public boolean isShowHealth() { return showHealth; }
    public void setShowHealth(boolean showHealth) { this.showHealth = showHealth; }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        ConfigData data = new ConfigData();
        data.header = header;
        data.footer = footer;
        data.playerFormat = playerFormat;
        data.animatedHeader = animatedHeader;
        data.animatedFooter = animatedFooter;
        data.headerFrames = headerFrames;
        data.footerFrames = footerFrames;
        data.animationInterval = animationInterval;
        data.updateInterval = updateInterval;
        data.showPing = showPing;
        data.showHealth = showHealth;
        return gson.toJson(data);
    }

    public void fromJson(String json) {
        Gson gson = new Gson();
        ConfigData data = gson.fromJson(json, ConfigData.class);
        if (data != null) {
            header = data.header;
            footer = data.footer;
            playerFormat = data.playerFormat;
            animatedHeader = data.animatedHeader;
            animatedFooter = data.animatedFooter;
            headerFrames = data.headerFrames;
            footerFrames = data.footerFrames;
            animationInterval = data.animationInterval;
            updateInterval = data.updateInterval;
            showPing = data.showPing;
            showHealth = data.showHealth;
            save();
        }
    }

    private static class ConfigData {
        String header;
        String footer;
        String playerFormat;
        boolean animatedHeader;
        boolean animatedFooter;
        List<String> headerFrames;
        List<String> footerFrames;
        int animationInterval;
        int updateInterval;
        boolean showPing;
        boolean showHealth;
    }
}
