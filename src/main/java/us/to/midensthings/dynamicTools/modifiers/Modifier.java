package us.to.midensthings.dynamicTools.modifiers;

import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.tools.ToolType;

import java.util.Set;

/***
 * A modifier a tool gets from its materials. Implement StatModifier to change the tool's stats,
 * or EventModifier to react to events while the tool is equipped. Modifiers that only change the item itself
 * can implement this interface directly and override applyToItem.
 */
public interface Modifier {

    /***
     * @return the unique ID used in materials.yml and stored on tools. Only lowercase letters, numbers, _ - . and / are allowed.
     */
    String getId();

    /***
     * @return the name shown in the tool's lore (MiniMessage). It's gray like enchantments unless it sets its own color.
     */
    String getDisplayName();

    /***
     * @return the tool types this modifier can be applied to. Tools of any other type don't get the modifier, even if their materials have it.
     */
    Set<ToolType> getValidToolTypes();

    /***
     * Changes the tool's item directly, for anything that isn't a stat (e.g. data components).
     * Called once when the tool is built, after everything else has been set.
     * @param item the tool's item
     * @param level the modifier's total level on the tool
     */
    default void applyToItem(ItemStack item, int level) {
    }
}
