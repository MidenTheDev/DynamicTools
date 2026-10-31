package us.to.midensthings.dynamicTools.tools;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemAttributeModifiers;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import io.papermc.paper.persistence.PersistentDataContainerView;
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
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.modifiers.Modifier;
import us.to.midensthings.dynamicTools.modifiers.StatModifier;
import us.to.midensthings.dynamicTools.resourcepack.ResourcePackGenerator;
import us.to.midensthings.dynamicTools.util.NamespacedKeys;
import us.to.midensthings.dynamicTools.util.RomanNumerals;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class Tool {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    // Same number format vanilla uses for attribute tooltips
    private static final DecimalFormat STAT_FORMAT = new DecimalFormat("#.##", DecimalFormatSymbols.getInstance(Locale.ROOT));

    private final ItemStack item;

    private final ToolType toolType;

    private int miningLevel;

    private final ToolMaterial primaryMaterial;
    private final ToolMaterial secondaryMaterial;
    private final ToolMaterial tertiaryMaterial;

    // Map of modifiers and their levels
    private Map<Modifier,Integer> modifiers;
    // Modifiers added on top of the materials' modifiers, e.g. with /dt addModifier
    private Map<Modifier,Integer> bonusModifiers;


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
        this(toolType, primaryMaterial, secondaryMaterial, tertiaryMaterial, Map.of());
    }

    /***
     * Constructs a tool with extra modifiers on top of the ones from its materials. Bonus modifiers always apply,
     * even to tool types they aren't valid for, and add their levels to the same modifier from the materials.
     * Does not check if the materials are valid for the tool in question.
     */
    public Tool(ToolType toolType, ToolMaterial primaryMaterial, ToolMaterial secondaryMaterial, ToolMaterial tertiaryMaterial,
                Map<Modifier,Integer> bonusModifiers) {
        this.toolType = toolType;
        this.primaryMaterial = primaryMaterial;
        this.secondaryMaterial = secondaryMaterial;
        this.tertiaryMaterial = tertiaryMaterial;

        this.miningLevel = primaryMaterial.getMiningLevel();


        // Construct item
        String baseMaterialType;

        // 1 or lower (including a material with no miningLevel set) is wood, 4 and up is diamond
        if (miningLevel >= 4) {
            baseMaterialType = "DIAMOND_";
        } else {
            switch (miningLevel) {
                case 2 -> baseMaterialType = "STONE_";
                case 3 -> baseMaterialType = "IRON_";
                default -> baseMaterialType = "WOODEN_";
            }
        }
        Material baseMaterial;

        if (toolType.isArmor()) {
            // Armor uses the vanilla armor of the frame's tier, which sets its equip sound and enchantability
            String armorMaterialType;
            if (miningLevel >= 4) {
                armorMaterialType = "DIAMOND_";
            } else {
                switch (miningLevel) {
                    case 2 -> armorMaterialType = "CHAINMAIL_";
                    case 3 -> armorMaterialType = "IRON_";
                    default -> armorMaterialType = "LEATHER_";
                }
            }
            baseMaterial = Material.valueOf(armorMaterialType + toolType.name());
        } else {
            switch (toolType) {
                case AXE -> baseMaterial = Material.valueOf(baseMaterialType+"AXE");
                case MACE -> baseMaterial = Material.MACE;
                case SPEAR -> baseMaterial = Material.valueOf(baseMaterialType+"SPEAR");
                case SWORD -> baseMaterial = Material.valueOf(baseMaterialType+"SWORD");


                case SHOVEL -> baseMaterial = Material.valueOf(baseMaterialType+"SHOVEL");
                case PICKAXE -> baseMaterial = Material.valueOf(baseMaterialType+"PICKAXE");
                default -> baseMaterial = Material.valueOf(baseMaterialType+"HOE");
            }
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


        /*
        Durability formula
        (Head Material Durability + 1/2 handle material Durability + 1/2 binding material Durability) / 2 X (Mean durability multiplier across all parts) rounded down.
         */
        double durabilityStat = (primaryMaterial.getDurability() + (0.5 * secondaryMaterial.getDurability()) + (0.5 * tertiaryMaterial.getDurability())) *
                ((primaryMaterial.getDurabilityMultiplier() + secondaryMaterial.getDurabilityMultiplier() + tertiaryMaterial.getDurabilityMultiplier()) / 3);

        /*
        Dig speed formula
        (Head Material Dig Speed + 1/2 handle material Dig Speed + 1/2 binding material Dig Speed) / 2 X (Mean dig speed multiplier across all parts).
         */
        double digSpeedStat = (primaryMaterial.getDigSpeed() + (0.5 * secondaryMaterial.getDigSpeed()) + (0.5 * tertiaryMaterial.getDigSpeed())) *
                ((primaryMaterial.getDigSpeedMultiplier() + secondaryMaterial.getDigSpeedMultiplier() + tertiaryMaterial.getDigSpeedMultiplier()) / 3);

        /*
        Armor formulas, same weighting as the tool stats
        Armor: Armor Type Modifier X (Frame Armor + 1/2 core Armor + 1/2 binding Armor) X (Mean armor multiplier across all parts)
        Toughness: (Frame Toughness + 1/2 core Toughness + 1/2 binding Toughness) X (Mean toughness multiplier across all parts)
        Durability: Armor Type Durability Modifier X (Frame + 1/2 core + 1/2 binding armor durability) X (Mean durability multiplier across all parts)
         */
        double armorStat = toolType.getArmorMod() * ((primaryMaterial.getArmor() + (0.5 * secondaryMaterial.getArmor()) + (0.5 * tertiaryMaterial.getArmor())) *
                ((primaryMaterial.getArmorMultiplier() + secondaryMaterial.getArmorMultiplier() + tertiaryMaterial.getArmorMultiplier()) / 3));
        double armorToughnessStat = (primaryMaterial.getArmorToughness() + (0.5 * secondaryMaterial.getArmorToughness()) + (0.5 * tertiaryMaterial.getArmorToughness())) *
                ((primaryMaterial.getArmorToughnessMultiplier() + secondaryMaterial.getArmorToughnessMultiplier() + tertiaryMaterial.getArmorToughnessMultiplier()) / 3);
        double armorDurabilityStat = toolType.getArmorDurabilityMod() * ((primaryMaterial.getArmorDurability() + (0.5 * secondaryMaterial.getArmorDurability()) + (0.5 * tertiaryMaterial.getArmorDurability())) *
                ((primaryMaterial.getDurabilityMultiplier() + secondaryMaterial.getDurabilityMultiplier() + tertiaryMaterial.getDurabilityMultiplier()) / 3));

        // Apply modifiers to the stats from the materials
        this.modifiers = collectModifiers(toolType, primaryMaterial, secondaryMaterial, tertiaryMaterial);
        this.bonusModifiers = new LinkedHashMap<>(bonusModifiers);
        bonusModifiers.forEach((modifier, level) -> modifiers.merge(modifier, level, Integer::sum));
        plugin.getModifierManager().setBonusModifiers(item, this.bonusModifiers);
        ToolStats stats = new ToolStats(damageStat, speedStat, toolType.isArmor() ? armorDurabilityStat : durabilityStat, digSpeedStat);
        if (toolType.isArmor()) {
            stats.setArmor(armorStat);
            stats.setArmorToughness(armorToughnessStat);
        }
        modifiers.forEach((modifier, level) -> {
            if (modifier instanceof StatModifier statModifier) {
                statModifier.apply(stats, level);
            }
        });
        plugin.getModifierManager().setModifiers(item, modifiers);

        EquipmentSlotGroup attributeSlot = toolType.getAttributeSlot();
        ItemAttributeModifiers.Builder mods = ItemAttributeModifiers.itemAttributes();
        if (toolType.isArmor()) {
            // Same keys vanilla armor uses, e.g. minecraft:armor.helmet
            NamespacedKey armorKey = NamespacedKey.minecraft("armor." + toolType.name().toLowerCase(Locale.ROOT));
            mods.addModifier(Attribute.ARMOR, new AttributeModifier(armorKey, stats.getArmor(), AttributeModifier.Operation.ADD_NUMBER, attributeSlot));
            if (stats.getArmorToughness() > 0) {
                mods.addModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(armorKey, stats.getArmorToughness(),
                        AttributeModifier.Operation.ADD_NUMBER, attributeSlot));
            }
        } else {
            mods.addModifier(Attribute.ATTACK_DAMAGE,
                            new AttributeModifier(NamespacedKey.minecraft("base_attack_damage"), stats.getAttackDamage() - 1.0,
                                    AttributeModifier.Operation.ADD_NUMBER, attributeSlot))
                    .addModifier(Attribute.ATTACK_SPEED,
                            new AttributeModifier(NamespacedKey.minecraft("base_attack_speed"), stats.getAttackSpeed() - 4.0,
                                    AttributeModifier.Operation.ADD_NUMBER, attributeSlot));
        }
        List<ToolStats.ExtraAttribute> extraAttributes = stats.getExtraAttributes();
        for (int i = 0; i < extraAttributes.size(); i++) {
            ToolStats.ExtraAttribute extra = extraAttributes.get(i);
            mods.addModifier(extra.attribute(), new AttributeModifier(new NamespacedKey(plugin, "tool_modifier_" + i), extra.amount(),
                    extra.operation(), attributeSlot));
        }
        item.setData(DataComponentTypes.ATTRIBUTE_MODIFIERS, mods.build());

        item.setData(DataComponentTypes.MAX_DAMAGE, Math.max(1, (int) Math.floor(stats.getDurability())));
        digSpeedStat = stats.getDigSpeed();

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
        // Modifiers go above the attributes, where vanilla shows enchantments.
        List<Component> lore = new ArrayList<>();
        modifiers.forEach((modifier, level) -> lore.add(modifierLine(modifier, level)));
        lore.add(Component.empty());
        lore.add(Component.translatable(getSlotTranslationKey(toolType), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (toolType.isArmor()) {
            // Vanilla shows armor as blue "+3 Armor" lines rather than green base stats
            lore.add(extraAttributeLine(new ToolStats.ExtraAttribute(Attribute.ARMOR, stats.getArmor(), AttributeModifier.Operation.ADD_NUMBER)));
            if (stats.getArmorToughness() > 0) {
                lore.add(extraAttributeLine(new ToolStats.ExtraAttribute(Attribute.ARMOR_TOUGHNESS, stats.getArmorToughness(), AttributeModifier.Operation.ADD_NUMBER)));
            }
        } else {
            lore.add(statLine(stats.getAttackDamage(), Component.translatable(Attribute.ATTACK_DAMAGE)));
            lore.add(statLine(stats.getAttackSpeed(), Component.translatable(Attribute.ATTACK_SPEED)));
        }
        if (toolType.isMiningTool()) {
            lore.add(statLine(digSpeedStat, Component.text("Mining Speed")));
        }
        for (ToolStats.ExtraAttribute extra : extraAttributes) {
            lore.add(extraAttributeLine(extra));
        }
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore));
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS)
                .build());

        ResourcePackGenerator.applyToolModel(item, toolType, primaryMaterial, secondaryMaterial, tertiaryMaterial);

        modifiers.forEach((modifier, level) -> modifier.applyToItem(item, level));
    }

    /***
     * @return the translation key of vanilla's "When in Main Hand:" / "When on Head:" line for the tool type
     */
    private static String getSlotTranslationKey(ToolType toolType) {
        if (!toolType.isArmor()) {
            return "item.modifiers.mainhand";
        }
        return switch (toolType.getArmorSlot()) {
            case HEAD -> "item.modifiers.head";
            case CHEST -> "item.modifiers.chest";
            case LEGS -> "item.modifiers.legs";
            default -> "item.modifiers.feet";
        };
    }

    /***
     * Gets the modifiers from the tool's materials that apply to this tool type. Levels from different materials add together,
     * but a material used in more than one part only counts once.
     */
    private static Map<Modifier,Integer> collectModifiers(ToolType toolType, ToolMaterial... materials) {
        Map<Modifier,Integer> modifiers = new LinkedHashMap<>();
        Set<ToolMaterial> countedMaterials = new HashSet<>();
        for (ToolMaterial material : materials) {
            if (!countedMaterials.add(material)) {
                continue;
            }
            material.getModifiers().forEach((modifier, level) -> {
                if (modifier.getValidToolTypes().contains(toolType)) {
                    modifiers.merge(modifier, level, Integer::sum);
                }
            });
        }
        return modifiers;
    }

    /***
     * Formats a modifier like a vanilla enchantment line, e.g. "Reinforced IV" in gray.
     */
    private static Component modifierLine(Modifier modifier, int level) {
        // The numeral goes inside the MiniMessage string so it gets the same formatting as the end of the name
        return MiniMessage.miniMessage().deserialize(modifier.getDisplayName() + " " + RomanNumerals.toRoman(level))
                .colorIfAbsent(NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false);
    }

    /***
     * Formats an extra attribute like a vanilla attribute modifier line, e.g. " +0.5 Entity Interaction Range" in blue.
     */
    private static Component extraAttributeLine(ToolStats.ExtraAttribute extra) {
        int operation = extra.operation().ordinal();
        // Vanilla shows the multiply operations as percentages
        double amount = operation == 0 ? extra.amount() : extra.amount() * 100;
        boolean positive = amount >= 0;
        return Component.space()
                .append(Component.translatable("attribute.modifier." + (positive ? "plus." : "take.") + operation,
                        Component.text(STAT_FORMAT.format(Math.abs(amount))), Component.translatable(extra.attribute())))
                .color(positive ? NamedTextColor.BLUE : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false);
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

    public Map<Modifier, Integer> getModifiers() {
        return modifiers;
    }

    public Map<Modifier, Integer> getBonusModifiers() {
        return bonusModifiers;
    }

    /***
     * Rebuilds a tool from the tool type, materials and bonus modifiers stored on its item, with the current materials.yml stats.
     * @param bonusModifiers the bonus modifiers for the rebuilt tool, or null to keep the item's bonus modifiers
     * @return the rebuilt tool, or null if the item isn't a tool or one of its materials no longer exists
     */
    public static Tool fromItemStack(ItemStack item, Map<Modifier,Integer> bonusModifiers) {
        if (!ToolItems.isTool(item)) {
            return null;
        }
        PersistentDataContainerView pdc = item.getPersistentDataContainer();
        ToolType toolType;
        try {
            toolType = ToolType.valueOf(pdc.get(NamespacedKeys.toolType, PersistentDataType.STRING));
        } catch (IllegalArgumentException e) {
            return null;
        }
        ToolMaterial primary = ToolMaterial.fromId(pdc.getOrDefault(NamespacedKeys.primaryMaterial, PersistentDataType.STRING, ""));
        ToolMaterial secondary = ToolMaterial.fromId(pdc.getOrDefault(NamespacedKeys.secondaryMaterial, PersistentDataType.STRING, ""));
        ToolMaterial tertiary = ToolMaterial.fromId(pdc.getOrDefault(NamespacedKeys.tertiaryMaterial, PersistentDataType.STRING, ""));
        if (primary == null || secondary == null || tertiary == null) {
            return null;
        }
        if (bonusModifiers == null) {
            bonusModifiers = plugin.getModifierManager().getBonusModifiers(item);
        }
        return new Tool(toolType, primary, secondary, tertiary, bonusModifiers);
    }

    /***
     * Copies what players can change about a tool (damage, enchantments, name, anvil repair cost) from an older copy
     * of the tool onto this one, e.g. after rebuilding it.
     */
    public void copyPlayerChanges(ItemStack oldItem) {
        int maxDamage = item.getDataOrDefault(DataComponentTypes.MAX_DAMAGE, 1);
        // Keep the tool from breaking if its new max durability is lower than the damage it had
        item.setData(DataComponentTypes.DAMAGE, Math.min(oldItem.getDataOrDefault(DataComponentTypes.DAMAGE, 0), maxDamage - 1));
        item.addUnsafeEnchantments(oldItem.getEnchantments());
        if (oldItem.hasData(DataComponentTypes.CUSTOM_NAME)) {
            item.setData(DataComponentTypes.CUSTOM_NAME, oldItem.getData(DataComponentTypes.CUSTOM_NAME));
        }
        if (oldItem.hasData(DataComponentTypes.REPAIR_COST)) {
            item.setData(DataComponentTypes.REPAIR_COST, oldItem.getData(DataComponentTypes.REPAIR_COST));
        }
    }
}
