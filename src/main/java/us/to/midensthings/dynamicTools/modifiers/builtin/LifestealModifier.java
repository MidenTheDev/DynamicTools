package us.to.midensthings.dynamicTools.modifiers.builtin;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.to.midensthings.dynamicTools.modifiers.EventModifier;
import us.to.midensthings.dynamicTools.tools.ToolType;

import java.util.Set;

/***
 * Heals the attacker for 8% of damage dealt per level
 */
public class LifestealModifier implements EventModifier {
    @Override
    public String getId() {
        return "lifesteal";
    }

    @Override
    public String getDisplayName() {
        return "<#443960>Lifesteal";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return Set.of();
    }

    @EventHandler(ignoreCancelled = true)
    public void onUse(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof LivingEntity attacker) || !(e.getEntity() instanceof LivingEntity)) {
            return;
        }
        int level = getLevel(attacker);
        if (level <= 0) {
            return;
        }
        double healing = e.getDamage() *(0.08*level);
        attacker.setHealth(attacker.getHealth()+healing);

    }
}
