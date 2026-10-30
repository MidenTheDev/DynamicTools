package us.to.midensthings.dynamicTools.materials;

public enum PartType {
    // Common
    BINDING,
    HANDLE,

    // Tools (handle + binding + tool head = complete piece)
    PICKAXE_HEAD,
    AXE_HEAD,
    HOE_HEAD,
    SHOVEL_HEAD,

    // Weapons (handle + binding + weapon head = complete piece)
    SWORD_BLADE,
    SPEAR_HEAD,
    MACE_CORE,

    // Armor (Binding + Armor Core + frame = complete piece)
    ARMOR_BINDING,
    ARMOR_CORE,
    HELMET_FRAME,
    CHESTPLATE_FRAME,
    LEGGINGS_FRAME,
    BOOTS_FRAME,

}
