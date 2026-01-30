package org.example.enotab.tab;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.example.enotab.EnoTab;
import org.example.enotab.config.TabConfig;
import org.example.enotab.utils.PlaceholderUtils;

public class TabManager {

    private final EnoTab plugin;
    private BukkitTask updateTask;
    private BukkitTask animationTask;
    private int headerFrame = 0;
    private int footerFrame = 0;

    public TabManager(EnoTab plugin) {
        this.plugin = plugin;
    }

    public void startTask() {
        TabConfig config = plugin.getTabConfig();

        // Ana güncelleme görevi
        updateTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAllPlayers, 20L, config.getUpdateInterval());

        // Animasyon görevi
        if (config.isAnimatedHeader() || config.isAnimatedFooter()) {
            animationTask = Bukkit.getScheduler().runTaskTimer(plugin, this::updateAnimation, 20L, config.getAnimationInterval());
        }
    }

    public void stopTask() {
        if (updateTask != null) {
            updateTask.cancel();
            updateTask = null;
        }
        if (animationTask != null) {
            animationTask.cancel();
            animationTask = null;
        }
    }

    public void restartTask() {
        stopTask();
        startTask();
    }

    private void updateAnimation() {
        TabConfig config = plugin.getTabConfig();

        if (config.isAnimatedHeader() && !config.getHeaderFrames().isEmpty()) {
            headerFrame = (headerFrame + 1) % config.getHeaderFrames().size();
        }

        if (config.isAnimatedFooter() && !config.getFooterFrames().isEmpty()) {
            footerFrame = (footerFrame + 1) % config.getFooterFrames().size();
        }

        updateAllPlayers();
    }

    public void updateAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayer(player);
        }
    }

    public void updatePlayer(Player player) {
        TabConfig config = plugin.getTabConfig();

        String header = getHeader(player);
        String footer = getFooter(player);

        player.setPlayerListHeaderFooter(
            colorize(PlaceholderUtils.replacePlaceholders(header, player)),
            colorize(PlaceholderUtils.replacePlaceholders(footer, player))
        );

        // Oyuncu listesi formatı
        updatePlayerListName(player);
    }

    private String getHeader(Player player) {
        TabConfig config = plugin.getTabConfig();

        if (config.isAnimatedHeader() && !config.getHeaderFrames().isEmpty()) {
            return config.getHeaderFrames().get(headerFrame);
        }
        return config.getHeader();
    }

    private String getFooter(Player player) {
        TabConfig config = plugin.getTabConfig();

        if (config.isAnimatedFooter() && !config.getFooterFrames().isEmpty()) {
            return config.getFooterFrames().get(footerFrame);
        }
        return config.getFooter();
    }

    private void updatePlayerListName(Player player) {
        TabConfig config = plugin.getTabConfig();
        String format = config.getPlayerFormat();

        String displayName = PlaceholderUtils.replacePlaceholders(format, player);
        player.setPlayerListName(colorize(displayName));
    }

    private String colorize(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}

