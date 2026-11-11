package us.to.midensthings.dynamicTools.modifiers;

import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.tools.ToolType;

import java.util.EnumSet;
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
     * @return the name shown in the tool's lore, in MiniMessage format (e.g. "<aqua>Frigid"). It's gray like enchantments
     * unless it sets its own color. The level numeral is added to the end and uses the same formatting as the end of the name,
     * so leave tags unclosed for the numeral to match.
     */
    String getDisplayName();

    /***
     * @return the tool types this modifier can be applied to. Tools of any other type don't get the modifier, even if their materials have it.
     */
    Set<ToolType> getValidToolTypes();

    /***
     * @return the parts a material has to be used in for its tool to get this modifier, e.g. only PRIMARY for a modifier
     * that only works on tool heads. A material used in more than one part only needs one of them to be valid. Defaults to every part.
     */
    default Set<PartSlot> getValidPartSlots() {
        return EnumSet.allOf(PartSlot.class);
    }

    /***
     * @return true if a material in this part of a tool of this type gives its tool this modifier
     */
    default boolean appliesTo(ToolType toolType, PartSlot partSlot) {
        return getValidToolTypes().contains(toolType) && getValidPartSlots().contains(partSlot);
    }

    /***
     * Changes the tool's mining level, which decides the base vanilla item and which blocks it can mine.
     * Called before the tool's item is made, starting from the head's mining level.
     * @param miningLevel the mining level so far (1 wood, 2 stone, 3 iron, 4 diamond)
     * @param level the modifier's total level on the tool
     * @return the new mining level
     */
    default int modifyMiningLevel(int miningLevel, int level) {
        return miningLevel;
    }

    /***
     * Changes the tool's item directly, for anything that isn't a stat (e.g. data components).
     * Called once when the tool is built, after everything else has been set.
     * @param item the tool's item
     * @param level the modifier's total level on the tool
     */
    default void applyToItem(ItemStack item, int level) {
    }
}
