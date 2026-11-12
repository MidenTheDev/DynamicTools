package us.to.midensthings.dynamicTools.compat;

import com.github.darksoulq.relique.api.RelicAPI;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;

import java.util.ArrayList;
import java.util.List;

/***
 * Optional support for Relique, which adds Curios-like accessory slots. Only use this class after checking
 * {@link DynamicTools#isReliqueEnabled()}, as it can't load without Relique installed.
 */
public final class ReliqueCompat {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    private ReliqueCompat() {
    }

    /***
     * @return copies of the relics the entity has equipped in every Relique slot, or an empty list if Relique couldn't
     * read them (e.g. a Relique version with a different API)
     */
    public static List<ItemStack> getEquippedRelics(LivingEntity entity) {
        try {
            List<RelicAPI.SlotResult> equipped = RelicAPI.getAllEquipped(entity);
            List<ItemStack> relics = new ArrayList<>(equipped.size());
            for (RelicAPI.SlotResult result : equipped) {
                relics.add(result.item());
            }
            return relics;
        } catch (RuntimeException | LinkageError e) {
            plugin.debug("Couldn't read the relics of " + entity.getName() + " from Relique: " + e);
            return List.of();
        }
    }
}
