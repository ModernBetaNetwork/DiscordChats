# DiscordChats
Communicate between many private Discord channels and Minecraft with easy configuration!
- **Supported Versions: 1.20 - 1.21.x**
- **Supported Platforms: Paper & Folia**

## Example usage
config.yml:
```yml
BotToken: "YOUR_DISCORD_BOT_TOKEN"

DiscordChats:
  yourchatname:
    discordChatID: "000000000000000000"
    aliases: ["ycn", "onlycoolpeople"]
  ... as many more as you want!
```
With this config the following will be created for you:
- Command: `/yourchatname <msg>` - Sends a minecraft message to the configured discord chat ID
  - Also all your _optional_ aliases will work: `/ycn <msg>` `/onlycoolpeople <msg>`
- Permission: `DiscordChats.yourchatname` - Users with this permission can use the above command and will recieve Discord messages from the configured chat ID in Minecraft!
