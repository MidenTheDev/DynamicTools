package us.to.midensthings.dynamicTools.modifiers.builtin;

import us.to.midensthings.dynamicTools.modifiers.Modifier;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.Set;

/***
 * Raises the tool's mining level by 1, up to diamond (4), whichever part the material is used for.
 * Tools that are already at diamond or higher keep their mining level.
 */
public class DiamondTippedModifier implements Modifier {
    private static final int DIAMOND_MINING_LEVEL = 4;

    @Override
    public int modifyMiningLevel(int miningLevel, int level) {
        if (miningLevel >= DIAMOND_MINING_LEVEL) {
            return miningLevel;
        }
        return Math.min(miningLevel + 1, DIAMOND_MINING_LEVEL);
    }

    @Override
    public String getId() {
        return "diamond_tipped";
    }

    @Override
    public String getDisplayName() {
        return "<#4aedd9>Diamond Tipped";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.noArmor;
    }
}
