package us.to.midensthings.dynamicTools.events;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.gui.DynamicToolsGui;
import us.to.midensthings.dynamicTools.gui.PartAssembler;
import us.to.midensthings.dynamicTools.gui.PartCrafter;
import us.to.midensthings.dynamicTools.gui.WorkstationChooser;
import us.to.midensthings.dynamicTools.parts.PartType;
import us.to.midensthings.dynamicTools.tools.Tool;
import us.to.midensthings.dynamicTools.util.ItemManager;

public class GuiListener implements Listener {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder(false) instanceof WorkstationChooser chooser) {
            e.setCancelled(true);
            Runnable action = e.getClickedInventory() == top ? chooser.getAction(e.getSlot()) : null;
            if (action != null) {
                // Opening another inventory has to wait until the click is over
                Bukkit.getScheduler().runTask(plugin, action);
            }
            return;
        }
        if (!(top.getHolder(false) instanceof DynamicToolsGui gui) || !(e.getWhoClicked() instanceof Player p)) {
            return;
        }

        // Double clicking gathers matching items from every slot, including the decorations and results
        if (e.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
            e.setCancelled(true);
            return;
        }

        Inventory clicked = e.getClickedInventory();
        if (clicked == null) {
            return;
        }

        if (clicked != top) {
            // Shift clicking would put the item in the first open slot, so send it to the right input slot instead
            if (e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                e.setCancelled(true);
                ItemStack item = e.getCurrentItem();
                if (item != null && !item.isEmpty()) {
                    e.setCurrentItem(gui.insertFromPlayer(item));
                    gui.update();
                }
            }
            return;
        }

        if (gui.isInputSlot(e.getSlot())) {
            // Let the click happen, then update the outputs once the input has changed
            scheduleUpdate(gui);
            return;
        }

        e.setCancelled(true);
        if (gui instanceof PartAssembler assembler && e.getSlot() == assembler.getResultSlot()) {
            takeAssemblerResult(e, p, assembler);
        } else if (gui instanceof PartCrafter crafter) {
            takeCrafterResult(e, p, crafter);
        }
    }

    private void takeAssemblerResult(InventoryClickEvent e, Player p, PartAssembler assembler) {
        Tool tool = assembler.getResultTool();
        if (tool == null) {
            return;
        }
        if (!giveResult(e, p, tool.getItemStack())) {
            return;
        }
        assembler.consumeInputs();
        assembler.update();
        plugin.debug(p.getName() + " assembled a " + tool.generateDisplayNameString());
    }

    private void takeCrafterResult(InventoryClickEvent e, Player p, PartCrafter crafter) {
        PartType partType = crafter.getPartAt(e.getSlot());
        if (partType == null) {
            return;
        }
        ItemStack part = crafter.createPart(partType);
        if (part == null) {
            return;
        }
        if (!giveResult(e, p, part)) {
            return;
        }
        crafter.consumeInput(partType);
        crafter.update();
        plugin.debug(p.getName() + " crafted a " + partType.name() + " part");
    }

    /***
     * Puts a crafted item on the player's cursor, or in their inventory when shift clicking.
     * @return true if the player received the item.
     */
    private boolean giveResult(InventoryClickEvent e, Player p, ItemStack result) {
        switch (e.getClick()) {
            case SHIFT_LEFT, SHIFT_RIGHT -> {
                ItemManager.addOrDropItem(result, p);
                return true;
            }
            case LEFT, RIGHT -> {
                ItemStack cursor = e.getView().getCursor();
                if (cursor.isEmpty()) {
                    e.getView().setCursor(result);
                    return true;
                }
                if (cursor.isSimilar(result) && cursor.getAmount() + result.getAmount() <= cursor.getMaxStackSize()) {
                    cursor.setAmount(cursor.getAmount() + result.getAmount());
                    e.getView().setCursor(cursor);
                    return true;
                }
                return false;
            }
            default -> {
                return false;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent e) {
        Inventory top = e.getView().getTopInventory();
        if (top.getHolder(false) instanceof WorkstationChooser) {
            e.setCancelled(true);
            return;
        }
        if (!(top.getHolder(false) instanceof DynamicToolsGui gui)) {
            return;
        }

        boolean touchesGui = false;
        for (int rawSlot : e.getRawSlots()) {
            if (rawSlot >= top.getSize()) {
                continue;
            }
            if (!gui.isInputSlot(rawSlot)) {
                e.setCancelled(true);
                return;
            }
            touchesGui = true;
        }
        if (touchesGui) {
            scheduleUpdate(gui);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder(false) instanceof DynamicToolsGui gui && e.getPlayer() instanceof Player p) {
            gui.returnInputs(p);
        }
    }

    private void scheduleUpdate(DynamicToolsGui gui) {
        Bukkit.getScheduler().runTask(plugin, gui::update);
    }
}
