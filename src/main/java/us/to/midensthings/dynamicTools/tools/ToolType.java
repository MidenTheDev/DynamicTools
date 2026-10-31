package us.to.midensthings.dynamicTools.tools;

import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import us.to.midensthings.dynamicTools.parts.PartType;

import java.util.List;
import java.util.Locale;

public enum ToolType {

    // sword, base damage
    // axe, 25% more damage
    // spear, 33% less damage
    // mace, 25% less damage

    PICKAXE(0.25,0.75, PartType.PICKAXE_HEAD,PartType.HANDLE,PartType.BINDING),
    AXE(1.25,0.625, PartType.AXE_HEAD,PartType.HANDLE,PartType.BINDING),
    SHOVEL(0.25,0.625, PartType.SHOVEL_HEAD,PartType.HANDLE,PartType.BINDING),
    HOE(0.2,2.5, PartType.HOE_HEAD,PartType.HANDLE,PartType.BINDING),

    SWORD(1.0,1.0, PartType.SWORD_BLADE,PartType.HANDLE,PartType.BINDING),
    MACE(0.75,0.375, PartType.MACE_CORE,PartType.HANDLE,PartType.BINDING),
    SPEAR(0.66,0.66, PartType.SPEAR_HEAD,PartType.HANDLE,PartType.BINDING),

    // Armor (frame + core + binding = complete piece)
    // The armor mods give vanilla iron armor's points (2/6/5/2) from iron's armor stat of 2,
    // the durability mods are vanilla's per-piece durability (11/16/15/13) halved, as the parts add up to double the material's stat
    HELMET(EquipmentSlot.HEAD, 0.5, 5.5, PartType.HELMET_FRAME,PartType.ARMOR_CORE,PartType.ARMOR_BINDING),
    CHESTPLATE(EquipmentSlot.CHEST, 1.5, 8.0, PartType.CHESTPLATE_FRAME,PartType.ARMOR_CORE,PartType.ARMOR_BINDING),
    LEGGINGS(EquipmentSlot.LEGS, 1.25, 7.5, PartType.LEGGINGS_FRAME,PartType.ARMOR_CORE,PartType.ARMOR_BINDING),
    BOOTS(EquipmentSlot.FEET, 0.5, 6.5, PartType.BOOTS_FRAME,PartType.ARMOR_CORE,PartType.ARMOR_BINDING);

    private double attackDamageMod;
    private double attackSpeedMod;
    private PartType primaryPart;
    private PartType secondaryPart;
    private PartType tertiaryPart;
    private List<PartType> parts;

    // Only used by armor
    private EquipmentSlot armorSlot;
    private double armorMod;
    private double armorDurabilityMod;

    ToolType(double attackDamageMod, double attackSpeedMod, PartType primaryPart, PartType secondaryPart, PartType tertiaryPart) {
        this.attackDamageMod = attackDamageMod;
        this.attackSpeedMod = attackSpeedMod;
        this.primaryPart = primaryPart;
        this.secondaryPart = secondaryPart;
        this.tertiaryPart = tertiaryPart;
        this.parts = List.of(primaryPart,secondaryPart,tertiaryPart);
    }

    ToolType(EquipmentSlot armorSlot, double armorMod, double armorDurabilityMod, PartType primaryPart, PartType secondaryPart, PartType tertiaryPart) {
        this(0, 0, primaryPart, secondaryPart, tertiaryPart);
        this.armorSlot = armorSlot;
        this.armorMod = armorMod;
        this.armorDurabilityMod = armorDurabilityMod;
    }

    /***
     * @return true if this is a piece of armor rather than a tool or weapon
     */
    public boolean isArmor() {
        return armorSlot != null;
    }

    /***
     * @return the slot the armor is worn in, or null if this isn't armor
     */
    public EquipmentSlot getArmorSlot() {
        return armorSlot;
    }

    /***
     * @return the slots the tool's attributes apply in: its armor slot for armor, the main hand for everything else
     */
    public EquipmentSlotGroup getAttributeSlot() {
        return isArmor() ? armorSlot.getGroup() : EquipmentSlotGroup.MAINHAND;
    }

    public double getArmorMod() {
        return armorMod;
    }

    public double getArmorDurabilityMod() {
        return armorDurabilityMod;
    }

    public double getAttackDamageMod() {
        return attackDamageMod;
    }

    public double getAttackSpeedMod() {
        return attackSpeedMod;
    }

    public List<PartType> getParts() {
        return parts;
    }

    public PartType getPrimaryPart() {
        return primaryPart;
    }

    public PartType getSecondaryPart() {
        return secondaryPart;
    }

    public PartType getTertiaryPart() {
        return tertiaryPart;
    }

    /***
     * Finds the tool type made from the given parts.
     * @return the ToolType, or null if the parts don't make a tool.
     */
    public static ToolType fromParts(PartType primaryPart, PartType secondaryPart, PartType tertiaryPart) {
        for (ToolType toolType : values()) {
            if (toolType.primaryPart == primaryPart && toolType.secondaryPart == secondaryPart && toolType.tertiaryPart == tertiaryPart) {
                return toolType;
            }
        }
        return null;
    }

    /***
     * @return true if this tool type digs blocks, and so has a dig speed.
     */
    public boolean isMiningTool() {
        return switch (this) {
            case PICKAXE, AXE, SHOVEL, HOE -> true;
            default -> false;
        };
    }

    public String getDisplayName() {
        return this.name().charAt(0)+this.name().substring(1).toLowerCase(Locale.ROOT);
    }
}
