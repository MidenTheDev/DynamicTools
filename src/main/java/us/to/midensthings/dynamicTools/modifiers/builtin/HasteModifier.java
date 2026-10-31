package us.to.midensthings.dynamicTools.modifiers.builtin;

import us.to.midensthings.dynamicTools.modifiers.StatModifier;
import us.to.midensthings.dynamicTools.tools.ToolStats;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.Set;

/***
 * Increases the tool's digSpeed by 0.35 per level
 */
public class HasteModifier implements StatModifier {
    @Override
    public void apply(ToolStats stats, int level) {
        stats.setDigSpeed(stats.getDigSpeed() + (0.35*level));
    }

    @Override
    public String getId() {
        return "haste";
    }

    @Override
    public String getDisplayName() {
        return "<#ffa348>Haste";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.onlyTools;
    }
}
