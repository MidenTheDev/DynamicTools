package us.to.midensthings.dynamicTools.gui;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

/***
 * Lets a player choose between a vanilla workstation's GUI and the DynamicTools GUI that shares the block.
 */
public class WorkstationChooser implements InventoryHolder {
    private final Inventory inv;

    private final int vanillaSlot = 11;
    private final int customSlot = 15;

    private final Runnable openVanilla;
    private final Runnable openCustom;

    /***
     * @param filler the item to fill the other slots with, or null to leave them empty
     * @param openVanilla opens the vanilla GUI
     * @param openCustom opens the DynamicTools GUI
     */
    public WorkstationChooser(Material vanillaIcon, String vanillaName, Material customIcon, String customName,
                              ItemStack filler, Runnable openVanilla, Runnable openCustom) {
        this.openVanilla = openVanilla;
        this.openCustom = openCustom;

        inv = Bukkit.createInventory(this, 27, Component.text("Choose a Workstation"));
        if (filler != null) {
            for (int slot = 0; slot < inv.getSize(); slot++) {
                inv.setItem(slot, filler);
            }
        }
        inv.setItem(vanillaSlot, button(vanillaIcon, vanillaName));
        inv.setItem(customSlot, button(customIcon, customName));
    }

    private static ItemStack button(Material icon, String name) {
        ItemStack item = ItemStack.of(icon);
        item.setData(DataComponentTypes.ITEM_NAME, Component.text(name, NamedTextColor.WHITE));
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay()
                .addHiddenComponents(DataComponentTypes.ATTRIBUTE_MODIFIERS)
                .build());
        return item;
    }

    /***
     * @return what clicking the slot should open, or null if the slot isn't a button.
     */
    public Runnable getAction(int slot) {
        if (slot == vanillaSlot) {
            return openVanilla;
        }
        if (slot == customSlot) {
            return openCustom;
        }
        return null;
    }

    public void open(Player player) {
        player.openInventory(inv);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inv;
    }
}
