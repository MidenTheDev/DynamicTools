package us.to.midensthings.dynamicTools.modifiers.builtin;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import us.to.midensthings.dynamicTools.modifiers.StatModifier;
import us.to.midensthings.dynamicTools.tools.ToolStats;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.Set;

/***
 * Increases knockback resistance by 0.5 for each level
 */
public class HeavyModifier implements StatModifier {
    @Override
    public void apply(ToolStats stats, int level) {
        stats.addAttribute(Attribute.KNOCKBACK_RESISTANCE,0.5*level, AttributeModifier.Operation.ADD_NUMBER);
    }

    @Override
    public String getId() {
        return "heavy";
    }

    @Override
    public String getDisplayName() {
        return "<dark_gray>Heavy";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.onlyArmor;
    }
}
