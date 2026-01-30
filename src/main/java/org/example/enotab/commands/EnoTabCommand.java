package org.example.enotab.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.example.enotab.EnoTab;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class EnoTabCommand implements CommandExecutor, TabCompleter {

    private final EnoTab plugin;

    public EnoTabCommand(EnoTab plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("enotab.admin")) {
            sender.sendMessage(color("&cBu komutu kullanma yetkiniz yok!"));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.getTabConfig().reload();
                plugin.getTabManager().restartTask();
                sender.sendMessage(color("&aEnoTab yapılandırması yeniden yüklendi!"));
                break;

            case "update":
                plugin.getTabManager().updateAllPlayers();
                sender.sendMessage(color("&aTüm oyuncuların tab listesi güncellendi!"));
                break;

            case "web":
                int port = plugin.getConfig().getInt("web.port", 6969);
                sender.sendMessage(color("&eWeb Panel: &fhttp://localhost:" + port));
                break;

            case "setheader":
                if (args.length < 2) {
                    sender.sendMessage(color("&cKullanım: /enotab setheader <metin>"));
                    return true;
                }
                String header = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getTabConfig().setHeader(header);
                plugin.getTabConfig().save();
                plugin.getTabManager().updateAllPlayers();
                sender.sendMessage(color("&aHeader güncellendi!"));
                break;

            case "setfooter":
                if (args.length < 2) {
                    sender.sendMessage(color("&cKullanım: /enotab setfooter <metin>"));
                    return true;
                }
                String footer = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
                plugin.getTabConfig().setFooter(footer);
                plugin.getTabConfig().save();
                plugin.getTabManager().updateAllPlayers();
                sender.sendMessage(color("&aFooter güncellendi!"));
                break;

            default:
                sendHelp(sender);
                break;
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&6&l━━━━━━ EnoTab Yardım ━━━━━━"));
        sender.sendMessage(color("&e/enotab reload &7- Yapılandırmayı yeniden yükle"));
        sender.sendMessage(color("&e/enotab update &7- Tab listesini güncelle"));
        sender.sendMessage(color("&e/enotab web &7- Web panel adresini göster"));
        sender.sendMessage(color("&e/enotab setheader <metin> &7- Header'ı ayarla"));
        sender.sendMessage(color("&e/enotab setfooter <metin> &7- Footer'ı ayarla"));
        sender.sendMessage(color("&6&l━━━━━━━━━━━━━━━━━━━━━━━"));
    }

    private String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("reload", "update", "web", "setheader", "setfooter");
            for (String sub : subcommands) {
                if (sub.toLowerCase().startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
        }

        return completions;
    }
}

