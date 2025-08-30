package org.modernbeta.discordChats.commands;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand extends Command {
    private final String chatName;
    private final String discordID;

    public ChatCommand(String chatName, String discordID) {
        super(chatName.toLowerCase() + "chat");
        this.chatName = chatName.toLowerCase();
        this.discordID = discordID;
        setDescription("Send a message to the " + chatName + " Discord chat.");
        setUsage("/" + getName() + " <message>");
        setPermission("discordchats." + chatName);
    }

    @Override
    public boolean execute(CommandSender sender, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage("Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission(getPermission())) {
            player.sendMessage("§cYou do not have permission to use this chat.");
            return true;
        }

        if (args.length == 0) {
            player.sendMessage("§cUsage: /" + getName() + " <message>");
            return true;
        }

        String message = String.join(" ", args);;

        if (discordID == null) {
            player.sendMessage("§cThis chat is not configured.");
            return true;
        }

        // Send to Discord
        if (DiscordSRV.getPlugin().getJda() != null) {
            var channel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordID);
            if (channel != null) {
                channel.sendMessage("**" + player.getName() + "**: " + message).queue();
                player.sendMessage("§7[You → " + chatName + "] §f" + message);
            } else {
                player.sendMessage("§cDiscord channel not found.");
            }
        }

        return true;
    }
}
