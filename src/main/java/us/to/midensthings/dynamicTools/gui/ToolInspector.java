package us.to.midensthings.dynamicTools.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.modifiers.Modifier;
import us.to.midensthings.dynamicTools.parts.Part;
import us.to.midensthings.dynamicTools.parts.PartType;
import us.to.midensthings.dynamicTools.tools.CustomAttribute;
import us.to.midensthings.dynamicTools.tools.Tool;
import us.to.midensthings.dynamicTools.tools.ToolItems;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;
import us.to.midensthings.dynamicTools.tools.ToolStats;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.ItemManager;
import us.to.midensthings.dynamicTools.util.RomanNumerals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.ToDoubleFunction;

/***
 * Shows the detailed stats of the tool put into its input slot: the material of each part, which materials give which
 * modifiers, and how every stat is worked out from the parts. Parts can be put in too, which shows what they're used for
 * and their material's stats. The item itself isn't changed.
 */
public class ToolInspector implements DynamicToolsGui {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    private Inventory inv;
    private boolean fillSlots;
    private ItemStack slotFillMaterial;
    private ItemStack pointerMaterial;
    private Material statsMaterial;
    private Material modifiersMaterial;
    String inventoryDisplayName;

    private final Player p;

    private final int inputSlot = 10;
    private final int pointerSlot = 11;
    // Head/frame, handle/core and binding
    // The parts in a column like the Part Assembler, by primary, secondary and tertiary part:
    // head or frame at the top, handle or core at the bottom, binding in the middle
    private final int[] partSlots = {4, 22, 13};
    // Where a part put in the inspector is shown
    private final int partInfoSlot = 13;
    private final int statsSlot = 15;
    // Stats past the first few go on a second item, as too many on one item runs off the screen
    private final int statsContinuedSlot = 16;
    private final int modifiersSlot = 17;
    private static final int STATS_PER_ITEM = 4;

    private final int rows = 3;

    /***
     * A stat worked out from the parts: type modifier X (head + 1/2 handle + 1/2 binding) X mean multiplier.
     */
    private record StatFormula(String name, ToDoubleFunction<ToolMaterial> value, ToDoubleFunction<ToolMaterial> multiplier,
                               double typeModifier, double finalValue) {}

