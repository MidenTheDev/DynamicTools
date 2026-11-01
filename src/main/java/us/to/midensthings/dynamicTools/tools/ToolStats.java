package us.to.midensthings.dynamicTools.tools;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/***
 * The stats of a tool while it's being built, after the materials have been applied. StatModifiers change these.
 */
public class ToolStats {

    /***
     * An attribute other than the tool's base stats, e.g. interaction range. Applied while the tool is in the main hand,
     * or while armor is worn.
     */
    public record ExtraAttribute(Attribute attribute, double amount, AttributeModifier.Operation operation) {}

    private double attackDamage;
    private double attackSpeed;
    private double durability;
    private double digSpeed;
    // Only used by armor
    private double armor;
    private double armorToughness;
    private final List<ExtraAttribute> extraAttributes = new ArrayList<>();
    // The tool's values for the attributes in custom-attributes.yml, by attribute ID. Only attributes its materials set.
    private final Map<String, Double> customAttributes = new LinkedHashMap<>();

    public ToolStats(double attackDamage, double attackSpeed, double durability, double digSpeed) {
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.durability = durability;
        this.digSpeed = digSpeed;
    }

    /***
     * Adds an attribute modifier to the tool for anything that isn't one of the base stats.
     * e.g. addAttribute(Attribute.ENTITY_INTERACTION_RANGE, 0.5, AttributeModifier.Operation.ADD_NUMBER)
     */
    public void addAttribute(Attribute attribute, double amount, AttributeModifier.Operation operation) {
        extraAttributes.add(new ExtraAttribute(attribute, amount, operation));
    }

    public List<ExtraAttribute> getExtraAttributes() {
        return extraAttributes;
    }

    public double getAttackDamage() {
        return attackDamage;
    }

    public void setAttackDamage(double attackDamage) {
        this.attackDamage = attackDamage;
    }

    public double getAttackSpeed() {
        return attackSpeed;
    }

    public void setAttackSpeed(double attackSpeed) {
        this.attackSpeed = attackSpeed;
    }

    public double getDurability() {
        return durability;
    }

    public void setDurability(double durability) {
        this.durability = durability;
    }

    public double getDigSpeed() {
        return digSpeed;
    }

    public void setDigSpeed(double digSpeed) {
        this.digSpeed = digSpeed;
    }

    public double getArmor() {
        return armor;
    }

    public void setArmor(double armor) {
        this.armor = armor;
    }

    public double getArmorToughness() {
        return armorToughness;
    }

    public void setArmorToughness(double armorToughness) {
        this.armorToughness = armorToughness;
    }

    /***
     * @return the tool's value for a custom attribute from custom-attributes.yml, or null if the tool doesn't have it
     */
    public Double getCustomAttribute(String id) {
        return customAttributes.get(id);
    }

    /***
     * Sets the tool's value for a custom attribute from custom-attributes.yml, adding it if the tool doesn't have it yet.
     */
    public void setCustomAttribute(String id, double value) {
        customAttributes.put(id, value);
    }

    public Map<String, Double> getCustomAttributes() {
        return customAttributes;
    }
}
