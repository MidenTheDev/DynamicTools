# Dynamic Tools
<div align=center>

[<img src="https://github.com/iuriineves/modding-badges/blob/main/Available%20on%20Modrinth.png?raw=true" width="200"/>](https://modrinth.com/plugin/dynamic-tools)
[<img src="https://github.com/iuriineves/modding-badges/blob/main/Source%20on%20GitHub.png?raw=true" width="200"/>](https://github.com/MidenTheDev/DynamicTools)
[<img src="https://raw.githubusercontent.com/MidenTheDev/NouveauEnchantingV2/refs/heads/master/page_resources/RequiresPaper.png" width="162"/>](https://papermc.io)
[<img src="https://github.com/iuriineves/modding-badges/blob/main/Support%20me%20on%20Ko-Fi.png?raw=true" width="236"/>](https://ko-fi.com/miden)

</div>

<div align=center>
  
![GitHub Release](https://img.shields.io/github/v/release/MidenTheDev/DynamicTools?style=for-the-badge&label=Latest%20Release&color=00B324&link=https%3A%2F%2Fmodrinth.com%2Fplugin%2Fnouveau-enchanting%2Fversions)
![GitHub Release](https://img.shields.io/github/v/release/MidenTheDev/DynamicTools?include_prereleases&style=for-the-badge&label=Latest%20Pre-Release&color=E38F27)
![Static Badge](https://img.shields.io/badge/Requires%20Version-26.1%2B-GREEN?style=for-the-badge)

</div>

A new tool crafting system that lets you mix and match different materials to create entirely unique tools and items (inspired by Tinkers' Construct)


## 🛠 Dynamic Tool Stats:

By mixing and matching parts made of different materials, you can create tools that have stats optimized for your specific use case! For example, Gold mines fast
but has a low mining level and low durability. By taking a Netherite Pickaxe head, which has high durability and a relatively high mining speed and making the rest
of the tool out of gold, you can create a tool with an incredibly high mining speed and decent durability:

INSERT DEMO GIF HERE

The plugin will also automatically generate all of the textures you need to get started, and ships with a set of configs and settings that match Vanilla.
In order to load the custom items, the plugin will hook into whatever custom item plugin you have present. The upside of this means that the resource pack generation
and item handling is all consistent within your server and you can even use your custom items as the basis for tool materials. The downside is that the plugin then
_requires_ one such plugin. I recommend using the free version of CraftEngine if you do not already own a premium Custom Item plugin such as Nexo, ItemsAdder, or 
CraftEngine's own premium version.

Below is a list of plugins the plugin supports for custom items:

| Plugin |Has Support   |
| ------------ | ------------ |
|ItemsAdder  | ✅  |
|CraftEngine| ✅ |
| Nexo | ❌ (planned) |
| Oraxen | ❌ |

## 🪙 Modifiers:
Different materials can have different Modifiers attached to them that further alter a crafted tool's behavior and stats. Modifiers, for the most part, behave similarly
to enchants, even having levels which affect the strength of the modifier. For example, Netherite has the Fireproof modifier which grants tools made of Netherite the
fire-resistant property that vanilla Netherite has.

The plugin comes with a large set of modifiers that should suit most use cases of the plugin. The only modifiers that are in the default config are the Reinforced modifier
and the Fireproof modifier, however. Other modifiers will need to be added to your materials manually. Click HERE for a list of all of the modifiers and how they work.

Materials can have multiple modifiers as well, simply add them to the material's modifier list in the materials.yml file.

Stacking modifiers, however, must be done with separate materials. Using the same material multiple times will only grant the specified modifier once. If you want, say,
Reinforced 2, you will need to make the tool out of two different materials which have the Reinforced modifier.

New modifiers can be created either via the Plugin's API or the Plugin's dedicated Addon system. See the wiki for more details.

## 💻 Commands:
**/dt reload** - Reloads the plugin configuration. This command will NOT load any new addons (restart your server) or generate any textures (/dt generateTextures).

**/dt open [partAssembler/partCrafter] {player}** - Opens the specified GUI for yourself or a specified player 

**/dt generateTextures** - Re-generates the plugin's automatically generated textures.


## ➕ Addons:
DynamicTools supports the ability for users to create Addons that are automatically loaded by the plugin during startup. As of now, Addons are used solely to add more
Modifiers to the plugin, as tool materials are handled by the materials.yml file and the different kinds of tools and parts you can craft are, as of now, hard-coded (enum).

If you are looking to DOWNLOAD addons made by community members, visit the plugin's [Discord Server](https://discord.gg/qUEcgsDN7T).

If you are a developer looking to CREATE addons, check the plugin's Wiki for details on how to get started.

## ❓ FAQ:

#### Q:  I Need Help! X Isn't working!
**A: ** Check the relevant Wiki page. If the Wiki isn't helpful or you otherwise don't understand, join the [Discord Server](https://discord.gg/qUEcgsDN7T) and ask for support there.

#### Q: Why does this plugin only support Paper and not Spigot?
**A:** This plugin makes extensive use of Paper's api, using a wide variety of features that simply are not supported on Spigot. While creating a plugin that does the same thing on Spigot is theoretically possible, it would be a massive undertaking and would not function nearly as seamlessly.
