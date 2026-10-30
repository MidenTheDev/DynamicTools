package us.to.midensthings.dynamicTools.modifiers.builtin;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.to.midensthings.dynamicTools.modifiers.EventModifier;
import us.to.midensthings.dynamicTools.tools.ToolType;

import java.util.EnumSet;
import java.util.Set;

/***
 * Slows down enemies hit with the tool. Each level adds 2 seconds and one level of Slowness.
 */
public class FrigidModifier implements EventModifier {

    @Override
    public String getId() {
        return "frigid";
    }

    @Override
    public String getDisplayName() {
        return "Frigid";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return EnumSet.of(ToolType.SWORD, ToolType.AXE, ToolType.MACE, ToolType.SPEAR);
    }

    @EventHandler(ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof LivingEntity attacker) || !(e.getEntity() instanceof LivingEntity victim)) {
            return;
        }
        int level = getLevel(attacker);
        if (level <= 0) {
            return;
        }
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40 * level, level - 1));
    }
}
