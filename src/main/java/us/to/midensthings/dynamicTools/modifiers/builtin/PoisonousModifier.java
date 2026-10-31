package us.to.midensthings.dynamicTools.modifiers.builtin;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.to.midensthings.dynamicTools.modifiers.EventModifier;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.Set;

/***
 * Applies poison to hit enemies. Levels above 1 increase duration and amplifier (max 2)
 */
public class PoisonousModifier implements EventModifier {
    @Override
    public String getId() {
        return "poisonous";
    }

    @Override
    public String getDisplayName() {
        return "<#2a502a>Poisonous";
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
        victim.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 60+(20 * level), amplifier));
    }
}
