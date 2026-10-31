package us.to.midensthings.dynamicTools;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import us.to.midensthings.dynamicTools.commands.CoreCommands;
import us.to.midensthings.dynamicTools.commands.DebugCommands;
import us.to.midensthings.dynamicTools.compat.CraftEngineCompat;
import us.to.midensthings.dynamicTools.compat.ItemsAdderCompat;
import us.to.midensthings.dynamicTools.events.GuiListener;
import us.to.midensthings.dynamicTools.events.VanillaRecipeListener;
import us.to.midensthings.dynamicTools.events.WorkstationListener;
import us.to.midensthings.dynamicTools.modifiers.Modifier;
import us.to.midensthings.dynamicTools.modifiers.ModifierManager;
import us.to.midensthings.dynamicTools.modifiers.ModifierRegistry;
import us.to.midensthings.dynamicTools.modifiers.builtin.FireproofModifier;
import us.to.midensthings.dynamicTools.modifiers.builtin.FrigidModifier;
import us.to.midensthings.dynamicTools.modifiers.builtin.ReinforcedModifier;
import us.to.midensthings.dynamicTools.parts.PartType;
import us.to.midensthings.dynamicTools.resourcepack.ResourcePackGenerator;
import us.to.midensthings.dynamicTools.tools.MaterialColors;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;
import us.to.midensthings.dynamicTools.tools.ToolMaterialRegistry;
import us.to.midensthings.dynamicTools.parts.PartManager;
import net.kyori.adventure.text.format.TextColor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.logging.Level;

public final class DynamicTools extends JavaPlugin {
    public enum CustomItemSource {
        ITEMSADDER,
        CRAFTENGINE
    }
    
    private ToolMaterialRegistry toolMaterialRegistry;
    private PartManager partManager;
    private ModifierRegistry modifierRegistry;
    private ModifierManager modifierManager;
    
    private PluginManager pm;
    private CustomItemSource customItemSource;
    
    private List<String> enabledCompats;

    File materialsFile = new File(this.getDataFolder()+"/materials.yml");
    YamlConfiguration materialsConf;

    @Override
    public void onEnable() {
        // Plugin startup logic
        pm = Bukkit.getPluginManager();
        
        loadCompats();

        // disablePlugin doesn't stop onEnable from running, so stop here if a step shut the plugin down
        if (!isEnabled()) {
            return;
        }

        loadConfigs();

        if (!isEnabled()) {
            return;
        }

        installCustomItemConfigs();

        initializeRegistries();

        generateResourcePack();

        // register events
        pm.registerEvents(new GuiListener(), this);
        pm.registerEvents(new VanillaRecipeListener(), this);
        pm.registerEvents(new WorkstationListener(), this);

        registerCommands();
    }

