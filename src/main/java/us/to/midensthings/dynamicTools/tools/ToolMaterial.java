package us.to.midensthings.dynamicTools.tools;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.compat.CraftEngineCompat;
import us.to.midensthings.dynamicTools.compat.ItemsAdderCompat;
import us.to.midensthings.dynamicTools.modifiers.Modifier;
import us.to.midensthings.dynamicTools.parts.PartType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ToolMaterial {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    private String id;

    private double durability;
    private double durabilityMultiplier;

    private double attackDamage;
    private double attackDamageMultiplier;

    private double attackSpeed;
    private double attackSpeedMultiplier;

    private double digSpeed;
    private double digSpeedMultiplier;

    private double armor;
    private double armorMultiplier;

    private double armorToughness;
    private double armorToughnessMultiplier;

    private int miningLevel;

    private String displayName;

    private List<PartType> validParts;
    private List<String> validInputMaterials;
    private final List<Tag<Material>> inputTags = new ArrayList<>();
    private final Set<Material> inputItems = new HashSet<>();
    private final Set<String> inputCustomItems = new HashSet<>();

    // Map for other attributes this material modifies?
    private Map<String,?> attributeMap;
    // The modifiers this material gives tools and their levels
    private Map<Modifier,Integer> modifiers;



    public ToolMaterial(String id, double durability, double durabilityMultiplier, double attackDamage, double attackDamageMultiplier, double attackSpeed, double attackSpeedMultiplier, double digSpeed, double digSpeedMultiplier, double armor, double armorMultiplier, double armorToughness, double armorToughnessMultiplier, int miningLevel, String displayName, List<PartType> validParts, List<String> validInputMaterials, Map<Modifier,Integer> modifiers) {
        this.id = id;
        this.durability = durability;
        this.durabilityMultiplier = durabilityMultiplier;
        this.attackDamage = attackDamage;
        this.attackDamageMultiplier = attackDamageMultiplier;
        this.attackSpeed = attackSpeed;
        this.attackSpeedMultiplier = attackSpeedMultiplier;
        this.digSpeed = digSpeed;
        this.digSpeedMultiplier = digSpeedMultiplier;
        this.armor = armor;
        this.armorMultiplier = armorMultiplier;
        this.armorToughness = armorToughness;
        this.armorToughnessMultiplier = armorToughnessMultiplier;
        this.miningLevel = miningLevel;
        this.displayName = displayName;
        this.validParts = validParts;
        this.validInputMaterials = validInputMaterials;
        this.modifiers = modifiers;
        parseInputMaterials();
    }

    /***
     * Sorts the entries of the materials list into item tags ("#minecraft:planks"), vanilla items ("minecraft:iron_ingot")
     * and custom items from ItemsAdder or CraftEngine ("my_items:ruby").
     */
    private void parseInputMaterials() {
        for (String entry : validInputMaterials) {
            String normalized = entry.toLowerCase(Locale.ROOT);
            if (normalized.startsWith("#")) {
                NamespacedKey key = NamespacedKey.fromString(normalized.substring(1));
                Tag<Material> tag = key == null ? null : Bukkit.getTag(Tag.REGISTRY_ITEMS, key, Material.class);
                if (tag == null) {
                    plugin.getLogger().warning("Unknown item tag " + entry + " in material " + id);
                    continue;
                }
                inputTags.add(tag);
                continue;
            }

            NamespacedKey key = NamespacedKey.fromString(normalized);
            if (key == null) {
                plugin.getLogger().warning("Invalid input material " + entry + " in material " + id);
                continue;
            }
            if (key.getNamespace().equals(NamespacedKey.MINECRAFT)) {
                Material material = Material.matchMaterial(normalized);
                if (material == null || !material.isItem()) {
                    plugin.getLogger().warning("Unknown item " + entry + " in material " + id);
                    continue;
                }
                inputItems.add(material);
            } else {
                inputCustomItems.add(key.asString());
            }
        }
    }

    /***
     * Checks if an item can be used to craft parts of this material.
     * Custom items only match their own ID, so a custom item built on a vanilla item won't match that item's tags.
     */
    public boolean matches(ItemStack item) {
        if (item == null || item.isEmpty()) {
            return false;
        }

        String customId = getCustomItemId(item);
        if (customId != null) {
            return inputCustomItems.contains(customId);
        }

        Material type = item.getType();
        if (inputItems.contains(type)) {
            return true;
        }
        for (Tag<Material> tag : inputTags) {
            if (tag.isTagged(type)) {
                return true;
            }
        }
        return false;
    }

    private static String getCustomItemId(ItemStack item) {
        List<String> compats = plugin.getEnabledCompats();
        if (compats.contains("ItemsAdder")) {
            String id = ItemsAdderCompat.getCustomItemId(item);
            if (id != null) {
                return id;
            }
        }
        if (compats.contains("CraftEngine")) {
            return CraftEngineCompat.getCustomItemId(item);
        }
        return null;
    }

    /***
     * Gets a tool material from the material's ID.
     * @param id
     * @return the ToolMaterial or null if no material is found
     */
    public static ToolMaterial fromId(String id) {
        return plugin.getToolMaterialRegistry().getToolMaterial(id);
    }

    /***
     * Gets the tool material an item can be crafted into.
     * @param item
     * @return the ToolMaterial or null if the item isn't an input for any material
     */
    public static ToolMaterial fromItemStack(ItemStack item) {
        return plugin.getToolMaterialRegistry().getToolMaterial(item);
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public double getDurability() {
        return durability;
    }

    public double getDurabilityMultiplier() {
        return durabilityMultiplier;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public double getAttackDamageMultiplier() {
        return attackDamageMultiplier;
    }

    public double getDigSpeed() {
        return digSpeed;
    }

    public double getDigSpeedMultiplier() {
        return digSpeedMultiplier;
    }

    public double getArmor() {
        return armor;
    }

    public double getArmorMultiplier() {
        return armorMultiplier;
    }

    public double getArmorToughness() {
        return armorToughness;
    }

    public double getArmorToughnessMultiplier() {
        return armorToughnessMultiplier;
    }

    public int getMiningLevel() {
        return miningLevel;
    }

    public Map<String, ?> getAttributeMap() {
        return attributeMap;
    }

    public double getAttackSpeed() {
        return attackSpeed;
    }

    public double getAttackSpeedMultiplier() {
        return attackSpeedMultiplier;
    }

    public List<PartType> getValidParts() {
        return validParts;
    }

    public List<String> getValidInputMaterials() {
        return validInputMaterials;
    }

    /***
     * @return the modifiers tools made with this material can get and their levels. Empty if the material has none.
     */
    public Map<Modifier, Integer> getModifiers() {
        return modifiers;
    }
}
