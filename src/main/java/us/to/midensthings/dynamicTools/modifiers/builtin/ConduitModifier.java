package us.to.midensthings.dynamicTools.modifiers.builtin;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import us.to.midensthings.dynamicTools.modifiers.StatModifier;
import us.to.midensthings.dynamicTools.tools.ToolStats;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.Set;

/***
 * Lowers the underwater mining penalty by 20% per level, removing it at level 5.
 * Players mine at 20% speed underwater (submerged mining speed 0.2), so each level adds 20% of the missing 0.8,
 * the same as Aqua Affinity at level 5.
 */
public class ConduitModifier implements StatModifier {
    private static final double PENALTY = 0.8;

    @Override
    public void apply(ToolStats stats, int level) {
        double reduction = Math.min(1.0, 0.2 * level) * PENALTY;
        stats.addAttribute(Attribute.SUBMERGED_MINING_SPEED, reduction, AttributeModifier.Operation.ADD_NUMBER);
    }

    @Override
    public String getId() {
        return "conduit";
    }

    @Override
    public String getDisplayName() {
        return "<#2ec4b6>Conduit";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.onlyTools;
    }
}
