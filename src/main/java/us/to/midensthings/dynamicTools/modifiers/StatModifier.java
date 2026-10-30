package us.to.midensthings.dynamicTools.modifiers;

import us.to.midensthings.dynamicTools.tools.ToolStats;

/***
 * A modifier that changes a tool's stats (attack damage, attack speed, durability, dig speed or any other attribute).
 */
public interface StatModifier extends Modifier {

    /***
     * Changes the tool's stats. Called once when the tool is built, after the stats from its materials have been calculated.
     * @param stats the tool's stats, change these to modify the tool
     * @param level the modifier's total level on the tool
     */
    void apply(ToolStats stats, int level);
}
