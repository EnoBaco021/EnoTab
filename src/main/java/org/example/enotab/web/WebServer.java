package org.example.enotab.web;

import com.sun.net.httpserver.HttpServer;
import org.example.enotab.EnoTab;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class WebServer {

    private final EnoTab plugin;
    private final int port;
    private HttpServer server;

    public WebServer(EnoTab plugin, int port) {
        this.plugin = plugin;
        this.port = port;
    }

    public void start() {
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.setExecutor(Executors.newFixedThreadPool(10));

            // Route'ları ekle
            server.createContext("/", new WebHandlers.RootHandler());
            server.createContext("/login", new WebHandlers.LoginHandler(plugin));
            server.createContext("/logout", new WebHandlers.LogoutHandler());
            server.createContext("/dashboard", new WebHandlers.DashboardHandler(plugin));
            server.createContext("/api/config", new WebHandlers.ConfigApiHandler(plugin));
            server.createContext("/api/players", new WebHandlers.PlayersApiHandler(plugin));
            server.createContext("/api/stats", new WebHandlers.StatsApiHandler(plugin));
            server.createContext("/api/presets", new WebHandlers.PresetsListHandler(plugin));
            server.createContext("/api/presets/", new WebHandlers.PresetDetailHandler(plugin));
            server.createContext("/assets/style.css", new WebHandlers.CssHandler());
            server.createContext("/assets/script.js", new WebHandlers.JsHandler());

            server.start();
            plugin.getLogger().info("Web sunucusu " + port + " portunda başlatıldı!");
        } catch (Exception e) {
            plugin.getLogger().severe("Web sunucusu başlatılamadı: " + e.getMessage());
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            plugin.getLogger().info("Web sunucusu durduruldu.");
        }
    }

    public int getPort() {
        return port;
    }
}
