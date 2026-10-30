package us.to.midensthings.dynamicTools.tools;

import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.materials.PartType;

import java.util.List;
import java.util.Map;

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

    // Map for other attributes this material modifies?
    private Map<String,?> attributeMap;


    public ToolMaterial(String id, double durability, double durabilityMultiplier, double attackDamage, double attackDamageMultiplier, double attackSpeed, double attackSpeedMultiplier, double digSpeed, double digSpeedMultiplier, double armor, double armorMultiplier, double armorToughness, double armorToughnessMultiplier, int miningLevel, String displayName, List<PartType> validParts, List<String> validInputMaterials) {
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
    }

    /***
     * Gets a tool material from the material's ID.
     * @param id
     * @return the ToolMaterial or null if no material is found
     */
    public static ToolMaterial fromId(String id) {
        return plugin.getToolMaterialRegistry().getToolMaterial(id);
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
}
