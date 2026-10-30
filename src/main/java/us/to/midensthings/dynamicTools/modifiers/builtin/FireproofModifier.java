package us.to.midensthings.dynamicTools.modifiers.builtin;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.DamageResistant;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.tags.DamageTypeTagKeys;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.modifiers.Modifier;
import us.to.midensthings.dynamicTools.tools.ToolType;

import java.util.EnumSet;
import java.util.Set;

/***
 * Makes the tool immune to fire and lava when dropped, like netherite tools. The level has no effect.
 */
public class FireproofModifier implements Modifier {

    @Override
    public String getId() {
        return "fireproof";
    }

    @Override
    public String getDisplayName() {
        return "Fireproof";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return EnumSet.allOf(ToolType.class);
    }

    @Override
    public void applyToItem(ItemStack item, int level) {
        // Same component vanilla netherite items use
        item.setData(DataComponentTypes.DAMAGE_RESISTANT, DamageResistant.damageResistant(
                RegistryAccess.registryAccess().getRegistry(RegistryKey.DAMAGE_TYPE).getTag(DamageTypeTagKeys.IS_FIRE)));
    }
}