    private void registerCommands() {
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, commands -> {
            DebugCommands debugCommands = new DebugCommands();
            commands.registrar().register(debugCommands.debugCore);
            CoreCommands coreCommands = new CoreCommands();
            commands.registrar().register(coreCommands.coreCommand);
        });
    }

    private void loadConfigs() {
        getLogger().info("Loading configurations");
        getConfig().options().copyDefaults();
        saveDefaultConfig();

        if (!materialsFile.exists()) {
            getLogger().info("Generating materials.yml");
            this.saveResource("materials.yml",false);
        }

        materialsConf = YamlConfiguration.loadConfiguration(materialsFile);

        // Set custom material source
        if (enabledCompats.contains(getConfig().getString("preferred-custom-item-plugin"))) {
            this.customItemSource = CustomItemSource.valueOf(getConfig().getString("preferred-custom-item-plugin").toUpperCase(Locale.ROOT));
        } else if (!enabledCompats.isEmpty()) {
            this.customItemSource = CustomItemSource.valueOf(enabledCompats.getFirst().toUpperCase(Locale.ROOT));
        } else {
            this.getLogger().severe("No supported custom items plugin found!");
            this.getLogger().severe("DynamicTools requires a custom items plugin to function! Shutting down...");
            pm.disablePlugin(this);
        }



    }

    /***
     * Adds the DynamicTools item and block configs to the custom item source plugin only.
     */
    private void installCustomItemConfigs() {
        if (customItemSource == null) {
            return;
        }
        switch (customItemSource) {
            case ITEMSADDER -> {
                if (ItemsAdderCompat.installConfigs()) {
                    getLogger().warning("Added DynamicTools configs to ItemsAdder. Run /iazip to load them.");
                }
            }
            case CRAFTENGINE -> {
                if (CraftEngineCompat.installConfigs()) {
                    getLogger().warning("Added DynamicTools configs to CraftEngine. Run /ce reload all to load them.");
                }
            }
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    public void initializeRegistries() {
        this.getLogger().info("Initializing internal plugin registries");
        this.modifierManager = new ModifierManager();
        this.modifierRegistry = new ModifierRegistry();

        // Modifiers have to be registered before the materials that use them are loaded
        modifierRegistry.registerModifier(new ReinforcedModifier());
        modifierRegistry.registerModifier(new FrigidModifier());
        modifierRegistry.registerModifier(new FireproofModifier());

        this.toolMaterialRegistry = new ToolMaterialRegistry();

        materialsConf.getConfigurationSection("").getKeys(false).forEach(mat -> {
            List<PartType> validParts = new ArrayList<>();
            if (!materialsConf.getStringList(mat+".part-whitelist").isEmpty()) {
                List<String> validPartsStrings = materialsConf.getStringList(mat+".part-whitelist");
                for (String partName : validPartsStrings) {
                    try {
                        validParts.add(PartType.valueOf(partName));
                    } catch (IllegalArgumentException e) {
                        this.getLogger().warning("Invalid part type found in material "+mat+" called: "+partName);
                    }
                }
            } else {
                validParts = new ArrayList<>(List.of(PartType.values()));
                if (!materialsConf.getStringList(mat+".part-blacklist").isEmpty()) {
                    List<String> invalidPartsStrings = materialsConf.getStringList(mat+".part-blacklist");
                    for (String partName : invalidPartsStrings) {
                        try {
                            validParts.remove(PartType.valueOf(partName));
                        } catch (IllegalArgumentException e) {
                            this.getLogger().warning("Invalid part type found in material "+mat+" called: "+partName);
                        }
                    }
                }


            }

            Map<Modifier,Integer> modifiers = new LinkedHashMap<>();
            ConfigurationSection modifiersSection = materialsConf.getConfigurationSection(mat+".modifiers");
            if (modifiersSection != null) {
                for (String modifierId : modifiersSection.getKeys(false)) {
                    Modifier modifier = modifierRegistry.getModifier(modifierId.toLowerCase(Locale.ROOT));
                    if (modifier == null) {
                        this.getLogger().warning("Unknown modifier "+modifierId+" in material "+mat);
                        continue;
                    }
                    modifiers.put(modifier, modifiersSection.getInt(modifierId, 1));
                }
            }

            ToolMaterial material = new ToolMaterial(
                    mat.toLowerCase(),
                    materialsConf.getDouble(mat+".stats.durability"),
                    materialsConf.getDouble(mat+".multipliers.durability"),
                    materialsConf.getDouble(mat+".stats.attackDamage"),
                    materialsConf.getDouble(mat+".multipliers.attackDamage"),
                    materialsConf.getDouble(mat+".stats.attackSpeed"),
                    materialsConf.getDouble(mat+".multipliers.attackSpeed"),
                    materialsConf.getDouble(mat+".stats.digSpeed"),
                    materialsConf.getDouble(mat+".multipliers.digSpeed"),
                    materialsConf.getDouble(mat+".stats.armor"),
                    materialsConf.getDouble(mat+".multipliers.armor"),
                    materialsConf.getDouble(mat+".stats.armorToughness"),
                    materialsConf.getDouble(mat+".multipliers.armorToughness"),
                    materialsConf.getInt(mat+".stats.miningLevel"),
                    materialsConf.getString(mat+".display-name"),
                    validParts,
                    materialsConf.getStringList(mat+".materials"),
                    modifiers,
                    loadMaterialColors(mat)
            );

            toolMaterialRegistry.registerToolMaterial(material);
            debug("Loaded material " + material.getId() + " with " + validParts.size() + " valid parts and " + modifiers.size() + " modifiers"
                    + (material.getColors() == null ? ", no texture colors" : ""));
        });

        this.partManager = new PartManager();
    }
    
    /***
     * @return the material's texture colors, or null if it has none or they're invalid.
     */
    private MaterialColors loadMaterialColors(String mat) {
        ConfigurationSection colorsSection = materialsConf.getConfigurationSection(mat+".colors");
        if (colorsSection == null) {
            return null;
        }
        TextColor light = TextColor.fromHexString(colorsSection.getString("light", ""));
        TextColor medium = TextColor.fromHexString(colorsSection.getString("medium", ""));
        TextColor dark = TextColor.fromHexString(colorsSection.getString("dark", ""));
        if (light == null || medium == null || dark == null) {
            this.getLogger().warning("Material "+mat+" needs light, medium and dark colors as hex codes like \"#a1b2c3\". Its parts won't have textures.");
            return null;
        }
        return new MaterialColors(light.value(), medium.value(), dark.value());
    }

    /***
     * Generates the part and tool textures and models into the custom item source plugin's resource pack folder.
     */
    private void generateResourcePack() {
        if (!getConfig().getBoolean("resource-pack.generate", true)) {
            return;
        }
        Path assetsFolder = switch (customItemSource) {
            case ITEMSADDER -> ItemsAdderCompat.getAssetsFolder();
            case CRAFTENGINE -> CraftEngineCompat.getAssetsFolder();
        };
        if (assetsFolder == null) {
            return;
        }

        getLogger().info("Generating resource pack files");
        try {
            int changed = new ResourcePackGenerator().generate(assetsFolder);
            debug(changed + " resource pack files changed in " + assetsFolder);
            if (changed > 0) {
                getLogger().info("Updated " + changed + " resource pack files in " + assetsFolder
                        + (customItemSource == CustomItemSource.ITEMSADDER ? ". Run /iazip if ItemsAdder has already built its pack." : ". Run /ce reload all if CraftEngine has already built its pack."));
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not generate the resource pack files", e);
        }
    }

    public void loadCompats() {
        this.getLogger().info("Loading plugin compatibility modules");
        enabledCompats = new ArrayList<>();
        if (pm.getPlugin("ItemsAdder") != null) {
            enabledCompats.add("ItemsAdder");
        }
        if (pm.getPlugin("CraftEngine") != null) {
            enabledCompats.add("CraftEngine");
        }
        if (enabledCompats.isEmpty()) {
            this.getLogger().severe("No supported custom items plugin found!");
            this.getLogger().severe("DynamicTools requires a custom items plugin to function! Shutting down...");
            pm.disablePlugin(this);
        }
        
        // Load other compats after checking for a valid custom item plugin first
    }

    /***
     * Logs a message if debug-mode is on in config.yml.
     */
    public void debug(String message) {
        if (getConfig().getBoolean("debug-mode", false)) {
            getLogger().info("[Debug] " + message);
        }
    }

    public ToolMaterialRegistry getToolMaterialRegistry() {
        return toolMaterialRegistry;
    }

    public PartManager getPartManager() {
        return partManager;
    }

    public ModifierRegistry getModifierRegistry() {
        return modifierRegistry;
    }

    public ModifierManager getModifierManager() {
        return modifierManager;
    }
    
    public List<String> getEnabledCompats() {
        return enabledCompats;
    }

    public YamlConfiguration getMaterialsConf() {
        return materialsConf;
    }

    public File getMaterialsFile() {
        return materialsFile;
    }

    public CustomItemSource getCustomItemSource() {
        return customItemSource;
    }
}
