package us.to.midensthings.dynamicTools.tools;

import us.to.midensthings.dynamicTools.materials.PartType;

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
    SPEAR(0.66,0.66, PartType.SPEAR_HEAD,PartType.HANDLE,PartType.BINDING);

    private double attackDamageMod;
    private double attackSpeedMod;
    private PartType primaryPart;
    private PartType secondaryPart;
    private PartType tertiaryPart;
    private List<PartType> parts;

    ToolType(double attackDamageMod, double attackSpeedMod, PartType primaryPart, PartType secondaryPart, PartType tertiaryPart) {
        this.attackDamageMod = attackDamageMod;
        this.attackSpeedMod = attackSpeedMod;
        this.primaryPart = primaryPart;
        this.secondaryPart = secondaryPart;
        this.tertiaryPart = tertiaryPart;
        this.parts = List.of(primaryPart,secondaryPart,tertiaryPart);
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
