package us.to.midensthings.dynamicTools.modifiers.builtin;

import us.to.midensthings.dynamicTools.modifiers.StatModifier;
import us.to.midensthings.dynamicTools.tools.ToolStats;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.Set;

/***
 * Increases the tool's attack damage by 1 per level
 */
public class SharpModifier implements StatModifier {
    @Override
    public void apply(ToolStats stats, int level) {
        stats.setAttackDamage(stats.getAttackDamage() + (1.0*level));
    }

    @Override
    public String getId() {
        return "sharp";
    }

    @Override
    public String getDisplayName() {
        return "<gray>Sharp";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.onlyWeapons;
    }
}
