package us.to.midensthings.dynamicTools;

import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import us.to.midensthings.dynamicTools.addons.AddonLoader;
import us.to.midensthings.dynamicTools.gui.DynamicToolsGui;
import us.to.midensthings.dynamicTools.commands.CoreCommands;
import us.to.midensthings.dynamicTools.commands.DebugCommands;
import us.to.midensthings.dynamicTools.compat.CraftEngineCompat;
import us.to.midensthings.dynamicTools.compat.CraftEngineStartupReload;
import us.to.midensthings.dynamicTools.compat.ItemsAdderBlockRecipes;
import us.to.midensthings.dynamicTools.compat.ItemsAdderCompat;
import us.to.midensthings.dynamicTools.events.GuiListener;
import us.to.midensthings.dynamicTools.events.VanillaRecipeListener;
import us.to.midensthings.dynamicTools.events.WorkstationListener;
import us.to.midensthings.dynamicTools.modifiers.Modifier;
import us.to.midensthings.dynamicTools.modifiers.ModifierManager;
import us.to.midensthings.dynamicTools.modifiers.ModifierRegistry;
import us.to.midensthings.dynamicTools.modifiers.builtin.*;
import us.to.midensthings.dynamicTools.parts.PartType;
import us.to.midensthings.dynamicTools.resourcepack.ResourcePackGenerator;
import us.to.midensthings.dynamicTools.tools.CustomAttribute;
import us.to.midensthings.dynamicTools.tools.CustomItemData;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;

public final class DynamicTools extends JavaPlugin {
    public enum CustomItemSource {
        ITEMSADDER,
        CRAFTENGINE
    }
    
    // volatile because /dt reload replaces these while other region threads may be reading them on Folia
    private volatile ToolMaterialRegistry toolMaterialRegistry;
    // The attributes from custom-attributes.yml, by ID
    private volatile Map<String, CustomAttribute> customAttributes = Map.of();
    File customAttributesFile = new File(this.getDataFolder()+"/custom-attributes.yml");
    private PartManager partManager;
    private ModifierRegistry modifierRegistry;
    private ModifierManager modifierManager;
    private AddonLoader addonLoader;
    
    private PluginManager pm;
    // Set when the custom item plugin started before DynamicTools added its files, see onSourceFilesChanged
    private final AtomicBoolean sourceNeedsReload = new AtomicBoolean(false);
    private CustomItemSource customItemSource;
    
    private List<String> enabledCompats;

    File materialsFile = new File(this.getDataFolder()+"/materials.yml");
    volatile YamlConfiguration materialsConf;

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
        if (customItemSource == CustomItemSource.ITEMSADDER) {
            // Also reloads ItemsAdder after its first load if it started before DynamicTools added its files
            pm.registerEvents(new ItemsAdderBlockRecipes(), this);
        } else {
            pm.registerEvents(new CraftEngineStartupReload(), this);
        }

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
        // Write config.yml before reading it. Reading it first loads an empty config on a fresh install, and options read
        // with their own fallback (e.g. custom-block) would then use that fallback instead of the defaults in config.yml.
        saveDefaultConfig();
        reloadConfig();

        if (!materialsFile.exists()) {
            getLogger().info("Generating materials.yml");
            this.saveResource("materials.yml",false);
        }

        materialsConf = YamlConfiguration.loadConfiguration(materialsFile);

        // Set custom material source
        this.customItemSource = chooseCustomItemSource();
        if (customItemSource == null) {
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
        boolean added = switch (customItemSource) {
            case ITEMSADDER -> ItemsAdderCompat.installConfigs();
            case CRAFTENGINE -> CraftEngineCompat.installConfigs();
        };
        if (added) {
            onSourceFilesChanged("item and block configs");
        }
    }

    /***
     * @return the plugin name of the custom item source
     */
    public String getCustomItemSourceName() {
        return switch (customItemSource) {
            case ITEMSADDER -> "ItemsAdder";
            case CRAFTENGINE -> "CraftEngine";
        };
    }

    /***
     * Called when DynamicTools adds or changes files in the custom item plugin's folders while starting. If that plugin
     * already started before DynamicTools (e.g. it loads at STARTUP), it has already read its files and won't see these,
     * so it gets reloaded once the server has finished starting. If it hasn't started yet, it loads them itself.
     */
    private void onSourceFilesChanged(String what) {
        if (pm.isPluginEnabled(getCustomItemSourceName())) {
            sourceNeedsReload.set(true);
            getLogger().info("Added DynamicTools " + what + " to " + getCustomItemSourceName()
                    + " after it already started, so it will reload them once the server has finished starting");
        } else {
            debug("Added DynamicTools " + what + " to " + getCustomItemSourceName() + " before it started");
        }
    }

