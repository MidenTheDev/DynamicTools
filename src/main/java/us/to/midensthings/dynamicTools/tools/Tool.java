package us.to.midensthings.dynamicTools.tools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.KineticWeapon;
import io.papermc.paper.datacomponent.item.PiercingWeapon;
import io.papermc.paper.datacomponent.item.Weapon;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class Tool {
    private final ItemStack item;

    private final ToolType toolType;

    private int miningLevel;

    private final ToolMaterial headToolMaterial;
    private final ToolMaterial handleToolMaterial;
    private final ToolMaterial bindingToolMaterial;


    /* Mining Levels
    1 = Wood
    2 = Stone
    3 = Iron
    4 = Diamond
    5+ -> set to diamond
     */

    /***
     * Constructs a toll from the given ToolType and Materials. Does not check if the materials are valid for the tool in question.
     * @param toolType
     * @param headToolMaterial
     * @param handleToolMaterial
     * @param bindingToolMaterial
     */
    public Tool(ToolType toolType, ToolMaterial headToolMaterial, ToolMaterial handleToolMaterial, ToolMaterial bindingToolMaterial) {
        this.toolType = toolType;
        this.headToolMaterial = headToolMaterial;
        this.handleToolMaterial = handleToolMaterial;
        this.bindingToolMaterial = bindingToolMaterial;

        this.miningLevel = headToolMaterial.getMiningLevel();


        // Construct item
        String baseMaterialType;

        switch (miningLevel) {
            case 2 -> baseMaterialType = "STONE_";
            case 3 -> baseMaterialType = "IRON_";
            case 4 -> baseMaterialType = "DIAMOND_";
            default -> baseMaterialType = "WOODEN_";

        }
        Material baseMaterial;


        switch (toolType) {
            case AXE -> baseMaterial = Material.valueOf(baseMaterialType+"AXE");
            case MACE -> baseMaterial = Material.MACE;
            case SPEAR -> baseMaterial = Material.valueOf(baseMaterialType+"SPEAR");
            case SWORD -> baseMaterial = Material.valueOf(baseMaterialType+"SWORD");


            case SHOVEL -> baseMaterial = Material.valueOf(baseMaterialType+"SHOVEL");
            case PICKAXE -> baseMaterial = Material.valueOf(baseMaterialType+"PICKAXE");
            default -> baseMaterial = Material.valueOf(baseMaterialType+"HOE");
        }
        this.item = ItemStack.of(baseMaterial);
        item.editPersistentDataContainer(pdc -> {
           // TODO set PDC data for the tool
        });

        // Set data components of the item:

        /*
        Damage formula
        Tool Type Modifier X (Head Material DMG + 1/2 handle material DMG + 1/2 binding material DMG) X (Mean dmg multiplier across all parts) rounded down.
         */

        double damageStat = toolType.getAttackDamageMod() * (headToolMaterial.getAttackDamage() + (0.5 * handleToolMaterial.getAttackDamage()) + (0.5 * bindingToolMaterial.getAttackDamage()) *
                ((headToolMaterial.getAttackDamageMultiplier() + handleToolMaterial.getAttackDamageMultiplier() + bindingToolMaterial.getAttackDamageMultiplier()) / 3));

        /*
        Speed formula
        Tool Type Modifier X (Head Material Speed + 1/2 handle material Speed + 1/2 binding material Speed) X (Mean dmg multiplier across all parts) rounded down.
         */
        double speedStat = toolType.getAttackSpeedMod() * (headToolMaterial.getAttackSpeed() + (0.5 * handleToolMaterial.getAttackSpeed()) + (0.5 * bindingToolMaterial.getAttackSpeed()) *
                ((headToolMaterial.getAttackSpeedMultiplier() + handleToolMaterial.getAttackSpeedMultiplier() + bindingToolMaterial.getAttackSpeedMultiplier()) / 3));


        ItemAttributeModifiers mods = ItemAttributeModifiers.itemAttributes()
                .addModifier(Attribute.ATTACK_DAMAGE,
                        new AttributeModifier(NamespacedKey.minecraft("base_attack_damage"), damageStat - 1.0,
                                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                .addModifier(Attribute.ATTACK_SPEED,
                        new AttributeModifier(NamespacedKey.minecraft("base_attack_speed"), speedStat - 4.0,
                                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                .build();
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, mods);


        // TODO: Other stats

        // TODO: Hide attribute modifiers and replace with realistic looking item lore


    }

    /***
     * Constructs a tool from the given ToolType and Materials if the materials are valid.
     * @param toolType The type of tool being constructed
     * @param primaryMaterial the head/blade/armor frame material
     * @param secondaryMaterial the handle/armor core material
     * @param tertiaryMaterial the binding material
     * @return a new Tool object, or null if the materials are invalid.
     */
    public static Tool constructTool(ToolType toolType, ToolMaterial primaryMaterial, ToolMaterial secondaryMaterial, ToolMaterial tertiaryMaterial) {
        if (!primaryMaterial.getValidParts().contains(toolType.getPrimaryPart())) {
            return null;
        }
        if (!secondaryMaterial.getValidParts().contains(toolType.getSecondaryPart())) {
            return null;
        }
        if (!tertiaryMaterial.getValidParts().contains(toolType.getTertiaryPart())) {
            return null;
        }
        return new Tool(toolType, primaryMaterial, secondaryMaterial, tertiaryMaterial);
    }

    public ItemStack getItemStack() {
        return item;
    }

    public ToolType getToolType() {
        return toolType;
    }

    public int getMiningLevel() {
        return miningLevel;
    }

    public ToolMaterial getHeadToolMaterial() {
        return headToolMaterial;
    }

    public ToolMaterial getHandleToolMaterial() {
        return handleToolMaterial;
    }

    public ToolMaterial getBindingToolMaterial() {
        return bindingToolMaterial;
    }
}
