package us.to.midensthings.dynamicTools.tools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import us.to.midensthings.dynamicTools.util.NamespacedKeys;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Tool {
    // Same number format vanilla uses for attribute tooltips
    private static final DecimalFormat STAT_FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final ItemStack item;

    private final ToolType toolType;

    private int miningLevel;

    private final ToolMaterial primaryMaterial;
    private final ToolMaterial secondaryMaterial;
    private final ToolMaterial tertiaryMaterial;


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
     * @param primaryMaterial
     * @param secondaryMaterial
     * @param tertiaryMaterial
     */
    public Tool(ToolType toolType, ToolMaterial primaryMaterial, ToolMaterial secondaryMaterial, ToolMaterial tertiaryMaterial) {
        this.toolType = toolType;
        this.primaryMaterial = primaryMaterial;
        this.secondaryMaterial = secondaryMaterial;
        this.tertiaryMaterial = tertiaryMaterial;

        this.miningLevel = primaryMaterial.getMiningLevel();


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
            pdc.set(NamespacedKeys.toolType, PersistentDataType.STRING, toolType.name());
            pdc.set(NamespacedKeys.primaryMaterial, PersistentDataType.STRING, primaryMaterial.getId());
            pdc.set(NamespacedKeys.secondaryMaterial, PersistentDataType.STRING, secondaryMaterial.getId());
            pdc.set(NamespacedKeys.tertiaryMaterial, PersistentDataType.STRING, tertiaryMaterial.getId());
        });

        // Set data components of the item:

        /*
        Damage formula
        Tool Type Modifier X (Head Material DMG + 1/2 handle material DMG + 1/2 binding material DMG) X (Mean dmg multiplier across all parts) rounded down.
         */

        double damageStat = toolType.getAttackDamageMod() * ((primaryMaterial.getAttackDamage() + (0.5 * secondaryMaterial.getAttackDamage()) + (0.5 * tertiaryMaterial.getAttackDamage())) *
                ((primaryMaterial.getAttackDamageMultiplier() + secondaryMaterial.getAttackDamageMultiplier() + tertiaryMaterial.getAttackDamageMultiplier()) / 3));

        /*
        Speed formula
        Tool Type Modifier X (Head Material Speed + 1/2 handle material Speed + 1/2 binding material Speed) X (Mean dmg multiplier across all parts) rounded down.
         */
        double speedStat = toolType.getAttackSpeedMod() * ((primaryMaterial.getAttackSpeed() + (0.5 * secondaryMaterial.getAttackSpeed()) + (0.5 * tertiaryMaterial.getAttackSpeed())) *
                ((primaryMaterial.getAttackSpeedMultiplier() + secondaryMaterial.getAttackSpeedMultiplier() + tertiaryMaterial.getAttackSpeedMultiplier()) / 3));


        ItemAttributeModifiers mods = ItemAttributeModifiers.itemAttributes()
                .addModifier(Attribute.ATTACK_DAMAGE,
                        new AttributeModifier(NamespacedKey.minecraft("base_attack_damage"), damageStat - 1.0,
                                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                .addModifier(Attribute.ATTACK_SPEED,
                        new AttributeModifier(NamespacedKey.minecraft("base_attack_speed"), speedStat - 4.0,
                                AttributeModifier.Operation.ADD_NUMBER, EquipmentSlotGroup.MAINHAND))
                .build();
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, mods);

        /*
        Durability formula
        (Head Material Durability + 1/2 handle material Durability + 1/2 binding material Durability) / 2 X (Mean durability multiplier across all parts) rounded down.
         */
        double durabilityStat = (primaryMaterial.getDurability() + (0.5 * secondaryMaterial.getDurability()) + (0.5 * tertiaryMaterial.getDurability())) *
                ((primaryMaterial.getDurabilityMultiplier() + secondaryMaterial.getDurabilityMultiplier() + tertiaryMaterial.getDurabilityMultiplier()) / 3);

        item.setData(DataComponentTypes.MAX_DAMAGE, Math.max(1, (int) Math.floor(durabilityStat)));

        /*
        Dig speed formula
        (Head Material Dig Speed + 1/2 handle material Dig Speed + 1/2 binding material Dig Speed) / 2 X (Mean dig speed multiplier across all parts).
         */
        double digSpeedStat = (primaryMaterial.getDigSpeed() + (0.5 * secondaryMaterial.getDigSpeed()) + (0.5 * tertiaryMaterial.getDigSpeed())) *
                ((primaryMaterial.getDigSpeedMultiplier() + secondaryMaterial.getDigSpeedMultiplier() + tertiaryMaterial.getDigSpeedMultiplier()) / 3);

        // Dig speed lives on the tool component's rules, so rebuild the base item's rules with the new speed.
        // Rules without a speed (the incorrect_for_X_tool drop deny list) keep the mining level of the base item.
        if (toolType.isMiningTool()) {
            io.papermc.paper.datacomponent.item.Tool baseTool = item.getData(DataComponentTypes.TOOL);
            if (baseTool != null) {
                List<io.papermc.paper.datacomponent.item.Tool.Rule> rules = new ArrayList<>();
                for (io.papermc.paper.datacomponent.item.Tool.Rule rule : baseTool.rules()) {
                    rules.add(rule.speed() == null ? rule
                            : io.papermc.paper.datacomponent.item.Tool.rule(rule.blocks(), (float) digSpeedStat, rule.correctForDrops()));
                }
                item.setData(DataComponentTypes.TOOL, io.papermc.paper.datacomponent.item.Tool.tool()
                        .defaultMiningSpeed(baseTool.defaultMiningSpeed())
                        .damagePerBlock(baseTool.damagePerBlock())
                        .canDestroyBlocksInCreative(baseTool.canDestroyBlocksInCreative())
                        .addRules(rules)
                        .build());
            }
        }

        // Set ItemMeta values for the item:
        ItemMeta meta = item.getItemMeta();
        meta.customName(generateDisplayNameComponent());
        item.setItemMeta(meta);

        // Hide the vanilla attribute tooltip and redraw it in the lore so dig speed can sit in the same block.
        List<Component> lore = new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Component.translatable("item.modifiers.mainhand", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(statLine(damageStat, Component.translatable(Attribute.ATTACK_DAMAGE)));
        lore.add(statLine(speedStat, Component.translatable(Attribute.ATTACK_SPEED)));
        if (toolType.isMiningTool()) {
            lore.add(statLine(digSpeedStat, Component.text("Mining Speed")));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS)
                .build());

        // TODO: Set custom model for custom texture
    }

    /***
     * Formats a stat the same way as a vanilla "When in Main Hand:" attribute line, e.g. " 4 Attack Damage" in dark green.
     */
    private static Component statLine(double value, Component name) {
        return Component.space()
                .append(Component.translatable("attribute.modifier.equals.0", Component.text(STAT_FORMAT.format(value)), name))
                .color(NamedTextColor.DARK_GREEN)
                .decoration(TextDecoration.ITALIC, false);
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

    public Component generateDisplayNameComponent() {
        return MiniMessage.miniMessage().deserialize(generateDisplayNameString());
    }

    public String generateDisplayNameString() {
        return primaryMaterial.getDisplayName() + " " + toolType.getDisplayName();
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

    public ToolMaterial getPrimaryMaterial() {
        return primaryMaterial;
    }

    public ToolMaterial getSecondaryMaterial() {
        return secondaryMaterial;
    }

    public ToolMaterial getTertiaryMaterial() {
        return tertiaryMaterial;
    }
}
