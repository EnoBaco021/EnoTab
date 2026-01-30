package org.example.enotab;

import org.bukkit.plugin.java.JavaPlugin;
import org.example.enotab.commands.EnoTabCommand;
import org.example.enotab.config.TabConfig;
import org.example.enotab.listeners.PlayerListener;
import org.example.enotab.tab.TabManager;
import org.example.enotab.web.WebServer;

public class EnoTab extends JavaPlugin {

    private static EnoTab instance;
    private TabConfig tabConfig;
    private TabManager tabManager;
    private WebServer webServer;

    @Override
    public void onEnable() {
        instance = this;

        // Config yükle
        saveDefaultConfig();
        tabConfig = new TabConfig(this);
        tabConfig.load();

        // Tab manager başlat
        tabManager = new TabManager(this);
        tabManager.startTask();

        // Web sunucusunu başlat
        int webPort = getConfig().getInt("web.port", 6969);
        webServer = new WebServer(this, webPort);
        webServer.start();

        // Listener kaydet
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        // Komutları kaydet
        getCommand("enotab").setExecutor(new EnoTabCommand(this));

        getLogger().info("EnoTab başarıyla etkinleştirildi!");
        getLogger().info("Web panel: http://localhost:" + webPort);
    }

    @Override
    public void onDisable() {
        if (webServer != null) {
            webServer.stop();
        }
        if (tabManager != null) {
            tabManager.stopTask();
        }
        getLogger().info("EnoTab devre dışı bırakıldı!");
    }

    public static EnoTab getInstance() {
        return instance;
    }

    public TabConfig getTabConfig() {
        return tabConfig;
    }

    public TabManager getTabManager() {
        return tabManager;
    }

    public WebServer getWebServer() {
        return webServer;
    }
}

