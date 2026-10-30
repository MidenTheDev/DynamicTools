package us.to.midensthings.dynamicTools.gui;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.parts.Part;
import us.to.midensthings.dynamicTools.parts.PartType;
import us.to.midensthings.dynamicTools.tools.Tool;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.ItemManager;

public class PartAssembler implements DynamicToolsGui {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    // Anvil-like GUI
    // Put the head, handle and binding in the left slots, the finished tool shows up in the result slot.
    private Inventory inv;
    private boolean fillSlots;
    private ItemStack slotFillMaterial;
    private ItemStack pointerMaterial;
    String inventoryDisplayName;

    private final Player p;

    // Shown top-down as head, binding, handle. Primary/secondary/tertiary is how much each part affects the tool's stats, not the slot order.
    private final int primaryMaterialSlot = 2;
    private final int secondaryMaterialSlot = 20;
    private final int tertiaryMaterialSlot = 11;
    private final int pointerSlot = 13;
    private final int resultSlot = 16;

    private final int rows = 3;

    public PartAssembler(Player player) {
        p = player;

        FileConfiguration config = plugin.getConfig();
        inventoryDisplayName = config.getString("assembler.gui.title", "Part Assembler");
        fillSlots = config.getBoolean("assembler.gui.fill-empty-slots", true);
        slotFillMaterial = GuiItems.createDecoration(config.getString("assembler.gui.fill-material"), Material.GRAY_STAINED_GLASS_PANE);
        pointerMaterial = GuiItems.createDecoration(config.getString("assembler.gui.pointer-material"), Material.ARROW);

        this.inv = Bukkit.createInventory(this,rows*9, MiniMessage.miniMessage().deserialize(inventoryDisplayName));

        if (fillSlots) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                if (!isInputSlot(slot) && slot != resultSlot) {
                    inv.setItem(slot, slotFillMaterial);
                }
            }
        }
        inv.setItem(pointerSlot, pointerMaterial);
    }

    @Override
    public boolean isInputSlot(int slot) {
        return slot == primaryMaterialSlot || slot == secondaryMaterialSlot || slot == tertiaryMaterialSlot;
    }

    @Override
    public void update() {
        Tool tool = getResultTool();
        inv.setItem(resultSlot, tool == null ? null : tool.getItemStack());
    }

    /***
     * @return the tool made from the parts in the input slots, or null if they don't make a valid tool.
     */
    public Tool getResultTool() {
        Part primary = Part.fromItemStack(inv.getItem(primaryMaterialSlot));
        Part secondary = Part.fromItemStack(inv.getItem(secondaryMaterialSlot));
        Part tertiary = Part.fromItemStack(inv.getItem(tertiaryMaterialSlot));
        if (primary == null || secondary == null || tertiary == null) {
            return null;
        }

        ToolType toolType = ToolType.fromParts(primary.getPartType(), secondary.getPartType(), tertiary.getPartType());
        if (toolType == null) {
            return null;
        }
        return Tool.constructTool(toolType, primary.getToolMaterial(), secondary.getToolMaterial(), tertiary.getToolMaterial());
    }

    /***
     * Uses up one part from each input slot after the result has been taken.
     */
    public void consumeInputs() {
        GuiItems.subtract(inv, primaryMaterialSlot, 1);
        GuiItems.subtract(inv, secondaryMaterialSlot, 1);
        GuiItems.subtract(inv, tertiaryMaterialSlot, 1);
    }

    @Override
    public ItemStack insertFromPlayer(ItemStack item) {
        Part part = Part.fromItemStack(item);
        if (part == null) {
            return item;
        }
        int slot = getInputSlotFor(part.getPartType());
        if (slot == -1) {
            return item;
        }
        return GuiItems.insert(inv, slot, item);
    }

    /***
     * @return the input slot the part type goes in, or -1 if the part type isn't used by any tool.
     */
    public int getInputSlotFor(PartType partType) {
        for (ToolType toolType : ToolType.values()) {
            if (toolType.getPrimaryPart() == partType) {
                return primaryMaterialSlot;
            }
            if (toolType.getSecondaryPart() == partType) {
                return secondaryMaterialSlot;
            }
            if (toolType.getTertiaryPart() == partType) {
                return tertiaryMaterialSlot;
            }
        }
        return -1;
    }

    @Override
    public void returnInputs(Player player) {
        for (int slot : new int[]{primaryMaterialSlot, secondaryMaterialSlot, tertiaryMaterialSlot}) {
            ItemStack item = inv.getItem(slot);
            if (item != null && !item.isEmpty()) {
                inv.setItem(slot, null);
                ItemManager.addOrDropItem(item, player);
            }
        }
        inv.setItem(resultSlot, null);
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

    public int getPrimaryMaterialSlot() {
        return primaryMaterialSlot;
    }

    public int getSecondaryMaterialSlot() {
        return secondaryMaterialSlot;
    }

    public int getTertiaryMaterialSlot() {
        return tertiaryMaterialSlot;
    }

    public int getPointerSlot() {
        return pointerSlot;
    }

    public int getResultSlot() {
        return resultSlot;
    }
}
