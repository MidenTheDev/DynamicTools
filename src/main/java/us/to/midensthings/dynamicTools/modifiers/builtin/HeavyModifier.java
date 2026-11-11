package us.to.midensthings.dynamicTools.modifiers.builtin;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import us.to.midensthings.dynamicTools.modifiers.StatModifier;
import us.to.midensthings.dynamicTools.tools.ToolStats;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.Set;

/***
 * Increases knockback resistance with diminishing returns: each level adds half as much as the one before.
 * Heavy III gives 0.25 per piece, so a full set of four is needed for complete knockback resistance,
 * and no level gives more than 2/7 (about 0.286) per piece, so three pieces can never reach it.
 */
public class HeavyModifier implements StatModifier {
    @Override
    public void apply(ToolStats stats, int level) {
        double resistance = (2.0 / 7.0) * (1 - Math.pow(0.5, level));
        // Rounded so Heavy III is exactly 0.25 and four pieces add up to exactly 1.0
        stats.addAttribute(Attribute.KNOCKBACK_RESISTANCE, Math.round(resistance * 1000) / 1000.0, AttributeModifier.Operation.ADD_NUMBER);
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