    /***
     * @return true if the custom item plugin has to reload to see files DynamicTools added while starting.
     * Only returns true once, so the reload only runs once.
     */
    public boolean takeSourceReload() {
        return sourceNeedsReload.getAndSet(false);
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
        // The close event isn't handled once the plugin is disabled, so give back the items in open GUIs before they're lost
        for (Player player : Bukkit.getOnlinePlayers()) {
            // On Folia, players owned by another thread can't be touched from here. Their GUIs close when they disconnect,
            // which gives the items back through the close event.
            if (!Bukkit.isOwnedByCurrentRegion(player)) {
                continue;
            }
            if (player.getOpenInventory().getTopInventory().getHolder(false) instanceof DynamicToolsGui gui) {
                gui.returnInputs(player);
                player.closeInventory();
            }
        }

        if (customItemSource == CustomItemSource.ITEMSADDER) {
            ItemsAdderBlockRecipes.removeRecipes();
        }

        if (addonLoader != null) {
            addonLoader.disableAddons();
        }
    }

    public void initializeRegistries() {
        this.getLogger().info("Initializing internal plugin registries");
        this.modifierManager = new ModifierManager();
        this.modifierRegistry = new ModifierRegistry();

        // Modifiers have to be registered before the materials that use them are loaded
        modifierRegistry.registerModifier(new ReinforcedModifier());
        modifierRegistry.registerModifier(new FrigidModifier());
        modifierRegistry.registerModifier(new FireproofModifier());
        modifierRegistry.registerModifier(new BouncyModifier());
        modifierRegistry.registerModifier(new HasteModifier());
        modifierRegistry.registerModifier(new HeavyModifier());
        modifierRegistry.registerModifier(new SharpModifier());
        modifierRegistry.registerModifier(new PoisonousModifier());
        modifierRegistry.registerModifier(new SusModifier());
        modifierRegistry.registerModifier(new WitheringModifier());
        modifierRegistry.registerModifier(new BeheadingModifier());
        modifierRegistry.registerModifier(new LifestealModifier());

        // Addons register their modifiers in onLoad, which also has to happen before the materials are loaded
        this.addonLoader = new AddonLoader();
        addonLoader.loadAddons();

        // Custom attributes have to be loaded before the materials that set them
        this.customAttributes = CustomAttribute.loadAll(loadCustomAttributesFile());
        this.toolMaterialRegistry = loadToolMaterials();

        this.partManager = new PartManager();
    }

    /***
     * Reloads config.yml and materials.yml and replaces the tool materials. Modifiers stay registered,
     * and textures are not regenerated. If either file has invalid YAML, nothing is changed.
     * @return the number of materials loaded
     */
    public int reloadConfigs() throws IOException, InvalidConfigurationException {
        saveDefaultConfig();
        if (!materialsFile.exists()) {
            saveResource("materials.yml", false);
        }

        // Load both files before changing anything, so a typo doesn't leave the plugin with no config or materials
        new YamlConfiguration().load(new File(getDataFolder(), "config.yml"));
        YamlConfiguration newMaterialsConf = new YamlConfiguration();
        newMaterialsConf.load(materialsFile);
        if (!customAttributesFile.exists()) {
            saveResource("custom-attributes.yml", false);
        }
        YamlConfiguration newCustomAttributesConf = new YamlConfiguration();
        newCustomAttributesConf.load(customAttributesFile);

        reloadConfig();
        materialsConf = newMaterialsConf;
        customAttributes = CustomAttribute.loadAll(newCustomAttributesConf);
        toolMaterialRegistry = loadToolMaterials();
        if (modifierRegistry.getModifier("beheading") instanceof BeheadingModifier beheading) {
            beheading.loadHeads();
        }
        return toolMaterialRegistry.getMaterials().size();
    }

