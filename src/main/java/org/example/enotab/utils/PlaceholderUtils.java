package org.example.enotab.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PlaceholderUtils {

    public static String replacePlaceholders(String text, Player player) {
        if (text == null) return "";

        // Sunucu placeholder'ları
        text = text.replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()));
        text = text.replace("%max%", String.valueOf(Bukkit.getMaxPlayers()));
        text = text.replace("%tps%", getTPS());
        text = text.replace("%server%", Bukkit.getServer().getName());
        text = text.replace("%motd%", Bukkit.getServer().getMotd());

        // Oyuncu placeholder'ları
        if (player != null) {
            text = text.replace("%player%", player.getName());
            text = text.replace("%displayname%", player.getDisplayName());
            text = text.replace("%ping%", String.valueOf(player.getPing()));
            text = text.replace("%health%", String.valueOf((int) player.getHealth()));
            text = text.replace("%maxhealth%", String.valueOf((int) player.getMaxHealth()));
            text = text.replace("%food%", String.valueOf(player.getFoodLevel()));
            text = text.replace("%level%", String.valueOf(player.getLevel()));
            text = text.replace("%exp%", String.valueOf((int) (player.getExp() * 100)));
            text = text.replace("%world%", player.getWorld().getName());
            text = text.replace("%x%", String.valueOf(player.getLocation().getBlockX()));
            text = text.replace("%y%", String.valueOf(player.getLocation().getBlockY()));
            text = text.replace("%z%", String.valueOf(player.getLocation().getBlockZ()));
            text = text.replace("%gamemode%", player.getGameMode().name());
        }

        // Zaman placeholder'ları
        text = text.replace("%time%", java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        text = text.replace("%date%", java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));

        return text;
    }

    private static String getTPS() {
        try {
            Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
            double[] tps = (double[]) server.getClass().getField("recentTps").get(server);
            return String.format("%.1f", Math.min(tps[0], 20.0));
        } catch (Exception e) {
            return "20.0";
        }
    }
}
