package us.to.midensthings.dynamicTools.modifiers;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import us.to.midensthings.dynamicTools.DynamicTools;

/***
 * A modifier that reacts to events. Its @EventHandler methods are registered when the modifier is registered,
 * and should use getLevel to check if the entity involved has the modifier.
 */
public interface EventModifier extends Modifier, Listener {

    /***
     * @return the slots a tool has to be in for this modifier to be active.
     */
    default EquipmentSlotGroup getActiveSlot() {
        return EquipmentSlotGroup.MAINHAND;
    }

    /***
     * @return the total level of this modifier on the items the entity has in the active slots, or 0 if it doesn't have it.
     */
    default int getLevel(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment == null) {
            return 0;
        }

        ModifierManager modifierManager = DynamicTools.getPlugin(DynamicTools.class).getModifierManager();
        int level = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (getActiveSlot().test(slot) && entity.canUseEquipmentSlot(slot)) {
                level += modifierManager.getLevel(equipment.getItem(slot), this);
            }
        }
        return level;
    }
}
