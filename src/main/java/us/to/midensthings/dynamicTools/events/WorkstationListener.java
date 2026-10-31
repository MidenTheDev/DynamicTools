package us.to.midensthings.dynamicTools.events;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.MenuType;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.gui.GuiItems;
import us.to.midensthings.dynamicTools.gui.PartAssembler;
import us.to.midensthings.dynamicTools.gui.PartCrafter;
import us.to.midensthings.dynamicTools.gui.WorkstationChooser;

/***
 * Opens the Part Crafter from smithing tables and the Part Assembler from anvils, depending on the config.
 * With replace, the DynamicTools GUI opens instead of the vanilla one. With share, the player chooses between them.
 */
public class WorkstationListener implements Listener {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK || e.getHand() != EquipmentSlot.HAND
                || e.useInteractedBlock() == Event.Result.DENY) {
            return;
        }
        Block block = e.getClickedBlock();
        if (block == null) {
            return;
        }
        Player p = e.getPlayer();
        // Sneaking with an item places the item instead of opening the block, same as vanilla
        if (p.isSneaking() && (!p.getInventory().getItemInMainHand().isEmpty() || !p.getInventory().getItemInOffHand().isEmpty())) {
            return;
        }

        FileConfiguration config = plugin.getConfig();
        Material type = block.getType();
        if (type == Material.SMITHING_TABLE) {
            boolean replace = config.getBoolean("part-crafter.replace-smithing-table", false);
            boolean share = config.getBoolean("part-crafter.share-smithing-table", true);
            if (!replace && !share) {
                return;
            }
            e.setCancelled(true);
            Runnable openPartCrafter = () -> new PartCrafter(p).open(p);
            if (replace) {
                plugin.debug(p.getName() + " opened the Part Crafter from a smithing table");
                openPartCrafter.run();
                return;
            }
            new WorkstationChooser(Material.SMITHING_TABLE, "Smithing Table", Material.IRON_INGOT, "Part Crafter",
                    getFiller("part-crafter"),
                    () -> MenuType.SMITHING.builder().location(block.getLocation()).checkReachable(true).build(p).open(),
                    openPartCrafter).open(p);
        } else if (Tag.ANVIL.isTagged(type)) {
            boolean replace = config.getBoolean("assembler.replace-anvil", false);
            boolean share = config.getBoolean("assembler.share-anvil", true);
            if (!replace && !share) {
                return;
            }
            e.setCancelled(true);
            Runnable openAssembler = () -> new PartAssembler(p).open(p);
            if (replace) {
                plugin.debug(p.getName() + " opened the Part Assembler from an anvil");
                openAssembler.run();
                return;
            }
            new WorkstationChooser(type, "Anvil", Material.IRON_PICKAXE, "Part Assembler",
                    getFiller("assembler"),
                    () -> MenuType.ANVIL.builder().location(block.getLocation()).checkReachable(true).build(p).open(),
                    openAssembler).open(p);
        }
    }

    /***
     * @return the fill item of the GUI's config section, or null if it doesn't fill empty slots
     */
    private org.bukkit.inventory.ItemStack getFiller(String section) {
        if (!plugin.getConfig().getBoolean(section + ".gui.fill-empty-slots", true)) {
            return null;
        }
        return GuiItems.createDecoration(plugin.getConfig().getString(section + ".gui.fill-material"), Material.GRAY_STAINED_GLASS_PANE);
    }
}
