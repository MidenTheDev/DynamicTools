package us.to.midensthings.dynamicTools.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ItemLore;
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
import us.to.midensthings.dynamicTools.parts.PartType;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;
import us.to.midensthings.dynamicTools.util.ItemManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PartCrafter implements DynamicToolsGui {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    // Stonecutter-like GUI
    // Put material in left slot, possible parts show up in the right-hand slots.
    // Only show valid parts that are in the parts list.
    private Inventory inv;
    private boolean fillSlots;
    private ItemStack slotFillMaterial;
    private ItemStack pointerMaterial;
    String inventoryDisplayName;

    private final Player p;

    private final int inputSlot = 10;
    private final int pointerSlot = 12;
    // 3x5 grid on the right, enough for every part type
    private final int[] outputSlots = {
            4, 5, 6, 7, 8,
            13, 14, 15, 16, 17,
            22, 23, 24, 25, 26
    };
    private final Map<Integer, PartType> outputParts = new HashMap<>();

    private final int rows = 3;

    public PartCrafter(Player player) {
        p = player;

        FileConfiguration config = plugin.getConfig();
        inventoryDisplayName = config.getString("part-crafter.gui.title", "Part Crafter");
        fillSlots = config.getBoolean("part-crafter.gui.fill-empty-slots", true);
        slotFillMaterial = GuiItems.createDecoration(config.getString("part-crafter.gui.fill-material"), Material.GRAY_STAINED_GLASS_PANE);
        pointerMaterial = GuiItems.createDecoration(config.getString("part-crafter.gui.pointer-material"), Material.ARROW);

        this.inv = Bukkit.createInventory(this, rows*9, MiniMessage.miniMessage().deserialize(inventoryDisplayName));

        if (fillSlots) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                if (slot != inputSlot) {
                    inv.setItem(slot, slotFillMaterial);
                }
            }
        }
        inv.setItem(pointerSlot, pointerMaterial);
    }

    @Override
    public boolean isInputSlot(int slot) {
        return slot == inputSlot;
    }

    @Override
    public void update() {
        outputParts.clear();
        for (int slot : outputSlots) {
            inv.setItem(slot, fillSlots ? slotFillMaterial : null);
        }

        ItemStack input = inv.getItem(inputSlot);
        ToolMaterial material = ToolMaterial.fromItemStack(input);
        if (material == null) {
            return;
        }

        int index = 0;
        for (PartType partType : material.getValidParts()) {
            if (index >= outputSlots.length) {
                break;
            }
            ItemStack preview = plugin.getPartManager().getNewPartStack(partType, material);
            if (preview == null) {
                continue;
            }
            addCostLore(preview, partType.getMaterialCost(), input.getAmount() >= partType.getMaterialCost());
            inv.setItem(outputSlots[index], preview);
            outputParts.put(outputSlots[index], partType);
            index++;
        }
    }

    private void addCostLore(ItemStack preview, int cost, boolean canAfford) {
        ItemLore existing = preview.getData(DataComponentTypes.LORE);
        List<Component> lines = existing == null ? new ArrayList<>() : new ArrayList<>(existing.lines());
        lines.add(Component.text("Cost: " + cost + " material" + (cost == 1 ? "" : "s"), canAfford ? NamedTextColor.GRAY : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        preview.setData(DataComponentTypes.LORE, ItemLore.lore(lines));
    }

    /***
     * @return the part type shown in an output slot, or null if the slot doesn't show a part.
     */
    public PartType getPartAt(int slot) {
        return outputParts.get(slot);
    }

    /***
     * Creates the part from the current input without using up the input. Call consumeInput once the part has been given to the player.
     * @return the new part, or null if the input can't make this part or there isn't enough of it.
     */
    public ItemStack createPart(PartType partType) {
        ItemStack input = inv.getItem(inputSlot);
        ToolMaterial material = ToolMaterial.fromItemStack(input);
        if (material == null || !material.getValidParts().contains(partType) || input.getAmount() < partType.getMaterialCost()) {
            return null;
        }
        return plugin.getPartManager().getNewPartStack(partType, material);
    }

    public void consumeInput(PartType partType) {
        GuiItems.subtract(inv, inputSlot, partType.getMaterialCost());
    }

    @Override
    public ItemStack insertFromPlayer(ItemStack item) {
        if (ToolMaterial.fromItemStack(item) == null) {
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
        update();
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }

    public boolean doFillSlots() {
        return fillSlots;
    }

    public ItemStack getSlotFillMaterial() {
        return slotFillMaterial;
    }

    public Player getPlayer() {
        return p;
    }

    public int getInputSlot() {
        return inputSlot;
    }

    public int getPointerSlot() {
        return pointerSlot;
    }

    public int[] getOutputSlots() {
        return outputSlots;
    }
}
