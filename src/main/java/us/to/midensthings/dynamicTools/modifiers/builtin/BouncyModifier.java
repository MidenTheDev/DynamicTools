package us.to.midensthings.dynamicTools.modifiers.builtin;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import us.to.midensthings.dynamicTools.modifiers.StatModifier;
import us.to.midensthings.dynamicTools.tools.ToolStats;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.Set;

/**
 * Increases the tool's knockback stat by 0.5 per level
 */
public class BouncyModifier implements StatModifier {
    @Override
    public void apply(ToolStats stats, int level) {
        stats.addAttribute(Attribute.ATTACK_KNOCKBACK,0.5*level, AttributeModifier.Operation.ADD_NUMBER);
    }

    @Override
    public String getId() {
        return "bouncy";
    }

    @Override
    public String getDisplayName() {
        return "<#57e389>Bouncy";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.onlyWeapons;
    }
}
