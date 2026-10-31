package us.to.midensthings.dynamicTools.modifiers.builtin;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Registry;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import us.to.midensthings.dynamicTools.modifiers.EventModifier;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/***
 * Apllies a random potion effect on hit. Amplifier & duration scale with level (max amplifier 2).
 * Uses any potion effects registered, not just the original enum.
 */
public class SusModifier implements EventModifier {
    @Override
    public String getId() {
        return "sus";
    }

    @Override
    public String getDisplayName() {
        return "<red>Sus";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.onlyWeapons;
    }

    private List<PotionEffectType> effects;

    private List<PotionEffectType> getEffects() {
        if (effects == null) {
            effects = List.copyOf(RegistryAccess.registryAccess().getRegistry(RegistryKey.MOB_EFFECT).stream().toList());
        }
        return effects;
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

        List<PotionEffectType> effects = getEffects();
        PotionEffectType effect = effects.get(ThreadLocalRandom.current().nextInt(effects.size()));
        victim.addPotionEffect(new PotionEffect(effect, 40 * level, amplifier));

    }
}
