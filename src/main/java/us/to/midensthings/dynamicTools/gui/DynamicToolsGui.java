package us.to.midensthings.dynamicTools.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

/***
 * A DynamicTools GUI. Players can only place items into the input slots, every other slot is controlled by the GUI.
 */
public interface DynamicToolsGui extends InventoryHolder {

    /***
     * @return true if players can freely place and take items in this slot.
     */
    boolean isInputSlot(int slot);

    /***
     * Recalculates the GUI's outputs from the current inputs.
     */
    void update();

    /***
     * Moves an item shift-clicked from the player's inventory into the matching input slot.
     * @return what is left of the item after moving it, or null if all of it was moved.
     */
    ItemStack insertFromPlayer(ItemStack item);

    /***
     * Gives the items in the input slots back to the player and clears the slots.
     */
    void returnInputs(Player player);

    default void open(Player player) {
        player.openInventory(getInventory());
    }
}
