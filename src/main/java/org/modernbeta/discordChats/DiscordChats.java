package org.modernbeta.discordChats;

import github.scarsz.discordsrv.api.events.DiscordGuildMessageReceivedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Message;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageReference;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
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
        String discordMessageAuthor = event.getAuthor().getName();
        String discordMessage = event.getMessage().getContentDisplay();

        // Detect if the message has attachments (e.g., images)
        boolean hasAttachment = !event.getMessage().getAttachments().isEmpty();

        // Detect if the message is a forwarded message (Discord has message references for replies/forwards)
        boolean isForwarded = event.getMessage().getReferencedMessage() != null;

        // If there is no message content and no attachments, skip it entirely
        if (discordMessage.trim().isEmpty() && !hasAttachment && !isForwarded) {
            return;
        }

        for (Map.Entry<String, String> entry : chatChannels.entrySet()) {
            String chatName = entry.getKey();
            String discordChatID = entry.getValue();

            if (!discordChatID.equals(discordMessageChatID)) {
                continue;
            }

            String message = "§d§l" + chatName.toUpperCase() + " §r§9" + discordMessageAuthor;
            MessageReference referencedMessage = event.getMessage().getMessageReference();
            if (referencedMessage != null && referencedMessage.getMessage() != null) {
                String referenceMessageAuthor = referencedMessage.getMessage().getAuthor().getName();
                if (!discordMessageAuthor.equals(referenceMessageAuthor)) { // don't self reply
                    message += "§7➥§7§o" + referencedMessage.getMessage().getAuthor().getName();
                }
            }
            message += " §8§l>§r " + (hasAttachment ? "§7§o[Attachment] §r" : "") + "§f" + discordMessage;
            String finalMessage = message;

            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("discordchats." + chatName)) {
                    runSync(player, () -> player.sendMessage(finalMessage));
                }
            }
        }
    }

    public void runSync(Player player, Runnable task) {
        try {
            Bukkit.getRegionScheduler().run(instance, player.getLocation(), schedulerTask -> task.run());
        } catch (NoSuchMethodError e) {
            Bukkit.getScheduler().runTask(this, task);
        }
    }
}
