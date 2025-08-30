package org.modernbeta.discordChats;

import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.*;
import org.bukkit.command.*;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import org.modernbeta.discordChats.commands.ChatCommand;
import org.modernbeta.discordChats.commands.ReloadCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DiscordChats extends JavaPlugin {

    private static DiscordChats instance;

    private final Map<String, String> chatChannels = new ConcurrentHashMap<>();
    private final List<ChatCommand> registeredCommands = new ArrayList<>();

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();
        loadChats();

        getCommand("discordchatsreload").setExecutor(new ReloadCommand(instance));
        Bukkit.getPluginManager().registerEvents(new org.bukkit.event.Listener() {}, this);
        DiscordSRV.api.subscribe(this);

        getLogger().info("DiscordChats enabled with " + chatChannels.size() + " chat(s).");
    }

    public static DiscordChats getInstance() {
        return instance;
    }

    public void reloadPlugin() { // TODO: this does not seem to work. previous discord ID is still in action with the pre-reload command
        DiscordSRV.api.unsubscribe(this);
        unregisterChatCommands();

        reloadConfig();

        loadChats();
        DiscordSRV.api.subscribe(this);
    }

    public void loadChats() {
        ConfigurationSection section = getConfig().getConfigurationSection("DiscordChats");
        if (section == null) return;

        try {
            SimpleCommandMap commandMap = (SimpleCommandMap) Bukkit.getServer().getClass()
                    .getMethod("getCommandMap").invoke(Bukkit.getServer());

            for (String chatName : section.getKeys(false)) {
                String discordChatID = section.getString(chatName + ".discordChatID");
                if (discordChatID == null) continue;

                chatChannels.put(chatName.toLowerCase(), discordChatID);

                ChatCommand command = new ChatCommand(chatName, discordChatID);
                commandMap.register(getDescription().getName(), command);
                registeredCommands.add(command);
            }
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().warning("Failed to dynamically register chat commands!");
        }
    }

    public void unregisterChatCommands() {
        try {
            SimpleCommandMap commandMap = (SimpleCommandMap) Bukkit.getServer().getClass()
                    .getMethod("getCommandMap").invoke(Bukkit.getServer());

            for (ChatCommand command : registeredCommands) {
                commandMap.getCommand(command.getName()).unregister(commandMap);
            }
            registeredCommands.clear();
            chatChannels.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Subscribe
    public void onDiscordMessage(DiscordGuildMessageReceivedEvent event) {
        String discordMessageChatID = event.getChannel().getId();
        Message messageObj = event.getMessage();

        // Ignore blank messages with no attachments or forwards
        if (messageObj.getContentDisplay().trim().isEmpty() &&
                messageObj.getAttachments().isEmpty() &&
                messageObj.getMessageReference() == null) return;

        // Main message author
        User author = messageObj.getAuthor();
        Guild guild = event.getGuild();
        String authorNickname = getServerNickname(guild, author);

        // Detect forwarded/referenced message
        MessageReference ref = messageObj.getMessageReference();
        String referencedNickname = null;
        if (ref != null && ref.getMessage() != null) {
            User referencedAuthor = ref.getMessage().getAuthor();

            // Get referenced user if they are human and not the same user
            if (!referencedAuthor.isBot()) {
                referencedNickname = getServerNickname(guild, referencedAuthor);
                if (referencedNickname.equals(authorNickname)) {
                    referencedNickname = null;
                }
            }
        }

        // Detect attachment
        boolean hasAttachment = !messageObj.getAttachments().isEmpty();

        // Iterate through all configured chats
        for (Map.Entry<String, String> entry : chatChannels.entrySet()) {
            String chatName = entry.getKey();
            String discordChannelId = entry.getValue();

            if (!discordChannelId.equals(discordMessageChatID)) continue;

            // Build Minecraft message
            StringBuilder mcMessage = new StringBuilder();
            mcMessage.append("§d§l").append(chatName.toUpperCase())
                    .append(" §r§9").append(authorNickname);
            if (referencedNickname != null) {
                mcMessage.append("§7➥§7§o").append(referencedNickname);
            }
            mcMessage.append(" §8§l>§r ");
            if (hasAttachment) {
                mcMessage.append("§7§o[Attachment] §r");
            }
            mcMessage.append("§f").append(messageObj.getContentDisplay());

            String finalMessage = mcMessage.toString();

            // Send to all players with permission
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (onlinePlayer.hasPermission("discordchats." + chatName)) {
                    sendMessage(onlinePlayer, finalMessage);
                }
            }
        }
    }

    private String getServerNickname(Guild guild, User user) {
        Member member = guild.getMember(user);
        return member != null && member.getNickname() != null ? member.getEffectiveName() : user.getEffectiveName();
    }

    public void sendMessage(Player player, String message) {
        try {
            // Folia-safe region scheduler
            Bukkit.getRegionScheduler().run(this, player.getLocation(), task -> player.sendMessage(message));
        } catch (NoSuchMethodError | UnsupportedOperationException e) {
            // Fallback for Bukkit/Paper
            Bukkit.getScheduler().runTask(this, () -> player.sendMessage(message));
        }
    }
}
