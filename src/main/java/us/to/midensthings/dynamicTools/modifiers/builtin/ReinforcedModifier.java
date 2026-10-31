package us.to.midensthings.dynamicTools.modifiers.builtin;

import us.to.midensthings.dynamicTools.modifiers.StatModifier;
import us.to.midensthings.dynamicTools.tools.ToolStats;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.EnumSet;
import java.util.Set;

/***
 * Increases the tool's durability by 25% per level.
 */
public class ReinforcedModifier implements StatModifier {

    @Override
    public String getId() {
        return "reinforced";
    }

    @Override
    public String getDisplayName() {
        return "<#9fb4c7>Reinforced";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.all;
    }

    @Override
    public void apply(ToolStats stats, int level) {
        stats.setDurability(stats.getDurability() * (1 + 0.25 * level));
    }
}
