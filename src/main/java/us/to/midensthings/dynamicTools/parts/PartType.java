package us.to.midensthings.dynamicTools.parts;

import java.util.Locale;

public enum PartType {
    // Material cost is how many material items the Part Crafter uses to make one of the part,
    // before the material's material-cost-ratio is applied.
    // Tool heads match the vanilla recipe's material count.

    // Common
    BINDING(1),
    HANDLE(1),

    // Tools (handle + binding + tool head = complete piece)
    PICKAXE_HEAD(3),
    AXE_HEAD(3),
    HOE_HEAD(2),
    SHOVEL_HEAD(1),

    // Weapons (handle + binding + weapon head = complete piece)
    SWORD_BLADE(2),
    SPEAR_HEAD(1),
    MACE_CORE(1),

    // Armor (Binding + Armor Core + frame = complete piece)
    ARMOR_BINDING(1),
    ARMOR_CORE(1),
    HELMET_FRAME(3),
    CHESTPLATE_FRAME(6),
    LEGGINGS_FRAME(5),
    BOOTS_FRAME(2),
    ;

    private final int materialCost;

    PartType(int materialCost) {
        this.materialCost = materialCost;
    }

    public int getMaterialCost() {
        return materialCost;
    }

    public String getDisplayName() {
        StringBuilder displayName = new StringBuilder();
        for (String word : this.name().toLowerCase(Locale.ROOT).split("_")) {
            if (!displayName.isEmpty()) {
                displayName.append(' ');
            }
            displayName.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return displayName.toString();
    }
}