    public ToolInspector(Player player) {
        p = player;

        FileConfiguration config = plugin.getConfig();
        inventoryDisplayName = config.getString("tool-inspector.gui.title", "Tool Inspector");
        fillSlots = config.getBoolean("tool-inspector.gui.fill-empty-slots", true);
        slotFillMaterial = GuiItems.createDecoration(config.getString("tool-inspector.gui.fill-material"), Material.GRAY_STAINED_GLASS_PANE);
        pointerMaterial = GuiItems.createDecoration(config.getString("tool-inspector.gui.pointer-material"), Material.ARROW);
        statsMaterial = iconMaterial(config.getString("tool-inspector.gui.stats-material"), Material.BOOK);
        modifiersMaterial = iconMaterial(config.getString("tool-inspector.gui.modifiers-material"), Material.ENCHANTED_BOOK);

        this.inv = Bukkit.createInventory(this, rows*9, MiniMessage.miniMessage().deserialize(inventoryDisplayName));
        clearInfo();
        if (fillSlots) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                if (slot != inputSlot) {
                    inv.setItem(slot, slotFillMaterial);
                }
            }
        }
        inv.setItem(pointerSlot, pointerMaterial);
    }

    private static Material iconMaterial(String name, Material fallback) {
        Material material = name == null ? null : Material.matchMaterial(name);
        if (material == null || !material.isItem() || material.isAir()) {
            return fallback;
        }
        return material;
    }

    @Override
    public boolean isInputSlot(int slot) {
        return slot == inputSlot;
    }

    @Override
    public void update() {
        clearInfo();
        ItemStack input = inv.getItem(inputSlot);
        if (ToolItems.isPart(input)) {
            Part part = Part.fromItemStack(input);
            if (part != null) {
                showPart(part, input);
            }
            return;
        }
        if (!ToolItems.isTool(input)) {
            return;
        }
        // Rebuilt from the tool's parts with the current materials.yml, which is how its stats are worked out
        Tool tool = Tool.fromItemStack(input, null);
        if (tool == null) {
            inv.setItem(statsSlot, infoItem(new ItemStack(Material.BARRIER), text("Unknown tool", NamedTextColor.RED),
                    List.of(text("This tool is made from a material that no longer exists.", NamedTextColor.GRAY))));
            return;
        }

        ToolType toolType = tool.getToolType();
        List<ToolMaterial> materials = List.of(tool.getPrimaryMaterial(), tool.getSecondaryMaterial(), tool.getTertiaryMaterial());
        List<PartType> parts = List.of(toolType.getPrimaryPart(), toolType.getSecondaryPart(), toolType.getTertiaryPart());
        List<StatFormula> formulas = getFormulas(tool);

        for (int i = 0; i < 3; i++) {
            inv.setItem(partSlots[i], partItem(tool, materials, parts, formulas, i));
        }
        List<List<Component>> statBlocks = statBlocks(tool, materials, formulas);
        inv.setItem(statsSlot, infoItem(new ItemStack(statsMaterial), text("Stats", NamedTextColor.WHITE),
                joinBlocks(statBlocks.subList(0, Math.min(STATS_PER_ITEM, statBlocks.size())))));
        if (statBlocks.size() > STATS_PER_ITEM) {
            inv.setItem(statsContinuedSlot, infoItem(new ItemStack(statsMaterial), text("Stats (continued)", NamedTextColor.WHITE),
                    joinBlocks(statBlocks.subList(STATS_PER_ITEM, statBlocks.size()))));
        }
        inv.setItem(modifiersSlot, modifiersItem(tool, materials));
    }

    /***
     * A material stat without a tool: its name and how to read a material's value and multiplier for it.
     */
    private record MaterialStat(String name, ToDoubleFunction<ToolMaterial> value, ToDoubleFunction<ToolMaterial> multiplier) {}

    private static List<MaterialStat> materialStats(boolean armor) {
        if (armor) {
            return List.of(
                    new MaterialStat("Armor", ToolMaterial::getArmor, ToolMaterial::getArmorMultiplier),
                    new MaterialStat("Armor Toughness", ToolMaterial::getArmorToughness, ToolMaterial::getArmorToughnessMultiplier),
                    new MaterialStat("Durability", ToolMaterial::getArmorDurability, ToolMaterial::getDurabilityMultiplier));
        }
        return List.of(
                new MaterialStat("Attack Damage", ToolMaterial::getAttackDamage, ToolMaterial::getAttackDamageMultiplier),
                new MaterialStat("Attack Speed", ToolMaterial::getAttackSpeed, ToolMaterial::getAttackSpeedMultiplier),
                new MaterialStat("Durability", ToolMaterial::getDurability, ToolMaterial::getDurabilityMultiplier),
                new MaterialStat("Mining Speed", ToolMaterial::getDigSpeed, ToolMaterial::getDigSpeedMultiplier));
    }

    /***
     * Shows a part: which tools it's used in and how much it counts there, its material's stats, the modifiers the
     * material gives and its custom attributes.
     */
    private void showPart(Part part, ItemStack input) {
        PartType partType = part.getPartType();
        ToolMaterial material = part.getToolMaterial();
        List<ToolType> usedIn = Arrays.stream(ToolType.values()).filter(type -> type.getParts().contains(partType)).toList();
        boolean armor = usedIn.stream().anyMatch(ToolType::isArmor);

        // What the part is used for
        List<Component> partLore = new ArrayList<>();
        if (usedIn.isEmpty()) {
            partLore.add(text("Not used in any tool.", NamedTextColor.GRAY));
        } else {
            partLore.add(text("Used in:", NamedTextColor.GRAY));
            for (ToolType type : usedIn) {
                int index = type.getParts().indexOf(partType);
                partLore.add(text("  " + type.getDisplayName() + ", as the " + partRole(type, index).toLowerCase()
                        + (index == 0 ? " (counts fully)" : " (counts for half)"), NamedTextColor.WHITE));
            }
        }
        int cost = material.getMaterialCost(partType);
        partLore.add(Component.empty());
        partLore.add(text("Part Crafter cost: " + cost + " material" + (cost == 1 ? "" : "s"), NamedTextColor.GRAY));
        inv.setItem(partInfoSlot, infoItem(input.clone(), mm(material.getDisplayName() + " " + partType.getDisplayName()), partLore));

        // The material's stats
        List<Component> statsLore = new ArrayList<>();
        for (MaterialStat stat : materialStats(armor)) {
            statsLore.add(text(stat.name() + ": ", NamedTextColor.GRAY)
                    .append(text(format(stat.value().applyAsDouble(material)), NamedTextColor.WHITE))
                    .append(text("  (x" + format(stat.multiplier().applyAsDouble(material)) + " multiplier)", NamedTextColor.DARK_GRAY)));
        }
        statsLore.addAll(materialAttributeLines(material));
        statsLore.add(Component.empty());
        statsLore.add(text("Tools get: head + half the handle + half the binding,", NamedTextColor.DARK_GRAY));
        statsLore.add(text("times the average multiplier of the three parts.", NamedTextColor.DARK_GRAY));
        inv.setItem(statsSlot, infoItem(new ItemStack(statsMaterial), mm(material.getDisplayName()).append(text(" Stats", NamedTextColor.WHITE)), statsLore));

        // The modifiers the material gives, and which of this part's tools they apply to
        List<Component> modifierLore = new ArrayList<>();
        material.getModifiers().forEach((modifier, level) -> {
            modifierLore.add(modifierName(modifier, level));
            List<String> appliesTo = usedIn.stream().filter(type -> modifier.getValidToolTypes().contains(type))
                    .map(ToolType::getDisplayName).toList();
            modifierLore.add(text(appliesTo.isEmpty() ? "  Doesn't apply to any tool this part makes"
                    : "  On: " + String.join(", ", appliesTo), NamedTextColor.GRAY));
        });
        if (modifierLore.isEmpty()) {
            modifierLore.add(text("This material gives no modifiers.", NamedTextColor.GRAY));
        }
        inv.setItem(modifiersSlot, infoItem(new ItemStack(modifiersMaterial), text("Modifiers", NamedTextColor.WHITE), modifierLore));

    }

    private void clearInfo() {
        ItemStack empty = fillSlots ? slotFillMaterial : null;
        for (int slot : partSlots) {
            inv.setItem(slot, empty);
        }
        inv.setItem(statsSlot, empty);
        inv.setItem(statsContinuedSlot, empty);
        inv.setItem(modifiersSlot, empty);
    }

    /***
     * @return the tool's stats, with the same formulas Tool uses
     */
    private static List<StatFormula> getFormulas(Tool tool) {
        ToolType toolType = tool.getToolType();
        ToolStats stats = tool.getStats();
        List<StatFormula> formulas = new ArrayList<>();
        if (toolType.isArmor()) {
            formulas.add(new StatFormula("Armor", ToolMaterial::getArmor, ToolMaterial::getArmorMultiplier, toolType.getArmorMod(), stats.getArmor()));
            formulas.add(new StatFormula("Armor Toughness", ToolMaterial::getArmorToughness, ToolMaterial::getArmorToughnessMultiplier, 1, stats.getArmorToughness()));
            formulas.add(new StatFormula("Durability", ToolMaterial::getArmorDurability, ToolMaterial::getDurabilityMultiplier,
                    toolType.getArmorDurabilityMod(), Math.floor(stats.getDurability())));
        } else {
            formulas.add(new StatFormula("Attack Damage", ToolMaterial::getAttackDamage, ToolMaterial::getAttackDamageMultiplier,
                    toolType.getAttackDamageMod(), stats.getAttackDamage()));
            formulas.add(new StatFormula("Attack Speed", ToolMaterial::getAttackSpeed, ToolMaterial::getAttackSpeedMultiplier,
                    toolType.getAttackSpeedMod(), stats.getAttackSpeed()));
            formulas.add(new StatFormula("Durability", ToolMaterial::getDurability, ToolMaterial::getDurabilityMultiplier, 1,
                    Math.floor(stats.getDurability())));
            if (toolType.isMiningTool()) {
                formulas.add(new StatFormula("Mining Speed", ToolMaterial::getDigSpeed, ToolMaterial::getDigSpeedMultiplier, 1, stats.getDigSpeed()));
            }
        }
        return formulas;
    }

    /***
     * One part's material: what it adds to each stat and which modifiers it gives.
     */
    private ItemStack partItem(Tool tool, List<ToolMaterial> materials, List<PartType> parts, List<StatFormula> formulas, int index) {
        ToolMaterial material = materials.get(index);
        PartType part = parts.get(index);
        ItemStack icon = plugin.getPartManager().getNewPartStack(part, material);
        if (icon == null) {
            icon = new ItemStack(Material.PAPER);
        }

        List<Component> lore = new ArrayList<>();
        lore.add(text(partRole(tool.getToolType(), index) + (index == 0 ? ", counts fully" : ", counts for half"), NamedTextColor.GRAY));
        lore.add(Component.empty());
        for (StatFormula formula : formulas) {
            lore.add(text(formula.name() + ": ", NamedTextColor.GRAY)
                    .append(text(format(formula.value().applyAsDouble(material)), NamedTextColor.WHITE))
                    .append(text("  (x" + format(formula.multiplier().applyAsDouble(material)) + " multiplier)", NamedTextColor.DARK_GRAY)));
        }

        lore.addAll(materialAttributeLines(material));

        // A material's modifiers only count once, even if it makes more than one part
        boolean countedEarlier = materials.subList(0, index).contains(material);
        List<Component> modifierLines = new ArrayList<>();
        material.getModifiers().forEach((modifier, level) -> {
            if (modifier.getValidToolTypes().contains(tool.getToolType())) {
                modifierLines.add(modifierName(modifier, level));
            }
        });
        if (!modifierLines.isEmpty()) {
            lore.add(Component.empty());
            lore.add(text(countedEarlier ? "Modifiers (already counted from the " + partRole(tool.getToolType(), materials.indexOf(material)).toLowerCase() + "):"
                    : "Gives modifiers:", NamedTextColor.GRAY));
            for (Component line : modifierLines) {
                lore.add(Component.text("  ").append(line));
            }
        }
        return infoItem(icon, mm(material.getDisplayName() + " " + part.getDisplayName()), lore);
    }

    /***
     * Every stat's final value and how it's worked out from the parts, one block of lines per stat,
     * followed by the custom attributes.
     */
    private List<List<Component>> statBlocks(Tool tool, List<ToolMaterial> materials, List<StatFormula> formulas) {
        List<List<Component>> blocks = new ArrayList<>();
        for (StatFormula formula : formulas) {
            List<Component> lore = new ArrayList<>();
            double head = formula.value().applyAsDouble(materials.get(0));
            double handle = formula.value().applyAsDouble(materials.get(1));
            double binding = formula.value().applyAsDouble(materials.get(2));
            double meanMultiplier = (formula.multiplier().applyAsDouble(materials.get(0)) + formula.multiplier().applyAsDouble(materials.get(1))
                    + formula.multiplier().applyAsDouble(materials.get(2))) / 3;
            double base = formula.typeModifier() * (head + 0.5 * handle + 0.5 * binding) * meanMultiplier;

            lore.add(text(formula.name() + ": ", NamedTextColor.WHITE).append(text(format(formula.finalValue()), NamedTextColor.GREEN)));
            lore.addAll(breakdown(tool.getToolType(), materials, head, handle, binding));
            if (formula.typeModifier() != 1) {
                lore.add(text("  x" + format(formula.typeModifier()) + " for a " + tool.getToolType().getDisplayName(), NamedTextColor.GRAY));
            }
            lore.add(text("  x" + format(meanMultiplier) + " average multiplier", NamedTextColor.GRAY));
            double fromModifiers = formula.finalValue() - (formula.name().equals("Durability") ? Math.floor(base) : base);
            if (Math.abs(fromModifiers) >= 0.005) {
                lore.add(text("  " + (fromModifiers > 0 ? "+" : "") + format(fromModifiers) + " from modifiers", NamedTextColor.GRAY));
            }
            blocks.add(lore);
        }
        blocks.addAll(attributeBlocks(tool, materials));
        return blocks;
    }

    /***
     * @return the blocks of lines, with an empty line between each
     */
    private static List<Component> joinBlocks(List<List<Component>> blocks) {
        List<Component> lore = new ArrayList<>();
        for (List<Component> block : blocks) {
            if (!lore.isEmpty()) {
                lore.add(Component.empty());
            }
            lore.addAll(block);
        }
        return lore;
    }

    /***
     * A material's custom attribute values, e.g. "example: 1.5  (x1 multiplier)". Attributes with a value of zero are left out.
     */
    private static List<Component> materialAttributeLines(ToolMaterial material) {
        List<Component> lines = new ArrayList<>();
        material.getCustomAttributes().forEach((id, value) -> {
            if (value.value() != 0) {
                lines.add(text(id + ": ", NamedTextColor.GRAY)
                        .append(text(format(value.value()), NamedTextColor.WHITE))
                        .append(text("  (x" + format(value.multiplier()) + " multiplier)", NamedTextColor.DARK_GRAY)));
            }
        });
        return lines;
    }

    /***
     * The lines for a stat's three parts, e.g. "  Head (Iron): 3" and "  Handle (Wood): +1 (half of 2)".
     */
    private List<Component> breakdown(ToolType toolType, List<ToolMaterial> materials, double head, double handle, double binding) {
        return List.of(
                partLine(partRole(toolType, 0), materials.get(0), format(head)),
                partLine(partRole(toolType, 1), materials.get(1), "+" + format(0.5 * handle) + " (half of " + format(handle) + ")"),
                partLine(partRole(toolType, 2), materials.get(2), "+" + format(0.5 * binding) + " (half of " + format(binding) + ")"));
    }

    private Component partLine(String role, ToolMaterial material, String value) {
        return text("  " + role + " (", NamedTextColor.GRAY)
                .append(mm(material.getDisplayName()))
                .append(text("): ", NamedTextColor.GRAY))
                .append(text(value, NamedTextColor.WHITE));
    }

    /***
     * Each modifier on the tool and where its levels come from.
     */
    private ItemStack modifiersItem(Tool tool, List<ToolMaterial> materials) {
        List<Component> lore = new ArrayList<>();
        Set<ToolMaterial> distinctMaterials = new LinkedHashSet<>(materials);
        tool.getModifiers().forEach((modifier, level) -> {
            lore.add(modifierName(modifier, level));
            for (ToolMaterial material : distinctMaterials) {
                Integer fromMaterial = material.getModifiers().get(modifier);
                if (fromMaterial != null && modifier.getValidToolTypes().contains(tool.getToolType())) {
                    lore.add(text("  " + RomanNumerals.toRoman(fromMaterial) + " from ", NamedTextColor.GRAY).append(mm(material.getDisplayName())));
                }
            }
            Integer bonus = tool.getBonusModifiers().get(modifier);
            if (bonus != null) {
                lore.add(text("  " + RomanNumerals.toRoman(bonus) + " added by command", NamedTextColor.GRAY));
            }
        });
        if (lore.isEmpty()) {
            lore.add(text("This tool has no modifiers.", NamedTextColor.GRAY));
        }
        return infoItem(new ItemStack(modifiersMaterial), text("Modifiers", NamedTextColor.WHITE), lore);
    }

    /***
     * Each custom attribute on the tool and how it's worked out from the parts, as blocks in the same layout as the normal stats.
     * Attributes with a final value of zero are left out.
     */
    private List<List<Component>> attributeBlocks(Tool tool, List<ToolMaterial> materials) {
        List<List<Component>> blocks = new ArrayList<>();
        tool.getStats().getCustomAttributes().forEach((id, finalValue) -> {
            CustomAttribute attribute = plugin.getCustomAttributes().get(id);
            if (attribute == null || finalValue == 0) {
                return;
            }
            CustomAttribute.MaterialValue fallback = new CustomAttribute.MaterialValue(attribute.defaultValue(), attribute.defaultMultiplier());
            List<CustomAttribute.MaterialValue> values = materials.stream()
                    .map(material -> material.getCustomAttributes().getOrDefault(id, fallback)).toList();
            double meanMultiplier = (values.get(0).multiplier() + values.get(1).multiplier() + values.get(2).multiplier()) / 3;
            double base = (values.get(0).value() + 0.5 * values.get(1).value() + 0.5 * values.get(2).value()) * meanMultiplier;

            List<Component> lore = new ArrayList<>();
            lore.add(text(id + ": ", NamedTextColor.WHITE).append(text(format(finalValue), NamedTextColor.GREEN)));
            lore.addAll(breakdown(tool.getToolType(), materials, values.get(0).value(), values.get(1).value(), values.get(2).value()));
            lore.add(text("  x" + format(meanMultiplier) + " average multiplier", NamedTextColor.GRAY));
            double fromModifiers = finalValue - base;
            if (Math.abs(fromModifiers) >= 0.005) {
                lore.add(text("  " + (fromModifiers > 0 ? "+" : "") + format(fromModifiers) + " from modifiers", NamedTextColor.GRAY));
            }
            lore.add(text("  Stored as " + attribute.key(), NamedTextColor.DARK_GRAY));
            blocks.add(lore);
        });
        return blocks;
    }

    /***
     * @return the name of the part at index 0-2: Head, Handle and Binding, or Frame, Core and Binding for armor
     */
    private static String partRole(ToolType toolType, int index) {
        if (toolType.isArmor()) {
            return new String[]{"Frame", "Core", "Binding"}[index];
        }
        return new String[]{"Head", "Handle", "Binding"}[index];
    }

    private static Component modifierName(Modifier modifier, int level) {
        return MiniMessage.miniMessage().deserialize(modifier.getDisplayName() + " " + RomanNumerals.toRoman(level))
                .colorIfAbsent(NamedTextColor.GRAY);
    }

    private static ItemStack infoItem(ItemStack item, Component name, List<Component> lore) {
        item.setData(DataComponentTypes.CUSTOM_NAME, name.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        item.setData(DataComponentTypes.LORE, ItemLore.lore(lore.stream()
                .map(line -> line.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE)).toList()));
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS)
                .build());
        return item;
    }

    private static Component text(String text, NamedTextColor color) {
        return Component.text(text, color);
    }

    private static Component mm(String miniMessage) {
        return MiniMessage.miniMessage().deserialize(miniMessage);
    }

    private static String format(double value) {
        return Tool.STAT_FORMAT.format(value);
    }

    @Override
    public ItemStack insertFromPlayer(ItemStack item) {
        if (!ToolItems.isDynamicToolsItem(item)) {
            return item;
        }
        return GuiItems.insert(inv, inputSlot, item);
    }

    @Override
    public void returnInputs(Player player) {
        ItemStack item = inv.getItem(inputSlot);
        if (item != null && !item.isEmpty()) {
            inv.setItem(inputSlot, null);
            ItemManager.addOrDropItem(item, player);
        }
        clearInfo();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public Player getPlayer() {
        return p;
    }

    public int getInputSlot() {
        return inputSlot;
    }
}