    /***
     * @return the custom item source config.yml currently asks for, or null if none of the supported plugins are installed
     */
    public CustomItemSource chooseCustomItemSource() {
        if (enabledCompats.contains(getConfig().getString("preferred-custom-item-plugin"))) {
            return CustomItemSource.valueOf(getConfig().getString("preferred-custom-item-plugin").toUpperCase(Locale.ROOT));
        } else if (!enabledCompats.isEmpty()) {
            return CustomItemSource.valueOf(enabledCompats.getFirst().toUpperCase(Locale.ROOT));
        }
        return null;
    }

    private ToolMaterialRegistry loadToolMaterials() {
        ToolMaterialRegistry registry = new ToolMaterialRegistry();

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
                    materialsConf.getDouble(mat+".stats.durability",100.0),
                    materialsConf.getDouble(mat+".multipliers.durability",1.0),
                    materialsConf.getDouble(mat+".stats.attackDamage",1.0),
                    materialsConf.getDouble(mat+".multipliers.attackDamage",1.0),
                    materialsConf.getDouble(mat+".stats.attackSpeed",0.8),
                    materialsConf.getDouble(mat+".multipliers.attackSpeed",1.0),
                    materialsConf.getDouble(mat+".stats.digSpeed",2),
                    materialsConf.getDouble(mat+".multipliers.digSpeed",1.0),
                    materialsConf.getDouble(mat+".stats.armor"),
                    materialsConf.getDouble(mat+".multipliers.armor",1.0),
                    materialsConf.getDouble(mat+".stats.armorToughness"),
                    materialsConf.getDouble(mat+".multipliers.armorToughness",1.0),
                    // Vanilla armor durability multipliers: leather 5, gold 7, copper 11, iron/chainmail 15, diamond 33, netherite 37
                    materialsConf.getDouble(mat+".stats.armorDurability",15),
                    materialsConf.getInt(mat+".stats.miningLevel"),
                    materialsConf.getString(mat+".display-name"),
                    validParts,
                    materialsConf.getStringList(mat+".materials"),
                    modifiers,
                    loadMaterialColors(mat),
                    CustomItemData.load(materialsConf.getConfigurationSection(mat+".custom-data"), mat),
                    materialsConf.getDouble(mat+".material-cost-ratio", 1.0),
                    CustomAttribute.loadMaterialValues(materialsConf.getConfigurationSection(mat+".custom-attributes"), customAttributes, mat)
            );

            registry.registerToolMaterial(material);
            debug("Loaded material " + material.getId() + " with " + validParts.size() + " valid parts and " + modifiers.size() + " modifiers"
                    + (material.getColors() == null ? ", no texture colors" : ""));
        });

        return registry;
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
        Path assetsFolder = getResourcePackAssetsFolder();
        if (assetsFolder == null) {
            return;
        }

        getLogger().info("Generating resource pack files");
        try {
            int changed = new ResourcePackGenerator().generate(assetsFolder);
            debug(changed + " resource pack files changed in " + assetsFolder);
            if (changed > 0) {
                getLogger().info("Updated " + changed + " resource pack files in " + assetsFolder);
                onSourceFilesChanged("textures and models");
            }
        } catch (IOException e) {
            getLogger().log(Level.SEVERE, "Could not generate the resource pack files", e);
        }
    }

    /***
     * @return the assets/dynamictools folder in the custom item source plugin's resource pack, or null if the plugin isn't installed
     */
    public Path getResourcePackAssetsFolder() {
        return switch (customItemSource) {
            case ITEMSADDER -> ItemsAdderCompat.getAssetsFolder();
            case CRAFTENGINE -> CraftEngineCompat.getAssetsFolder();
        };
    }

    /***
     * @return the console command that makes the custom item source plugin rebuild its resource pack
     */
    public String getResourcePackRebuildCommand() {
        return switch (customItemSource) {
            case ITEMSADDER -> "iazip";
            case CRAFTENGINE -> "ce reload all";
        };
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

    /***
     * @return the attributes from custom-attributes.yml, by ID
     */
    public Map<String, CustomAttribute> getCustomAttributes() {
        return customAttributes;
    }

    /***
     * Reads custom-attributes.yml, creating it if it doesn't exist.
     */
    private YamlConfiguration loadCustomAttributesFile() {
        if (!customAttributesFile.exists()) {
            getLogger().info("Generating custom-attributes.yml");
            saveResource("custom-attributes.yml", false);
        }
        return YamlConfiguration.loadConfiguration(customAttributesFile);
    }

    public ModifierManager getModifierManager() {
        return modifierManager;
    }

    public AddonLoader getAddonLoader() {
        return addonLoader;
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
