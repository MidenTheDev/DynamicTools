package us.to.midensthings.dynamicTools.compat;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.RecipeChoice;
import org.bukkit.inventory.ShapedRecipe;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.util.ResourceInstaller;

import java.nio.file.Path;
import java.util.List;

/***
 * The custom blocks DynamicTools can add to the custom item plugin (the Part Crafter and Part Assembler blocks),
 * turned on with the custom-block options in config.yml. DynamicTools only installs their configs and textures,
 * the custom item plugin makes the block models and resource pack.
 */
public final class CustomBlocks {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    /***
     * @param id the block's ID in the dynamictools namespace
     * @param configSection the config.yml section with its custom-block option
     * @param configFile the block's config file name in ItemsAdderConfigs/ and CraftEngineConfigs/
     * @param textures the block's textures in textures/block/, without .png
     * @param recipeTop the item on top of the planks in its crafting recipe
     */
    public record CustomBlock(String id, String configSection, String configFile, List<String> textures, Material recipeTop) {

        public boolean isEnabled() {
            return plugin.getConfig().getBoolean(configSection + ".custom-block", false);
        }
    }

    public static final CustomBlock PART_CRAFTER = new CustomBlock("part_crafter", "part-crafter", "dynamicToolsPartCrafter.yml",
            List.of("part_crafter_bottom", "part_crafter_front", "part_crafter_side", "part_crafter_top"), Material.STICK);
    public static final CustomBlock PART_ASSEMBLER = new CustomBlock("part_assembler", "assembler", "dynamicToolsPartAssembler.yml",
            List.of("part_assembler_bottom", "part_assembler_front", "part_assembler_side", "part_assembler_top"), Material.COPPER_INGOT);

    public static final List<CustomBlock> ALL = List.of(PART_CRAFTER, PART_ASSEMBLER);

    private CustomBlocks() {
    }

    /***
     * Installs the configs and textures of the enabled blocks. Configs that are still the empty placeholders from older
     * versions are replaced, otherwise existing files are kept so edits made by server owners stay.
     * @param resourceFolder the folder in the jar the block configs are in, e.g. ItemsAdderConfigs/
     * @param configsFolder where the custom item plugin reads configs from
     * @param assetsFolder the assets/dynamictools folder of the custom item plugin's resource pack
     * @return true if any files were added
     */
    public static boolean install(String resourceFolder, Path configsFolder, Path assetsFolder) {
        boolean added = false;
        for (CustomBlock block : ALL) {
            if (!block.isEnabled()) {
                continue;
            }
            added |= ResourceInstaller.copyResourceReplacingPlaceholder(resourceFolder + block.configFile(), configsFolder.resolve(block.configFile()));
            for (String texture : block.textures()) {
                added |= ResourceInstaller.copyResourceIfAbsent("textures/block/" + texture + ".png",
                        assetsFolder.resolve("textures").resolve("block").resolve(texture + ".png"));
            }
        }
        return added;
    }

    /***
     * @return the block's crafting recipe: two of its recipe top item over four planks of any wood
     * (sticks for the Part Crafter, copper ingots for the Part Assembler).
     * The same recipes are in the CraftEngine block configs, which support tags themselves.
     * @param result the block's item
     */
    public static ShapedRecipe createRecipe(CustomBlock block, ItemStack result) {
        ShapedRecipe recipe = new ShapedRecipe(new NamespacedKey(plugin, block.id()), result);
        recipe.shape("TT", "PP", "PP");
        recipe.setIngredient('T', block.recipeTop());
        recipe.setIngredient('P', new RecipeChoice.MaterialChoice(Tag.PLANKS));
        return recipe;
    }
}
