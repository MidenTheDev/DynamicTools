package us.to.midensthings.dynamicTools.modifiers.builtin;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.to.midensthings.dynamicTools.modifiers.EventModifier;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.EnumSet;
import java.util.Set;

/***
 * Applies the slowness effect to enemies hit. Levels above 1 increase duration and amplifier (max 2)
 */
public class FrigidModifier implements EventModifier {

    @Override
    public String getId() {
        return "frigid";
    }

    @Override
    public String getDisplayName() {
        return "<aqua>Frigid";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.onlyWeapons;
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
        int amplifier = 0;
        if (level > 1) amplifier = 1;
        victim.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, 40 * level, amplifier));
    }
}
