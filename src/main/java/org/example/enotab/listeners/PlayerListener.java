package org.example.enotab.listeners;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.example.enotab.EnoTab;

public class PlayerListener implements Listener {

    private final EnoTab plugin;

    public PlayerListener(EnoTab plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Oyuncu giriş yaptığında tab'ı güncelle
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getTabManager().updatePlayer(event.getPlayer());
            plugin.getTabManager().updateAllPlayers();
        }, 10L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Oyuncu çıktığında diğer oyuncuların tab'ını güncelle
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getTabManager().updateAllPlayers();
        }, 5L);
    }
}

