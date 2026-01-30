package org.example.enotab.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.example.enotab.EnoTab;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

public class WebHandlers {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private static final Set<String> activeSessions = new HashSet<>();

    // Session oluştur
    private static String createSession() {
        String sessionId = UUID.randomUUID().toString();
        activeSessions.add(sessionId);
        return sessionId;
    }

    // Session doğrula
    private static boolean validateSession(HttpExchange exchange) {
        String cookie = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookie != null && cookie.contains("session=")) {
            String sessionId = cookie.split("session=")[1].split(";")[0];
            return activeSessions.contains(sessionId);
        }
        return false;
    }

    // Login sayfası handler
    public static class LoginHandler implements HttpHandler {
        private final EnoTab plugin;

        public LoginHandler(EnoTab plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                String html = getLoginTemplate();
                sendResponse(exchange, 200, html, "text/html");
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, String> params = parseFormData(body);

                String username = params.get("username");
                String password = params.get("password");

                String configUser = plugin.getConfig().getString("web.username", "admin");
                String configPass = plugin.getConfig().getString("web.password", "admin123");

                if (configUser.equals(username) && configPass.equals(password)) {
                    String sessionId = createSession();
                    exchange.getResponseHeaders().add("Set-Cookie", "session=" + sessionId + "; Path=/; HttpOnly");
                    exchange.getResponseHeaders().add("Location", "/dashboard");
                    exchange.sendResponseHeaders(302, -1);
                } else {
                    String html = getLoginTemplate().replace("<!--ERROR-->",
                        "<div class=\"error-msg\"><i class=\"fas fa-exclamation-circle\"></i> Hatalı kullanıcı adı veya şifre!</div>");
                    sendResponse(exchange, 200, html, "text/html");
                }
            }
        }
    }

    // Logout handler
    public static class LogoutHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            if (cookie != null && cookie.contains("session=")) {
                String sessionId = cookie.split("session=")[1].split(";")[0];
                activeSessions.remove(sessionId);
            }
            exchange.getResponseHeaders().add("Set-Cookie", "session=; Path=/; Max-Age=0");
            exchange.getResponseHeaders().add("Location", "/");
            exchange.sendResponseHeaders(302, -1);
        }
    }

    // Ana sayfa handler (login'e yönlendir)
    public static class RootHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (validateSession(exchange)) {
                exchange.getResponseHeaders().add("Location", "/dashboard");
            } else {
                exchange.getResponseHeaders().add("Location", "/login");
            }
            exchange.sendResponseHeaders(302, -1);
        }
    }

    // Dashboard handler
    public static class DashboardHandler implements HttpHandler {
        private final EnoTab plugin;

        public DashboardHandler(EnoTab plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!validateSession(exchange)) {
                exchange.getResponseHeaders().add("Location", "/login");
                exchange.sendResponseHeaders(302, -1);
                return;
            }
            String html = getHtmlTemplate();
            sendResponse(exchange, 200, html, "text/html");
        }
    }

    // Config API handler
    public static class ConfigApiHandler implements HttpHandler {
        private final EnoTab plugin;

        public ConfigApiHandler(EnoTab plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!validateSession(exchange)) {
                sendResponse(exchange, 401, "{\"error\": \"Unauthorized\"}", "application/json");
                return;
            }

            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                String json = plugin.getTabConfig().toJson();
                sendResponse(exchange, 200, json, "application/json");
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Bukkit.getScheduler().runTask(plugin, () -> {
                    plugin.getTabConfig().fromJson(body);
                    plugin.getTabManager().restartTask();
                    plugin.getTabManager().updateAllPlayers();
                });
                sendResponse(exchange, 200, "{\"success\": true}", "application/json");
            } else {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}", "application/json");
            }
        }
    }

    // Players API handler
    public static class PlayersApiHandler implements HttpHandler {
        private final EnoTab plugin;

        public PlayersApiHandler(EnoTab plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!validateSession(exchange)) {
                sendResponse(exchange, 401, "{\"error\": \"Unauthorized\"}", "application/json");
                return;
            }

            List<Map<String, Object>> players = new ArrayList<>();

            for (Player player : Bukkit.getOnlinePlayers()) {
                Map<String, Object> playerData = new HashMap<>();
                playerData.put("name", player.getName());
                playerData.put("uuid", player.getUniqueId().toString());
                playerData.put("ping", player.getPing());
                playerData.put("health", player.getHealth());
                playerData.put("maxHealth", player.getMaxHealth());
                playerData.put("level", player.getLevel());
                playerData.put("world", player.getWorld().getName());
                playerData.put("gamemode", player.getGameMode().name());
                playerData.put("op", player.isOp());
                players.add(playerData);
            }

            String json = gson.toJson(players);
            sendResponse(exchange, 200, json, "application/json");
        }
    }

    // Stats API handler
    public static class StatsApiHandler implements HttpHandler {
        private final EnoTab plugin;

        public StatsApiHandler(EnoTab plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!validateSession(exchange)) {
                sendResponse(exchange, 401, "{\"error\": \"Unauthorized\"}", "application/json");
                return;
            }

            Map<String, Object> stats = new HashMap<>();
            stats.put("onlinePlayers", Bukkit.getOnlinePlayers().size());
            stats.put("maxPlayers", Bukkit.getMaxPlayers());
            stats.put("tps", getTPS());
            stats.put("serverName", Bukkit.getServer().getName());
            stats.put("version", Bukkit.getVersion());
            stats.put("motd", Bukkit.getServer().getMotd());
            stats.put("worlds", Bukkit.getWorlds().size());

            Runtime runtime = Runtime.getRuntime();
            stats.put("usedMemory", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
            stats.put("totalMemory", runtime.totalMemory() / 1024 / 1024);
            stats.put("maxMemory", runtime.maxMemory() / 1024 / 1024);

            String json = gson.toJson(stats);
            sendResponse(exchange, 200, json, "application/json");
        }

        private double getTPS() {
            try {
                Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
                double[] tps = (double[]) server.getClass().getField("recentTps").get(server);
                return Math.min(tps[0], 20.0);
            } catch (Exception e) {
                return 20.0;
            }
        }
    }

    // CSS handler
    public static class CssHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String css = getCssContent();
            sendResponse(exchange, 200, css, "text/css");
        }
    }

    // JS handler
    public static class JsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String js = getJsContent();
            sendResponse(exchange, 200, js, "application/javascript");
        }
    }

    // Presets List API handler
    public static class PresetsListHandler implements HttpHandler {
        private final EnoTab plugin;

        public PresetsListHandler(EnoTab plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!validateSession(exchange)) {
                sendResponse(exchange, 401, "{\"error\": \"Unauthorized\"}", "application/json");
                return;
            }

            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                List<Map<String, String>> presets = getDefaultPresets();
                // Kullanıcı şablonlarını da ekle
                File presetsDir = new File(plugin.getDataFolder(), "presets");
                if (presetsDir.exists()) {
                    for (File file : presetsDir.listFiles((dir, name) -> name.endsWith(".json"))) {
                        Map<String, String> preset = new HashMap<>();
                        preset.put("name", file.getName().replace(".json", ""));
                        preset.put("type", "custom");
                        presets.add(preset);
                    }
                }
                sendResponse(exchange, 200, gson.toJson(presets), "application/json");
            } else if ("POST".equals(method)) {
                String body = readBody(exchange);
                Map<String, Object> data = gson.fromJson(body, Map.class);
                String name = (String) data.get("name");
                Map<String, Object> configData = (Map<String, Object>) data.get("config");

                File presetsDir = new File(plugin.getDataFolder(), "presets");
                if (!presetsDir.exists()) presetsDir.mkdirs();

                File presetFile = new File(presetsDir, name + ".json");
                try (FileWriter writer = new FileWriter(presetFile)) {
                    gson.toJson(configData, writer);
                }
                sendResponse(exchange, 200, "{\"success\": true}", "application/json");
            } else {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}", "application/json");
            }
        }

        private List<Map<String, String>> getDefaultPresets() {
            List<Map<String, String>> presets = new ArrayList<>();
            String[] defaultNames = {"🎮 Modern Gaming", "⚔️ PvP Server", "🏰 Survival", "🌟 Premium", "🎨 Minimalist", "🔥 Hardcore"};
            for (String name : defaultNames) {
                Map<String, String> preset = new HashMap<>();
                preset.put("name", name);
                preset.put("type", "default");
                presets.add(preset);
            }
            return presets;
        }
    }

    // Preset Detail API handler
    public static class PresetDetailHandler implements HttpHandler {
        private final EnoTab plugin;

        public PresetDetailHandler(EnoTab plugin) {
            this.plugin = plugin;
        }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!validateSession(exchange)) {
                sendResponse(exchange, 401, "{\"error\": \"Unauthorized\"}", "application/json");
                return;
            }

            String path = exchange.getRequestURI().getPath();
            String presetName = java.net.URLDecoder.decode(path.substring("/api/presets/".length()), "UTF-8");
            String method = exchange.getRequestMethod();

            if ("GET".equals(method)) {
                Map<String, Object> result = new HashMap<>();
                result.put("name", presetName);

                Map<String, Object> configData = getPresetConfig(presetName);
                if (configData == null) {
                    sendResponse(exchange, 404, "{\"error\": \"Preset not found\"}", "application/json");
                    return;
                }
                result.put("config", configData);
                sendResponse(exchange, 200, gson.toJson(result), "application/json");
            } else if ("DELETE".equals(method)) {
                File presetsDir = new File(plugin.getDataFolder(), "presets");
                File presetFile = new File(presetsDir, presetName + ".json");
                if (presetFile.exists()) {
                    presetFile.delete();
                    sendResponse(exchange, 200, "{\"success\": true}", "application/json");
                } else {
                    sendResponse(exchange, 404, "{\"error\": \"Preset not found\"}", "application/json");
                }
            } else {
                sendResponse(exchange, 405, "{\"error\": \"Method not allowed\"}", "application/json");
            }
        }

        private Map<String, Object> getPresetConfig(String name) {
            // Varsayılan şablonlar
            Map<String, Object> config = new HashMap<>();

            switch (name) {
                case "🎮 Modern Gaming":
                    config.put("header", "&b&l⚡ &f&lGAMING SERVER &b&l⚡\\n&7━━━━━━━━━━━━━━━━━━━━━━━\\n&fHoş geldin, &b%player%&f!");
                    config.put("footer", "&7━━━━━━━━━━━━━━━━━━━━━━━\\n&b⚡ &fOyuncular: &b%online%&7/&b%max% &8| &fTPS: &a%tps%\\n&b⚡ &fPing: &a%ping%ms &8| &fDünya: &e%world%\\n&7━━━━━━━━━━━━━━━━━━━━━━━");
                    config.put("playerFormat", "&b⚡ &f%player%");
                    config.put("animatedHeader", true);
                    config.put("animatedFooter", false);
                    config.put("headerFrames", Arrays.asList(
                        "&b&l⚡ &f&lGAMING SERVER &b&l⚡",
                        "&3&l⚡ &f&lGAMING SERVER &3&l⚡",
                        "&9&l⚡ &f&lGAMING SERVER &9&l⚡"
                    ));
                    config.put("footerFrames", new ArrayList<>());
                    config.put("animationInterval", 15);
                    config.put("updateInterval", 40);
                    config.put("showPing", true);
                    config.put("showHealth", false);
                    break;

                case "⚔️ PvP Server":
                    config.put("header", "&c&l⚔ &4&lPVP ARENA &c&l⚔\\n&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\\n&7Savaşçı: &c%player%");
                    config.put("footer", "&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬\\n&c❤ &fCan: &c%health%&7/&c%maxhealth% &8| &e⚔ &fKill: &e0\\n&a✦ &fOyuncular: &a%online% &8| &b⌚ &fPing: &b%ping%ms\\n&8▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬▬");
                    config.put("playerFormat", "&c⚔ &7%player% &8[&c%health%❤&8]");
                    config.put("animatedHeader", true);
                    config.put("animatedFooter", false);
                    config.put("headerFrames", Arrays.asList(
                        "&c&l⚔ &4&lPVP ARENA &c&l⚔",
                        "&4&l⚔ &c&lPVP ARENA &4&l⚔",
                        "&c&l⚔ &f&lPVP ARENA &c&l⚔"
                    ));
                    config.put("footerFrames", new ArrayList<>());
                    config.put("animationInterval", 10);
                    config.put("updateInterval", 20);
                    config.put("showPing", true);
                    config.put("showHealth", true);
                    break;

                case "🏰 Survival":
                    config.put("header", "&2&l✿ &a&lSURVIVAL WORLD &2&l✿\\n&7═══════════════════════\\n&fMaceracı &a%player% &fhoş geldin!");
                    config.put("footer", "&7═══════════════════════\\n&2🌍 &fDünya: &a%world% &8| &2📍 &fKonum: &a%x%&7, &a%y%&7, &a%z%\\n&2👥 &fOyuncular: &a%online%&7/&a%max% &8| &2🕐 &f%time%\\n&7═══════════════════════");
                    config.put("playerFormat", "&a✿ &f%player% &7[Lvl &e%level%&7]");
                    config.put("animatedHeader", false);
                    config.put("animatedFooter", true);
                    config.put("headerFrames", new ArrayList<>());
                    config.put("footerFrames", Arrays.asList(
                        "&2🌍 &fDünya: &a%world% &8| &2📍 &fKonum: &a%x%&7, &a%y%&7, &a%z%",
                        "&2👥 &fOyuncular: &a%online%&7/&a%max% &8| &2⏰ &f%time%",
                        "&2💚 &fCan: &a%health% &8| &2🍖 &fAçlık: &e%food%"
                    ));
                    config.put("animationInterval", 40);
                    config.put("updateInterval", 40);
                    config.put("showPing", true);
                    config.put("showHealth", true);
                    break;

                case "🌟 Premium":
                    config.put("header", "&6&l✦ &e&lPREMIUM &6&lSERVER &e&l✦\\n&8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯\\n&e★ &6VIP &fÜye: &e%player% &e★");
                    config.put("footer", "&8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯\\n&6✦ &fSunucu: &ePREMIUM &8| &6✦ &fTPS: &a%tps%\\n&6✦ &fOyuncular: &e%online%&7/&e%max% &8| &6✦ &fPing: &a%ping%ms\\n&e&lwww.sunucu.com\\n&8⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯⎯");
                    config.put("playerFormat", "&6✦ &e%player%");
                    config.put("animatedHeader", true);
                    config.put("animatedFooter", false);
                    config.put("headerFrames", Arrays.asList(
                        "&6&l✦ &e&lPREMIUM &6&lSERVER &e&l✦",
                        "&e&l✦ &6&lPREMIUM &e&lSERVER &6&l✦",
                        "&f&l✦ &e&lPREMIUM &f&lSERVER &e&l✦"
                    ));
                    config.put("footerFrames", new ArrayList<>());
                    config.put("animationInterval", 20);
                    config.put("updateInterval", 40);
                    config.put("showPing", true);
                    config.put("showHealth", false);
                    break;

                case "🎨 Minimalist":
                    config.put("header", "&f&lMINECRAFT\\n&7%player%");
                    config.put("footer", "&7%online% oyuncu çevrimiçi\\n&8%time% | %date%");
                    config.put("playerFormat", "&7• &f%player%");
                    config.put("animatedHeader", false);
                    config.put("animatedFooter", false);
                    config.put("headerFrames", new ArrayList<>());
                    config.put("footerFrames", new ArrayList<>());
                    config.put("animationInterval", 20);
                    config.put("updateInterval", 60);
                    config.put("showPing", false);
                    config.put("showHealth", false);
                    break;

                case "🔥 Hardcore":
                    config.put("header", "&4&l☠ &c&lHARDCORE &4&l☠\\n&8━━━━━━━━━━━━━━━━━━━\\n&7Cesur savaşçı &c%player%");
                    config.put("footer", "&8━━━━━━━━━━━━━━━━━━━\\n&4☠ &cCan: &4%health%&8/&4%maxhealth% &7| &4☠ &cAçlık: &4%food%\\n&4⚠ &cÖlürsen her şeyi kaybedersin!\\n&8━━━━━━━━━━━━━━━━━━━");
                    config.put("playerFormat", "&4☠ &c%player% &8[&4%health%❤&8]");
                    config.put("animatedHeader", true);
                    config.put("animatedFooter", false);
                    config.put("headerFrames", Arrays.asList(
                        "&4&l☠ &c&lHARDCORE &4&l☠",
                        "&c&l☠ &4&lHARDCORE &c&l☠",
                        "&4&l☠ &f&lHARDCORE &4&l☠"
                    ));
                    config.put("footerFrames", new ArrayList<>());
                    config.put("animationInterval", 8);
                    config.put("updateInterval", 20);
                    config.put("showPing", true);
                    config.put("showHealth", true);
                    break;

                default:
                    // Kullanıcı şablonu
                    File presetsDir = new File(plugin.getDataFolder(), "presets");
                    File presetFile = new File(presetsDir, name + ".json");
                    if (presetFile.exists()) {
                        try (FileReader reader = new FileReader(presetFile)) {
                            return gson.fromJson(reader, Map.class);
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
            }

            return config;
        }
    }

    // Yardımcı metodlar
    private static void sendResponse(HttpExchange exchange, int statusCode, String response, String contentType) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, bytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private static Map<String, String> parseFormData(String body) {
        Map<String, String> params = new HashMap<>();
        if (body != null && !body.isEmpty()) {
            for (String param : body.split("&")) {
                String[] pair = param.split("=");
                if (pair.length == 2) {
                    try {
                        params.put(java.net.URLDecoder.decode(pair[0], "UTF-8"),
                                   java.net.URLDecoder.decode(pair[1], "UTF-8"));
                    } catch (Exception e) {
                        params.put(pair[0], pair[1]);
                    }
                }
            }
        }
        return params;
    }

    private static String getLoginTemplate() {
        return """
<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>EnoTab - Giriş</title>
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: 'Inter', sans-serif;
            background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
            min-height: 100vh;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #fff;
        }
        .login-container {
            background: rgba(255, 255, 255, 0.05);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 20px;
            padding: 40px;
            width: 100%;
            max-width: 400px;
            backdrop-filter: blur(10px);
        }
        .login-logo {
            text-align: center;
            margin-bottom: 30px;
        }
        .login-logo i {
            font-size: 50px;
            color: #00d9ff;
            margin-bottom: 15px;
        }
        .login-logo h1 {
            font-size: 28px;
            font-weight: 700;
            color: #00d9ff;
        }
        .login-logo p {
            color: rgba(255, 255, 255, 0.6);
            margin-top: 5px;
        }
        .form-group {
            margin-bottom: 20px;
        }
        .form-group label {
            display: block;
            margin-bottom: 8px;
            font-weight: 500;
            color: rgba(255, 255, 255, 0.9);
        }
        .form-group input {
            width: 100%;
            padding: 14px 16px;
            background: rgba(0, 0, 0, 0.3);
            border: 1px solid rgba(255, 255, 255, 0.1);
            border-radius: 10px;
            color: #fff;
            font-size: 15px;
            transition: all 0.3s ease;
        }
        .form-group input:focus {
            outline: none;
            border-color: #00d9ff;
            box-shadow: 0 0 0 3px rgba(0, 217, 255, 0.1);
        }
        .btn-login {
            width: 100%;
            padding: 14px;
            background: linear-gradient(135deg, #00d9ff 0%, #00a8cc 100%);
            border: none;
            border-radius: 10px;
            color: #fff;
            font-size: 16px;
            font-weight: 600;
            cursor: pointer;
            transition: all 0.3s ease;
        }
        .btn-login:hover {
            transform: translateY(-2px);
            box-shadow: 0 5px 20px rgba(0, 217, 255, 0.4);
        }
        .error-msg {
            background: rgba(255, 87, 87, 0.2);
            border: 1px solid #ff5757;
            color: #ff5757;
            padding: 12px;
            border-radius: 8px;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
            gap: 10px;
        }
        .input-icon {
            position: relative;
        }
        .input-icon i {
            position: absolute;
            left: 14px;
            top: 50%;
            transform: translateY(-50%);
            color: rgba(255, 255, 255, 0.4);
        }
        .input-icon input {
            padding-left: 45px;
        }
    </style>
</head>
<body>
    <div class="login-container">
        <div class="login-logo">
            <i class="fas fa-tablet-alt"></i>
            <h1>EnoTab</h1>
            <p>Web Kontrol Paneli</p>
        </div>
        <!--ERROR-->
        <form method="POST" action="/login">
            <div class="form-group">
                <label for="username">Kullanıcı Adı</label>
                <div class="input-icon">
                    <i class="fas fa-user"></i>
                    <input type="text" id="username" name="username" placeholder="Kullanıcı adınızı girin" required>
                </div>
            </div>
            <div class="form-group">
                <label for="password">Şifre</label>
                <div class="input-icon">
                    <i class="fas fa-lock"></i>
                    <input type="password" id="password" name="password" placeholder="Şifrenizi girin" required>
                </div>
            </div>
            <button type="submit" class="btn-login">
                <i class="fas fa-sign-in-alt"></i> Giriş Yap
            </button>
        </form>
    </div>
</body>
</html>
""";
    }

    private static String getHtmlTemplate() {
        return """
<!DOCTYPE html>
<html lang="tr">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>EnoTab - Web Panel</title>
    <link rel="stylesheet" href="/assets/style.css">
    <link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap" rel="stylesheet">
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
</head>
<body>
    <div class="container">
        <nav class="sidebar">
            <div class="logo">
                <i class="fas fa-tablet-alt"></i>
                <span>EnoTab</span>
            </div>
            <ul class="nav-menu">
                <li class="nav-item active" data-tab="dashboard">
                    <i class="fas fa-home"></i>
                    <span>Dashboard</span>
                </li>
                <li class="nav-item" data-tab="presets">
                    <i class="fas fa-magic"></i>
                    <span>Hazır Şablonlar</span>
                </li>
                <li class="nav-item" data-tab="tabconfig">
                    <i class="fas fa-list"></i>
                    <span>Tab Ayarları</span>
                </li>
                <li class="nav-item" data-tab="animation">
                    <i class="fas fa-film"></i>
                    <span>Animasyonlar</span>
                </li>
                <li class="nav-item" data-tab="players">
                    <i class="fas fa-users"></i>
                    <span>Oyuncular</span>
                </li>
                <li class="nav-item" data-tab="placeholders">
                    <i class="fas fa-code"></i>
                    <span>Placeholder'lar</span>
                </li>
            </ul>
            <div class="sidebar-footer">
                <a href="/logout" class="logout-btn">
                    <i class="fas fa-sign-out-alt"></i>
                    <span>Çıkış Yap</span>
                </a>
            </div>
        </nav>
        
        <main class="main-content">
            <header class="header">
                <h1 id="page-title">Dashboard</h1>
                <div class="header-actions">
                    <button class="btn btn-primary" onclick="saveConfig()">
                        <i class="fas fa-save"></i> Kaydet
                    </button>
                    <button class="btn btn-secondary" onclick="loadConfig()">
                        <i class="fas fa-sync"></i> Yenile
                    </button>
                </div>
            </header>
            
            <!-- Dashboard -->
            <section id="dashboard" class="tab-content active">
                <div class="stats-grid">
                    <div class="stat-card">
                        <div class="stat-icon"><i class="fas fa-users"></i></div>
                        <div class="stat-info">
                            <span class="stat-value" id="online-players">0</span>
                            <span class="stat-label">Çevrimiçi Oyuncu</span>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon"><i class="fas fa-server"></i></div>
                        <div class="stat-info">
                            <span class="stat-value" id="tps">20.0</span>
                            <span class="stat-label">TPS</span>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon"><i class="fas fa-memory"></i></div>
                        <div class="stat-info">
                            <span class="stat-value" id="memory">0 MB</span>
                            <span class="stat-label">RAM Kullanımı</span>
                        </div>
                    </div>
                    <div class="stat-card">
                        <div class="stat-icon"><i class="fas fa-globe"></i></div>
                        <div class="stat-info">
                            <span class="stat-value" id="worlds">0</span>
                            <span class="stat-label">Dünya</span>
                        </div>
                    </div>
                </div>
                
                <div class="card">
                    <h3><i class="fas fa-info-circle"></i> Sunucu Bilgisi</h3>
                    <div class="info-grid">
                        <div class="info-item">
                            <span class="info-label">Versiyon:</span>
                            <span class="info-value" id="server-version">-</span>
                        </div>
                        <div class="info-item">
                            <span class="info-label">MOTD:</span>
                            <span class="info-value" id="server-motd">-</span>
                        </div>
                    </div>
                </div>
            </section>
            
            <!-- Şablonlar -->
            <section id="presets" class="tab-content">
                <div class="card">
                    <h3><i class="fas fa-magic"></i> Hazır Tab Şablonları</h3>
                    <p>Sunucu ayarlarınıza uygun hazır tab şablonlarını buradan yükleyebilir veya mevcut şablonlarınızı kaydedebilirsiniz.</p>
                    
                    <div class="form-group">
                        <label for="preset-select">Şablon Seçin</label>
                        <select id="preset-select" onchange="loadPreset(this.value)">
                            <option value="">-- Şablon Seçin --</option>
                        </select>
                    </div>
                    
                    <div class="form-group">
                        <label for="preset-name">Şablon Adı</label>
                        <input type="text" id="preset-name" placeholder="Yeni şablon adı">
                    </div>
                    
                    <div class="form-actions">
                        <button class="btn btn-primary" onclick="savePreset()">
                            <i class="fas fa-save"></i> Şablonu Kaydet
                        </button>
                        <button class="btn btn-secondary" onclick="deletePreset()">
                            <i class="fas fa-trash"></i> Şablonu Sil
                        </button>
                    </div>
                </div>
            </section>
            
            <!-- Tab Ayarları -->
            <section id="tabconfig" class="tab-content">
                <div class="card">
                    <h3><i class="fas fa-heading"></i> Header & Footer</h3>
                    <div class="form-group">
                        <label for="header-text">Header Metni</label>
                        <textarea id="header-text" rows="3" placeholder="Tab header metni...">&6&lEnoTab &7- &eSunucu</textarea>
                        <small>Renk kodları için & kullanın (örn: &a yeşil, &c kırmızı)</small>
                    </div>
                    <div class="form-group">
                        <label for="footer-text">Footer Metni</label>
                        <textarea id="footer-text" rows="3" placeholder="Tab footer metni...">&7Oyuncular: &e%online%&7/&e%max%</textarea>
                    </div>
                </div>
                
                <div class="card">
                    <h3><i class="fas fa-user"></i> Oyuncu Formatı</h3>
                    <div class="form-group">
                        <label for="player-format">Oyuncu Listesi Formatı</label>
                        <input type="text" id="player-format" value="&f%player%">
                        <small>%player%, %ping%, %health% gibi placeholder'lar kullanabilirsiniz</small>
                    </div>
                </div>
                
                <div class="card">
                    <h3><i class="fas fa-cog"></i> Genel Ayarlar</h3>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="update-interval">Güncelleme Aralığı (tick)</label>
                            <input type="number" id="update-interval" value="40" min="10" max="200">
                        </div>
                        <div class="form-group">
                            <label class="checkbox-label">
                                <input type="checkbox" id="show-ping">
                                <span>Ping Göster</span>
                            </label>
                        </div>
                        <div class="form-group">
                            <label class="checkbox-label">
                                <input type="checkbox" id="show-health">
                                <span>Can Göster</span>
                            </label>
                        </div>
                    </div>
                </div>
            </section>
            
            <!-- Animasyonlar -->
            <section id="animation" class="tab-content">
                <div class="card">
                    <h3><i class="fas fa-magic"></i> Header Animasyonu</h3>
                    <div class="form-group">
                        <label class="checkbox-label">
                            <input type="checkbox" id="animated-header">
                            <span>Header Animasyonu Aktif</span>
                        </label>
                    </div>
                    <div class="form-group">
                        <label>Header Frameleri</label>
                        <div id="header-frames-container" class="frames-container">
                        </div>
                        <button class="btn btn-small" onclick="addHeaderFrame()">
                            <i class="fas fa-plus"></i> Frame Ekle
                        </button>
                    </div>
                </div>
                
                <div class="card">
                    <h3><i class="fas fa-magic"></i> Footer Animasyonu</h3>
                    <div class="form-group">
                        <label class="checkbox-label">
                            <input type="checkbox" id="animated-footer">
                            <span>Footer Animasyonu Aktif</span>
                        </label>
                    </div>
                    <div class="form-group">
                        <label>Footer Frameleri</label>
                        <div id="footer-frames-container" class="frames-container">
                        </div>
                        <button class="btn btn-small" onclick="addFooterFrame()">
                            <i class="fas fa-plus"></i> Frame Ekle
                        </button>
                    </div>
                </div>
                
                <div class="card">
                    <h3><i class="fas fa-clock"></i> Animasyon Ayarları</h3>
                    <div class="form-group">
                        <label for="animation-interval">Animasyon Hızı (tick)</label>
                        <input type="number" id="animation-interval" value="20" min="5" max="100">
                        <small>Düşük değer = Hızlı animasyon (20 tick = 1 saniye)</small>
                    </div>
                </div>
            </section>
            
            <!-- Oyuncular -->
            <section id="players" class="tab-content">
                <div class="card">
                    <h3><i class="fas fa-users"></i> Çevrimiçi Oyuncular</h3>
                    <div class="table-container">
                        <table id="players-table">
                            <thead>
                                <tr>
                                    <th>İsim</th>
                                    <th>Ping</th>
                                    <th>Can</th>
                                    <th>Seviye</th>
                                    <th>Dünya</th>
                                    <th>Mod</th>
                                </tr>
                            </thead>
                            <tbody>
                            </tbody>
                        </table>
                    </div>
                </div>
            </section>
            
            <!-- Placeholder'lar -->
            <section id="placeholders" class="tab-content">
                <div class="card">
                    <h3><i class="fas fa-code"></i> Kullanılabilir Placeholder'lar</h3>
                    <div class="placeholder-grid">
                        <div class="placeholder-category">
                            <h4>Sunucu</h4>
                            <ul>
                                <li><code>%online%</code> - Çevrimiçi oyuncu sayısı</li>
                                <li><code>%max%</code> - Maksimum oyuncu sayısı</li>
                                <li><code>%tps%</code> - Sunucu TPS</li>
                                <li><code>%server%</code> - Sunucu adı</li>
                                <li><code>%motd%</code> - Sunucu MOTD</li>
                            </ul>
                        </div>
                        <div class="placeholder-category">
                            <h4>Oyuncu</h4>
                            <ul>
                                <li><code>%player%</code> - Oyuncu adı</li>
                                <li><code>%displayname%</code> - Görünen ad</li>
                                <li><code>%ping%</code> - Ping değeri</li>
                                <li><code>%health%</code> - Can</li>
                                <li><code>%maxhealth%</code> - Maksimum can</li>
                                <li><code>%food%</code> - Açlık</li>
                                <li><code>%level%</code> - Seviye</li>
                                <li><code>%exp%</code> - Deneyim %</li>
                                <li><code>%world%</code> - Dünya adı</li>
                                <li><code>%gamemode%</code> - Oyun modu</li>
                            </ul>
                        </div>
                        <div class="placeholder-category">
                            <h4>Konum</h4>
                            <ul>
                                <li><code>%x%</code> - X koordinatı</li>
                                <li><code>%y%</code> - Y koordinatı</li>
                                <li><code>%z%</code> - Z koordinatı</li>
                            </ul>
                        </div>
                        <div class="placeholder-category">
                            <h4>Zaman</h4>
                            <ul>
                                <li><code>%time%</code> - Saat (HH:mm)</li>
                                <li><code>%date%</code> - Tarih (dd/MM/yyyy)</li>
                            </ul>
                        </div>
                    </div>
                </div>
                
                <div class="card">
                    <h3><i class="fas fa-palette"></i> Renk Kodları</h3>
                    <div class="color-codes">
                        <span class="color-code" style="color: #000000">&0 Siyah</span>
                        <span class="color-code" style="color: #0000AA">&1 Koyu Mavi</span>
                        <span class="color-code" style="color: #00AA00">&2 Koyu Yeşil</span>
                        <span class="color-code" style="color: #00AAAA">&3 Koyu Aqua</span>
                        <span class="color-code" style="color: #AA0000">&4 Koyu Kırmızı</span>
                        <span class="color-code" style="color: #AA00AA">&5 Mor</span>
                        <span class="color-code" style="color: #FFAA00">&6 Altın</span>
                        <span class="color-code" style="color: #AAAAAA">&7 Gri</span>
                        <span class="color-code" style="color: #555555">&8 Koyu Gri</span>
                        <span class="color-code" style="color: #5555FF">&9 Mavi</span>
                        <span class="color-code" style="color: #55FF55">&a Yeşil</span>
                        <span class="color-code" style="color: #55FFFF">&b Aqua</span>
                        <span class="color-code" style="color: #FF5555">&c Kırmızı</span>
                        <span class="color-code" style="color: #FF55FF">&d Pembe</span>
                        <span class="color-code" style="color: #FFFF55">&e Sarı</span>
                        <span class="color-code" style="color: #FFFFFF; background: #333; padding: 2px 5px;">&f Beyaz</span>
                    </div>
                    <div class="format-codes" style="margin-top: 15px;">
                        <span class="format-code"><b>&l</b> Kalın</span>
                        <span class="format-code"><i>&o</i> İtalik</span>
                        <span class="format-code"><u>&n</u> Altı Çizili</span>
                        <span class="format-code"><s>&m</s> Üstü Çizili</span>
                        <span class="format-code">&k Karışık</span>
                        <span class="format-code">&r Sıfırla</span>
                    </div>
                </div>
            </section>
        </main>
    </div>
    
    <div id="toast" class="toast"></div>
    
    <script src="/assets/script.js"></script>
</body>
</html>
""";
    }

    private static String getCssContent() {
        return """
* {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
}

body {
    font-family: 'Inter', sans-serif;
    background: linear-gradient(135deg, #1a1a2e 0%, #16213e 100%);
    min-height: 100vh;
    color: #fff;
}

.container {
    display: flex;
    min-height: 100vh;
}

.sidebar {
    width: 260px;
    background: rgba(0, 0, 0, 0.3);
    backdrop-filter: blur(10px);
    padding: 20px;
    border-right: 1px solid rgba(255, 255, 255, 0.1);
    display: flex;
    flex-direction: column;
}

.logo {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 15px;
    margin-bottom: 30px;
    font-size: 24px;
    font-weight: 700;
    color: #00d9ff;
}

.logo i { font-size: 28px; }

.nav-menu {
    list-style: none;
    flex: 1;
}

.nav-item {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    margin-bottom: 8px;
    border-radius: 10px;
    cursor: pointer;
    transition: all 0.3s ease;
    color: rgba(255, 255, 255, 0.7);
}

.nav-item:hover {
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
}

.nav-item.active {
    background: linear-gradient(135deg, #00d9ff 0%, #00a8cc 100%);
    color: #fff;
    box-shadow: 0 4px 15px rgba(0, 217, 255, 0.3);
}

.nav-item i { width: 20px; text-align: center; }

.sidebar-footer {
    padding-top: 20px;
    border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.logout-btn {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 14px 18px;
    border-radius: 10px;
    color: #ff5757;
    text-decoration: none;
    transition: all 0.3s ease;
}

.logout-btn:hover {
    background: rgba(255, 87, 87, 0.2);
}

.main-content {
    flex: 1;
    padding: 30px;
    overflow-y: auto;
}

.header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30px;
}

.header h1 { font-size: 28px; font-weight: 600; }

.header-actions { display: flex; gap: 12px; }

.btn {
    display: inline-flex;
    align-items: center;
    gap: 8px;
    padding: 12px 24px;
    border: none;
    border-radius: 8px;
    font-size: 14px;
    font-weight: 500;
    cursor: pointer;
    transition: all 0.3s ease;
}

.btn-primary {
    background: linear-gradient(135deg, #00d9ff 0%, #00a8cc 100%);
    color: #fff;
}

.btn-primary:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 15px rgba(0, 217, 255, 0.4);
}

.btn-secondary {
    background: rgba(255, 255, 255, 0.1);
    color: #fff;
    border: 1px solid rgba(255, 255, 255, 0.2);
}

.btn-secondary:hover { background: rgba(255, 255, 255, 0.2); }

.btn-small {
    padding: 8px 16px;
    font-size: 13px;
    background: rgba(0, 217, 255, 0.2);
    color: #00d9ff;
    border: 1px solid #00d9ff;
}

.btn-small:hover { background: rgba(0, 217, 255, 0.3); }

.stats-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 20px;
    margin-bottom: 30px;
}

.stat-card {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 15px;
    padding: 25px;
    display: flex;
    align-items: center;
    gap: 20px;
    transition: all 0.3s ease;
}

.stat-card:hover {
    transform: translateY(-5px);
    border-color: rgba(0, 217, 255, 0.3);
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2);
}

.stat-icon {
    width: 60px;
    height: 60px;
    background: linear-gradient(135deg, #00d9ff 0%, #00a8cc 100%);
    border-radius: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 24px;
}

.stat-value { font-size: 28px; font-weight: 700; display: block; }
.stat-label { color: rgba(255, 255, 255, 0.6); font-size: 14px; }

.card {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 15px;
    padding: 25px;
    margin-bottom: 20px;
}

.card h3 {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 20px;
    display: flex;
    align-items: center;
    gap: 10px;
}

.card h3 i { color: #00d9ff; }

.form-group { margin-bottom: 20px; }

.form-group label {
    display: block;
    margin-bottom: 8px;
    font-weight: 500;
    color: rgba(255, 255, 255, 0.9);
}

.form-group input[type="text"],
.form-group input[type="number"],
.form-group textarea,
.form-group select {
    width: 100%;
    padding: 12px 16px;
    background: rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 8px;
    color: #fff;
    font-size: 14px;
    font-family: 'Consolas', monospace;
    transition: all 0.3s ease;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
    outline: none;
    border-color: #00d9ff;
    box-shadow: 0 0 0 3px rgba(0, 217, 255, 0.1);
}

.form-group select option {
    background: #1a1a2e;
    color: #fff;
}

.form-actions {
    display: flex;
    gap: 12px;
    margin-top: 20px;
}

.form-row { display: flex; gap: 20px; flex-wrap: wrap; }
.form-row .form-group { flex: 1; min-width: 200px; }

.checkbox-label {
    display: flex;
    align-items: center;
    gap: 10px;
    cursor: pointer;
}

.checkbox-label input[type="checkbox"] {
    width: 20px;
    height: 20px;
    accent-color: #00d9ff;
}

.tab-content { display: none; }
.tab-content.active {
    display: block;
    animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(10px); }
    to { opacity: 1; transform: translateY(0); }
}

.frames-container {
    display: flex;
    flex-direction: column;
    gap: 10px;
    margin-bottom: 15px;
}

.frame-item { display: flex; gap: 10px; align-items: center; }

.frame-item input {
    flex: 1;
    padding: 10px 14px;
    background: rgba(0, 0, 0, 0.3);
    border: 1px solid rgba(255, 255, 255, 0.1);
    border-radius: 6px;
    color: #fff;
    font-family: 'Consolas', monospace;
}

.frame-item .btn-remove {
    padding: 8px 12px;
    background: rgba(255, 87, 87, 0.2);
    border: 1px solid #ff5757;
    color: #ff5757;
    border-radius: 6px;
    cursor: pointer;
    transition: all 0.3s ease;
}

.frame-item .btn-remove:hover { background: rgba(255, 87, 87, 0.3); }

.table-container { overflow-x: auto; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 14px; text-align: left; border-bottom: 1px solid rgba(255, 255, 255, 0.1); }
th { background: rgba(0, 0, 0, 0.2); font-weight: 600; color: #00d9ff; }
tr:hover { background: rgba(255, 255, 255, 0.05); }

.placeholder-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
    gap: 25px;
}

.placeholder-category h4 { color: #00d9ff; margin-bottom: 12px; font-size: 16px; }
.placeholder-category ul { list-style: none; }
.placeholder-category li { padding: 8px 0; border-bottom: 1px solid rgba(255, 255, 255, 0.05); font-size: 14px; }

.placeholder-category code {
    background: rgba(0, 217, 255, 0.1);
    padding: 2px 8px;
    border-radius: 4px;
    color: #00d9ff;
    font-family: 'Consolas', monospace;
}

.color-codes, .format-codes { display: flex; flex-wrap: wrap; gap: 10px; }

.color-code, .format-code {
    padding: 6px 12px;
    background: rgba(0, 0, 0, 0.3);
    border-radius: 6px;
    font-family: 'Consolas', monospace;
    font-size: 13px;
}

.info-grid {
    display: grid;
    grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
    gap: 15px;
}

.info-item { display: flex; flex-direction: column; gap: 5px; }
.info-label { color: rgba(255, 255, 255, 0.6); font-size: 13px; }
.info-value { font-weight: 500; }

.toast {
    position: fixed;
    bottom: 30px;
    right: 30px;
    padding: 15px 25px;
    background: #00d9ff;
    color: #000;
    border-radius: 10px;
    font-weight: 500;
    transform: translateY(100px);
    opacity: 0;
    transition: all 0.3s ease;
    z-index: 1000;
}

.toast.show { transform: translateY(0); opacity: 1; }
.toast.error { background: #ff5757; color: #fff; }
.toast.success { background: #00c853; color: #fff; }

@media (max-width: 768px) {
    .container { flex-direction: column; }
    .sidebar { width: 100%; border-right: none; border-bottom: 1px solid rgba(255, 255, 255, 0.1); }
    .nav-menu { display: flex; overflow-x: auto; gap: 10px; padding-bottom: 10px; }
    .nav-item { white-space: nowrap; }
    .header { flex-direction: column; gap: 15px; align-items: flex-start; }
}
""";
    }

    private static String getJsContent() {
        return """
let config = {};

document.addEventListener('DOMContentLoaded', () => {
    loadConfig();
    loadStats();
    loadPlayers();
    loadPresets();
    
    setInterval(loadStats, 5000);
    setInterval(loadPlayers, 5000);
    
    document.querySelectorAll('.nav-item').forEach(item => {
        item.addEventListener('click', () => {
            const tab = item.dataset.tab;
            switchTab(tab);
        });
    });
});

function switchTab(tabId) {
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
        if (item.dataset.tab === tabId) {
            item.classList.add('active');
        }
    });
    
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });
    document.getElementById(tabId).classList.add('active');
    
    const titles = {
        'dashboard': 'Dashboard',
        'presets': 'Hazır Şablonlar',
        'tabconfig': 'Tab Ayarları',
        'animation': 'Animasyonlar',
        'players': 'Oyuncular',
        'placeholders': 'Placeholder\\'lar'
    };
    document.getElementById('page-title').textContent = titles[tabId] || tabId;
}

async function loadConfig() {
    try {
        const response = await fetch('/api/config');
        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }
        config = await response.json();
        updateUI();
        showToast('Yapılandırma yüklendi', 'success');
    } catch (error) {
        console.error('Config yüklenemedi:', error);
        showToast('Yapılandırma yüklenemedi', 'error');
    }
}

async function saveConfig() {
    try {
        collectFormData();
        
        const response = await fetch('/api/config', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(config)
        });
        
        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }
        
        if (response.ok) {
            showToast('Yapılandırma kaydedildi!', 'success');
        } else {
            showToast('Kaydetme başarısız', 'error');
        }
    } catch (error) {
        console.error('Config kaydedilemedi:', error);
        showToast('Kaydetme başarısız', 'error');
    }
}

function updateUI() {
    document.getElementById('header-text').value = config.header || '';
    document.getElementById('footer-text').value = config.footer || '';
    document.getElementById('player-format').value = config.playerFormat || '';
    
    document.getElementById('animated-header').checked = config.animatedHeader || false;
    document.getElementById('animated-footer').checked = config.animatedFooter || false;
    document.getElementById('animation-interval').value = config.animationInterval || 20;
    
    document.getElementById('update-interval').value = config.updateInterval || 40;
    document.getElementById('show-ping').checked = config.showPing || false;
    document.getElementById('show-health').checked = config.showHealth || false;
    
    updateHeaderFrames();
    updateFooterFrames();
}

function collectFormData() {
    config.header = document.getElementById('header-text').value;
    config.footer = document.getElementById('footer-text').value;
    config.playerFormat = document.getElementById('player-format').value;
    
    config.animatedHeader = document.getElementById('animated-header').checked;
    config.animatedFooter = document.getElementById('animated-footer').checked;
    config.animationInterval = parseInt(document.getElementById('animation-interval').value) || 20;
    
    config.updateInterval = parseInt(document.getElementById('update-interval').value) || 40;
    config.showPing = document.getElementById('show-ping').checked;
    config.showHealth = document.getElementById('show-health').checked;
    
    config.headerFrames = [];
    document.querySelectorAll('#header-frames-container input').forEach(input => {
        if (input.value.trim()) {
            config.headerFrames.push(input.value);
        }
    });
    
    config.footerFrames = [];
    document.querySelectorAll('#footer-frames-container input').forEach(input => {
        if (input.value.trim()) {
            config.footerFrames.push(input.value);
        }
    });
}

function updateHeaderFrames() {
    const container = document.getElementById('header-frames-container');
    container.innerHTML = '';
    (config.headerFrames || []).forEach((frame, index) => {
        container.appendChild(createFrameItem(frame, index, 'header'));
    });
}

function updateFooterFrames() {
    const container = document.getElementById('footer-frames-container');
    container.innerHTML = '';
    (config.footerFrames || []).forEach((frame, index) => {
        container.appendChild(createFrameItem(frame, index, 'footer'));
    });
}

function createFrameItem(value, index, type) {
    const div = document.createElement('div');
    div.className = 'frame-item';
    div.innerHTML = `
        <input type="text" value="${escapeHtml(value)}" placeholder="Frame ${index + 1}">
        <button class="btn-remove" onclick="removeFrame(this, '${type}')">
            <i class="fas fa-trash"></i>
        </button>
    `;
    return div;
}

function addHeaderFrame() {
    if (!config.headerFrames) config.headerFrames = [];
    config.headerFrames.push('');
    updateHeaderFrames();
}

function addFooterFrame() {
    if (!config.footerFrames) config.footerFrames = [];
    config.footerFrames.push('');
    updateFooterFrames();
}

function removeFrame(button, type) {
    const container = button.closest('.frames-container');
    const items = container.querySelectorAll('.frame-item');
    const index = Array.from(items).indexOf(button.closest('.frame-item'));
    
    if (type === 'header') {
        config.headerFrames.splice(index, 1);
        updateHeaderFrames();
    } else {
        config.footerFrames.splice(index, 1);
        updateFooterFrames();
    }
}

async function loadStats() {
    try {
        const response = await fetch('/api/stats');
        if (response.status === 401) return;
        const stats = await response.json();
        
        document.getElementById('online-players').textContent = stats.onlinePlayers + '/' + stats.maxPlayers;
        document.getElementById('tps').textContent = stats.tps.toFixed(1);
        document.getElementById('memory').textContent = stats.usedMemory + ' / ' + stats.maxMemory + ' MB';
        document.getElementById('worlds').textContent = stats.worlds;
        document.getElementById('server-version').textContent = stats.version;
        document.getElementById('server-motd').textContent = stats.motd;
    } catch (error) {
        console.error('Stats yüklenemedi:', error);
    }
}

async function loadPlayers() {
    try {
        const response = await fetch('/api/players');
        if (response.status === 401) return;
        const players = await response.json();
        
        const tbody = document.querySelector('#players-table tbody');
        tbody.innerHTML = '';
        
        if (players.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: rgba(255,255,255,0.5);">Çevrimiçi oyuncu yok</td></tr>';
            return;
        }
        
        players.forEach(player => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td>${escapeHtml(player.name)} ${player.op ? '<i class="fas fa-crown" style="color: gold; margin-left: 5px;" title="OP"></i>' : ''}</td>
                <td>${player.ping} ms</td>
                <td>${Math.round(player.health)}/${Math.round(player.maxHealth)}</td>
                <td>${player.level}</td>
                <td>${escapeHtml(player.world)}</td>
                <td>${player.gamemode}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (error) {
        console.error('Oyuncular yüklenemedi:', error);
    }
}

async function loadPresets() {
    try {
        const response = await fetch('/api/presets');
        if (response.status === 401) return;
        const presets = await response.json();
        
        const select = document.getElementById('preset-select');
        select.innerHTML = '<option value="">-- Şablon Seçin --</option>';
        
        presets.forEach(preset => {
            const option = document.createElement('option');
            option.value = preset.name;
            option.textContent = preset.name;
            select.appendChild(option);
        });
    } catch (error) {
        console.error('Şablonlar yüklenemedi:', error);
    }
}

async function loadPreset(name) {
    if (!name) return;
    try {
        const response = await fetch('/api/presets/' + encodeURIComponent(name));
        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }
        if (!response.ok) throw new Error('Şablon yüklenemedi');
        
        const preset = await response.json();
        config = preset.config;
        updateUI();
        showToast('Şablon yüklendi: ' + name, 'success');
    } catch (error) {
        console.error('Şablon yüklenemedi:', error);
        showToast('Şablon yüklenemedi', 'error');
    }
}

async function savePreset() {
    const name = document.getElementById('preset-name').value.trim();
    if (!name) {
        return showToast('Lütfen şablon adı girin', 'error');
    }
    try {
        const response = await fetch('/api/presets', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, config })
        });
        
        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }
        
        if (response.ok) {
            showToast('Şablon kaydedildi: ' + name, 'success');
            loadPresets();
        } else {
            showToast('Kaydetme başarısız', 'error');
        }
    } catch (error) {
        console.error('Şablon kaydedilemedi:', error);
        showToast('Kaydetme başarısız', 'error');
    }
}

async function deletePreset() {
    const select = document.getElementById('preset-select');
    const name = select.value;
    if (!name) return;
    
    if (!confirm('Bu şablonu silmek istediğinize emin misiniz?')) return;
    
    try {
        const response = await fetch('/api/presets/' + encodeURIComponent(name), {
            method: 'DELETE'
        });
        
        if (response.status === 401) {
            window.location.href = '/login';
            return;
        }
        
        if (response.ok) {
            showToast('Şablon silindi: ' + name, 'success');
            loadPresets();
            document.getElementById('preset-name').value = '';
        } else {
            showToast('Silme başarısız', 'error');
        }
    } catch (error) {
        console.error('Şablon silinemedi:', error);
        showToast('Silme başarısız', 'error');
    }
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('toast');
    toast.textContent = message;
    toast.className = 'toast ' + type;
    toast.classList.add('show');
    setTimeout(() => { toast.classList.remove('show'); }, 3000);
}

function escapeHtml(text) {
    if (!text) return '';
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}
""";
    }
}

