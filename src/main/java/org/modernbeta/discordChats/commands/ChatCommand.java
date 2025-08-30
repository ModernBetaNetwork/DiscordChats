package org.modernbeta.discordChats.commands;

import github.scarsz.discordsrv.DiscordSRV;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.modernbeta.discordChats.DiscordChats;

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

        // Send to all Minecraft players who have permission to view this chat
        String formattedMessage = "§d§l" + chatName.toUpperCase() + " §r§d" + player.getName() + " §8§l>§r " + message;
        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.hasPermission("discordchats." + chatName)) {
                DiscordChats.getInstance().runSync(player, () -> onlinePlayer.sendMessage(formattedMessage));
            }
        }

        // Send to Discord
        if (DiscordSRV.getPlugin().getJda() != null) {
            var channel = DiscordSRV.getPlugin().getJda().getTextChannelById(discordID);
            if (channel == null) {
                player.sendMessage("§cDiscord channel not found.");
                return true;
            }

            channel.sendMessage("**" + player.getName() + "** > " + message).queue();
        }

        return true;
    }
}
