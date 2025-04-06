
![file_00000000915851f7ad88282e9bb67d0d_conversation_id67ef8974-6570-8011-84ca-8d4eb9dfe491message_id0374a87c-f9a9-4611-82d3-15a96018164e](https://github.com/user-attachments/assets/f31ee386-c966-4cd1-9df9-a0f89901a213)


# 🔒 Whitelist Mod 

*A sophisticated mod control system for Fabric Minecraft servers*

---

## ✨ Features

✔ **Automatic mod verification** for connecting clients  
✔ **Three configurable lists** (required/allowed/banned)  
✔ **Clear rejection messages** with detailed explanations  
✔ **Simple JSON configuration**  
✔ **Fabric 1.21.1** compatible  

---

## 📦 Installation

1. Place the mod JAR in your server's `mods` folder
2. Restart the server to generate the config file
3. Configure your lists in `config/modchecker/allowed_mods.json`

---

## ⚙️ Configuration

### 📂 Config File Structure
```json
{
  "USE_WHITELIST_ONLY": true,
  "LANGUAGE": "en_us",
  "CLIENT_MOD_NECESSARY": [
    "whitelistmod"
  ],
  "CLIENT_MOD_WHITELIST": [
    "fabric-api",
    "litematica"
  ],
  "CLIENT_MOD_BLACKLIST": [
    "wurst",
    "aristois"
  ]
}
🔍 Finding Mod IDs
Players can find their mod IDs in the client logs (logs/latest.log) by searching for:


[ModChecker] Client mod list: ["mod1", "mod2", ...]
Note: Server logs no longer display this information to reduce console spam.

✂️ Easy Copy-Paste
Open latest.log with a text editor
Locate the line containing [ModChecker] Client mod list
Copy the array between square brackets
Paste into the appropriate section of your config file
❌ Rejection Messages
Error Type	Example Message
Missing Mods	Required mods missing: [whitelistmod]
Banned Mods	Unauthorized mods detected: [wurst]. Please open a ticket for approval.
📜 Best Practices
✅ Whitelist: Add quality-of-life and optimization mods
❌ Blacklist: Block known cheat clients
⚠️ Required Mods: Reserve for essential mods only
❓ Support
For assistance, contact us on Discord or GitHub

✨ Thank you for using Whitelist Mod! ✨
