package org.modernbeta.discordChats.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.modernbeta.discordChats.DiscordChats;

public class ReloadCommand implements CommandExecutor {

    private final DiscordChats DISCORD_CHATS;

    public ReloadCommand(DiscordChats discordChats) {
        this.DISCORD_CHATS = discordChats;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("discordchats.reload")) {
            sender.sendMessage("§cYou don't have permission to reload DiscordChats.");
            return true;
        }

        DISCORD_CHATS.reloadPlugin();
        sender.sendMessage("§aDiscordChats reloaded successfully!");
        return true;
    }
}
