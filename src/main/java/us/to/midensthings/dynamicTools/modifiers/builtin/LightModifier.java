package us.to.midensthings.dynamicTools.modifiers.builtin;

import us.to.midensthings.dynamicTools.modifiers.PartSlot;
import us.to.midensthings.dynamicTools.modifiers.StatModifier;
import us.to.midensthings.dynamicTools.tools.ToolStats;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.EnumSet;
import java.util.Set;

/***
 * Increases a weapon's attack speed by 10% per level. Only applies when the material is used for the head.
 */
public class LightModifier implements StatModifier {
    @Override
    public void apply(ToolStats stats, int level) {
        stats.setAttackSpeed(stats.getAttackSpeed() * (1 + (0.1 * level)));
    }

    @Override
    public String getId() {
        return "light";
    }

    @Override
    public String getDisplayName() {
        return "<#f6f5f4>Light";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.onlyWeapons;
    }

    @Override
    public Set<PartSlot> getValidPartSlots() {
        return EnumSet.of(PartSlot.PRIMARY);
    }
}
