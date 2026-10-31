package us.to.midensthings.dynamicTools.resourcepack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
import io.papermc.paper.datacomponent.item.Equippable;
import net.kyori.adventure.key.Key;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.parts.PartType;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;
import us.to.midensthings.dynamicTools.tools.ToolType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.stream.Stream;

/***
 * Generates the textures, models and item model definitions for parts and tools into assets/dynamictools.
 * <pre>
 * textures/item/part/&lt;material&gt;/&lt;texture&gt;.png             part icons (base_&lt;part&gt;.png) recolored with the material's colors
 * textures/item/tool/&lt;tool&gt;/&lt;material&gt;/&lt;layer&gt;.png        tool layers (tool/&lt;tool&gt;/&lt;layer&gt;.png) recolored the same way
 * textures/item/armor/&lt;armor&gt;/&lt;material&gt;/&lt;layer&gt;.png      armor icon core and binding layers (armor/&lt;armor&gt;/core.png
 *                                                              and binding.png) recolored the same way, the frame layer is the frame's part icon
 * models/item/part/&lt;material&gt;/&lt;part&gt;.json                  one model per part of each material
 * models/item/tool/&lt;tool&gt;/&lt;head&gt;/&lt;handle&gt;/&lt;binding&gt;.json  the tool layers stacked into a finished tool
 * models/item/armor/&lt;armor&gt;/&lt;frame&gt;/&lt;core&gt;/&lt;binding&gt;.json the armor icon layers stacked into a finished piece
 * items/part, items/tool, items/armor                          pick the model from the item's custom_model_data string
 * textures/entity/equipment/&lt;layer type&gt;/&lt;material&gt;/&lt;layer&gt;.png  worn armor layers (equipment/&lt;layer type&gt;/&lt;layer&gt;.png) recolored
 * equipment/armor/&lt;frame&gt;/&lt;core&gt;/&lt;binding&gt;.json             the worn layers stacked core, frame, binding
 * </pre>
 * Parts and tools of materials without colors use the uncolored textures in the base folders.
 * Tool types without all three layers aren't generated and keep their vanilla look, and worn armor keeps its vanilla look
 * unless both humanoid and humanoid_leggings have all three layers.
 */
public class ResourcePackGenerator {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    public static final String NAMESPACE = "dynamictools";

    // Everything in these folders is generated, so files in them that aren't generated any more get deleted
    private static final List<String> GENERATED_FOLDERS = List.of(
            "textures/item/part", "textures/item/tool", "textures/item/armor",
            "models/item/part", "models/item/tool", "models/item/armor",
            "items/part", "items/tool", "items/armor",
            "equipment/armor", "textures/entity/equipment/humanoid", "textures/entity/equipment/humanoid_leggings");

    // The equipment layer types worn armor is drawn with: humanoid for helmets, chestplates and boots, humanoid_leggings for leggings
    private static final List<String> EQUIPMENT_LAYER_TYPES = List.of("humanoid", "humanoid_leggings");

    /***
     * The grayscale layers of a finished tool or piece of armor, positioned where each part sits on it.
     * For tools these are the head, handle and binding, for armor the frame, core and binding.
     */
    private record ToolLayers(BufferedImage primary, BufferedImage secondary, BufferedImage tertiary) {}

    private static final Gson GSON = new Gson();
    // Part item icons, from textures/item/base_<part>.png
    private static final Map<PartType, BufferedImage> BASE_TEXTURES = loadBaseTextures();
    // Finished tool layers, from textures/item/tool/<tool>/head.png, handle.png and binding.png,
    // or textures/item/armor/<armor>/frame.png, core.png and binding.png for armor
    private static final Map<ToolType, ToolLayers> TOOL_LAYERS = loadToolLayers();
    // Worn armor layers, from textures/equipment/<layer type>/frame.png, core.png and binding.png
    private static final Map<String, ToolLayers> EQUIPMENT_LAYERS = loadEquipmentLayers();

    // Path relative to assets/dynamictools -> file contents
    private final Map<String, byte[]> files = new TreeMap<>();

    /***
     * Generates every file from the registered materials and writes the ones that changed.
     * @param assetsFolder the assets/dynamictools folder of the resource pack
     * @return the number of files written or deleted
     */
    public int generate(Path assetsFolder) throws IOException {
        return generate(assetsFolder, plugin.getToolMaterialRegistry().getMaterials(), false);
    }

    /***
     * Generates every file and writes them. Safe to run off the main thread, as long as the materials list is a copy.
     * @param assetsFolder the assets/dynamictools folder of the resource pack
     * @param toolMaterials the materials to generate textures for
     * @param force write every file, even the ones that haven't changed
     * @return the number of files written or deleted
     */
    public int generate(Path assetsFolder, Collection<ToolMaterial> toolMaterials, boolean force) throws IOException {
        files.clear();

        List<ToolMaterial> materials = new ArrayList<>();
        for (ToolMaterial material : toolMaterials) {
            if (material.getColors() == null) {
                continue;
            }
            if (!hasValidId(material)) {
                plugin.getLogger().warning("Material " + material.getId() + " can't have textures, its ID can only use lowercase letters, numbers, _ - and . and can't be \"base\"");
                continue;
            }
            materials.add(material);
        }
        materials.sort(Comparator.comparing(ToolMaterial::getId));

        for (Map.Entry<PartType, BufferedImage> entry : BASE_TEXTURES.entrySet()) {
            generatePart(entry.getKey(), entry.getValue(), materials);
        }
        for (ToolType toolType : ToolType.values()) {
            if (hasTextures(toolType)) {
                generateTool(toolType, materials);
            }
        }
        if (hasEquipmentTextures()) {
            generateEquipment(materials);
        }

        return write(assetsFolder, force);
    }

    private void generatePart(PartType partType, BufferedImage baseTexture, List<ToolMaterial> materials) throws IOException {
        String part = partType.name().toLowerCase(Locale.ROOT);
        String texture = getTextureName(partType);

        addTexture("part/base/" + texture, baseTexture);
        String baseModel = addModel("part/base/" + part, "minecraft:item/generated", "part/base/" + texture);

        JsonArray cases = new JsonArray();
        for (ToolMaterial material : materials) {
            if (!material.getValidParts().contains(partType)) {
                continue;
            }
            String texturePath = "part/" + material.getId() + "/" + texture;
            // Armor frame icons (also the frame layer of the armor icons) are only the material's dark color
            float[] range = TextureRecolorer.luminanceRange(baseTexture);
            addTexture(texturePath, TextureRecolorer.recolor(baseTexture, material.getColors(), range[0], range[1],
                    isArmorFrame(partType) ? TextureRecolorer.ColorRange.DARK_ONLY : TextureRecolorer.ColorRange.FULL));
            String model = addModel("part/" + material.getId() + "/" + part, "minecraft:item/generated", texturePath);
            cases.add(selectCase(material.getId(), model));
        }
        addItemDefinition("part/" + part, cases, baseModel);
    }

    private void generateTool(ToolType toolType, List<ToolMaterial> materials) throws IOException {
        String tool = getItemFolder(toolType) + "/" + toolType.name().toLowerCase(Locale.ROOT);
        String[] layerNames = getLayerNames(toolType);
        ToolLayers layers = TOOL_LAYERS.get(toolType);
        // Tools are held at an angle, armor icons are flat like vanilla armor
        String parent = toolType.isArmor() ? "minecraft:item/generated" : "minecraft:item/handheld";

        // Recolor each layer for every material that can make that part.
        // Armor icons use the frame's part icon as the frame layer, which generatePart already recolored.
        if (!toolType.isArmor()) {
            addTexture(tool + "/base/" + layerNames[0], layers.primary());
            for (ToolMaterial material : materialsFor(materials, toolType.getPrimaryPart())) {
                addTexture(tool + "/" + material.getId() + "/" + layerNames[0], TextureRecolorer.recolor(layers.primary(), material.getColors()));
            }
        }
        addTexture(tool + "/base/" + layerNames[1], layers.secondary());
        addTexture(tool + "/base/" + layerNames[2], layers.tertiary());
        // Tool layers are each stretched over the material's colors. Armor layers share one brightness range,
        // so each layer stays as light or dark compared to the others as it's drawn.
        float[] secondaryRange = toolType.isArmor()
                ? TextureRecolorer.luminanceRange(layers.primary(), layers.secondary(), layers.tertiary())
                : TextureRecolorer.luminanceRange(layers.secondary());
        float[] tertiaryRange = toolType.isArmor() ? secondaryRange : TextureRecolorer.luminanceRange(layers.tertiary());
        for (ToolMaterial material : materialsFor(materials, toolType.getSecondaryPart())) {
            addTexture(tool + "/" + material.getId() + "/" + layerNames[1], TextureRecolorer.recolor(layers.secondary(), material.getColors(),
                    secondaryRange[0], secondaryRange[1], TextureRecolorer.ColorRange.FULL));
        }
        for (ToolMaterial material : materialsFor(materials, toolType.getTertiaryPart())) {
            addTexture(tool + "/" + material.getId() + "/" + layerNames[2], TextureRecolorer.recolor(layers.tertiary(), material.getColors(),
                    tertiaryRange[0], tertiaryRange[1], TextureRecolorer.ColorRange.FULL));
        }

        // The secondary part (handle or core) is drawn at the back, then the primary part (head or frame), then the binding on top
        String baseModel = addModel(tool + "/base", parent,
                tool + "/base/" + layerNames[1], primaryTexture(toolType, "base"), tool + "/base/" + layerNames[2]);

        JsonArray cases = new JsonArray();
        for (ToolMaterial primary : materialsFor(materials, toolType.getPrimaryPart())) {
            for (ToolMaterial secondary : materialsFor(materials, toolType.getSecondaryPart())) {
                for (ToolMaterial tertiary : materialsFor(materials, toolType.getTertiaryPart())) {
                    String combination = getModelData(primary, secondary, tertiary);
                    String model = addModel(tool + "/" + combination, parent,
                            tool + "/" + secondary.getId() + "/" + layerNames[1],
                            primaryTexture(toolType, primary.getId()),
                            tool + "/" + tertiary.getId() + "/" + layerNames[2]);
                    cases.add(selectCase(combination, model));
                }
            }
        }
        addItemDefinition(tool, cases, baseModel);
    }

    /***
     * @return the texture of the tool's primary layer for a material (or "base"). Armor uses the frame's part icon.
     */
    private static String primaryTexture(ToolType toolType, String material) {
        if (toolType.isArmor()) {
            return "part/" + material + "/" + getTextureName(toolType.getPrimaryPart());
        }
        return getItemFolder(toolType) + "/" + toolType.name().toLowerCase(Locale.ROOT) + "/" + material + "/" + getLayerNames(toolType)[0];
    }

    /***
     * Generates the worn armor textures and one equipment asset per frame/core/binding combination.
     * An equipment asset is shared by the helmet, chestplate, leggings and boots made of the same materials.
     */
    private void generateEquipment(List<ToolMaterial> materials) throws IOException {
        List<ToolMaterial> frames = materials.stream().filter(ResourcePackGenerator::canMakeArmorFrame).toList();
        List<ToolMaterial> cores = materialsFor(materials, PartType.ARMOR_CORE);
        List<ToolMaterial> bindings = materialsFor(materials, PartType.ARMOR_BINDING);

        // Every worn layer is recolored with one brightness range across all six textures, so each layer stays as light
        // or dark compared to the others as it's drawn, and a shade gets the same color on the leggings as on the rest of the armor
        ToolLayers humanoid = EQUIPMENT_LAYERS.get("humanoid");
        ToolLayers leggings = EQUIPMENT_LAYERS.get("humanoid_leggings");
        float[] range = TextureRecolorer.luminanceRange(humanoid.primary(), humanoid.secondary(), humanoid.tertiary(),
                leggings.primary(), leggings.secondary(), leggings.tertiary());

        for (String layerType : EQUIPMENT_LAYER_TYPES) {
            ToolLayers layers = EQUIPMENT_LAYERS.get(layerType);
            addEquipmentTexture(layerType, "base/frame", layers.primary());
            addEquipmentTexture(layerType, "base/core", layers.secondary());
            addEquipmentTexture(layerType, "base/binding", layers.tertiary());
            for (ToolMaterial material : frames) {
                // Worn frames only use the dark to medium colors
                addEquipmentTexture(layerType, material.getId() + "/frame",
                        TextureRecolorer.recolor(layers.primary(), material.getColors(), range[0], range[1], TextureRecolorer.ColorRange.DARK_TO_MEDIUM));
            }
            for (ToolMaterial material : cores) {
                addEquipmentTexture(layerType, material.getId() + "/core",
                        TextureRecolorer.recolor(layers.secondary(), material.getColors(), range[0], range[1], TextureRecolorer.ColorRange.FULL));
            }
            for (ToolMaterial material : bindings) {
                addEquipmentTexture(layerType, material.getId() + "/binding",
                        TextureRecolorer.recolor(layers.tertiary(), material.getColors(), range[0], range[1], TextureRecolorer.ColorRange.FULL));
            }
        }

        addEquipmentAsset("armor/base", "base", "base", "base");
        for (ToolMaterial frame : frames) {
            for (ToolMaterial core : cores) {
                for (ToolMaterial binding : bindings) {
                    addEquipmentAsset("armor/" + getModelData(frame, core, binding), frame.getId(), core.getId(), binding.getId());
                }
            }
        }
    }

    private void addEquipmentTexture(String layerType, String path, BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        files.put("textures/entity/equipment/" + layerType + "/" + path + ".png", out.toByteArray());
    }

    /***
     * Adds an equipment asset that draws the core, then the frame on top of it, then the binding on top of the frame.
     */
    private void addEquipmentAsset(String path, String frame, String core, String binding) {
        JsonObject layerTypes = new JsonObject();
        for (String layerType : EQUIPMENT_LAYER_TYPES) {
            JsonArray layers = new JsonArray();
            for (String texture : new String[]{core + "/core", frame + "/frame", binding + "/binding"}) {
                JsonObject layer = new JsonObject();
                layer.addProperty("texture", NAMESPACE + ":" + texture);
                layers.add(layer);
            }
            layerTypes.add(layerType, layers);
        }
        JsonObject asset = new JsonObject();
        asset.add("layers", layerTypes);
        files.put("equipment/" + path + ".json", GSON.toJson(asset).getBytes(StandardCharsets.UTF_8));
    }

    /***
     * @return true if the part is the frame of a piece of armor
     */
    private static boolean isArmorFrame(PartType partType) {
        for (ToolType toolType : ToolType.values()) {
            if (toolType.isArmor() && toolType.getPrimaryPart() == partType) {
                return true;
            }
        }
        return false;
    }

    private static boolean canMakeArmorFrame(ToolMaterial material) {
        for (ToolType toolType : ToolType.values()) {
            if (toolType.isArmor() && material.getValidParts().contains(toolType.getPrimaryPart())) {
                return true;
            }
        }
        return false;
    }

    private static List<ToolMaterial> materialsFor(List<ToolMaterial> materials, PartType partType) {
        return materials.stream().filter(material -> material.getValidParts().contains(partType)).toList();
    }

    private void addTexture(String path, BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        files.put("textures/item/" + path + ".png", out.toByteArray());
    }

    /***
     * Adds an item model made of texture layers, the first layer at the back.
     * @return the model's resource location
     */
    private String addModel(String path, String parent, String... textures) {
        JsonObject layers = new JsonObject();
        for (int i = 0; i < textures.length; i++) {
            layers.addProperty("layer" + i, NAMESPACE + ":item/" + textures[i]);
        }
        JsonObject model = new JsonObject();
        model.addProperty("parent", parent);
        model.add("textures", layers);
        files.put("models/item/" + path + ".json", GSON.toJson(model).getBytes(StandardCharsets.UTF_8));
        return NAMESPACE + ":item/" + path;
    }

    /***
     * Adds an item model definition that picks a model by the item's first custom_model_data string.
     */
    private void addItemDefinition(String path, JsonArray cases, String fallbackModel) {
        JsonObject select = new JsonObject();
        select.addProperty("type", "minecraft:select");
        select.addProperty("property", "minecraft:custom_model_data");
        select.addProperty("index", 0);
        select.add("cases", cases);
        select.add("fallback", modelReference(fallbackModel));

        JsonObject definition = new JsonObject();
        definition.add("model", select);
        files.put("items/" + path + ".json", GSON.toJson(definition).getBytes(StandardCharsets.UTF_8));
    }

    private static JsonObject selectCase(String when, String model) {
        JsonObject selectCase = new JsonObject();
        selectCase.addProperty("when", when);
        selectCase.add("model", modelReference(model));
        return selectCase;
    }

    private static JsonObject modelReference(String model) {
        JsonObject reference = new JsonObject();
        reference.addProperty("type", "minecraft:model");
        reference.addProperty("model", model);
        return reference;
    }

    /***
     * Writes the generated files that changed and deletes generated files from earlier runs that aren't generated any more.
     * @param force write every file, even the ones that haven't changed
     * @return the number of files written or deleted
     */
    private int write(Path assetsFolder, boolean force) throws IOException {
        int changed = 0;

        for (String folder : GENERATED_FOLDERS) {
            Path generatedFolder = assetsFolder.resolve(folder);
            if (!Files.isDirectory(generatedFolder)) {
                continue;
            }
            List<Path> existing;
            try (Stream<Path> walk = Files.walk(generatedFolder)) {
                // Deepest paths first so folders are emptied before they're checked
                existing = walk.sorted(Comparator.reverseOrder()).toList();
            }
            for (Path path : existing) {
                if (Files.isDirectory(path)) {
                    try (Stream<Path> contents = Files.list(path)) {
                        if (contents.findAny().isEmpty()) {
                            Files.delete(path);
                        }
                    }
                } else if (!files.containsKey(relativePath(assetsFolder, path))) {
                    Files.delete(path);
                    changed++;
                }
            }
        }

        for (Map.Entry<String, byte[]> file : files.entrySet()) {
            Path target = assetsFolder.resolve(file.getKey());
            if (!force && Files.isRegularFile(target) && Arrays.equals(Files.readAllBytes(target), file.getValue())) {
                continue;
            }
            Files.createDirectories(target.getParent());
            Files.write(target, file.getValue());
            changed++;
        }
        return changed;
    }

    private static String relativePath(Path root, Path file) {
        return root.relativize(file).toString().replace(file.getFileSystem().getSeparator(), "/");
    }

    /***
     * Loads the layers finished tools and armor are made of. A tool type only gets a model if it has all three layers.
     */
    private static Map<ToolType, ToolLayers> loadToolLayers() {
        Map<ToolType, ToolLayers> layers = new EnumMap<>(ToolType.class);
        for (ToolType toolType : ToolType.values()) {
            String folder = "textures/item/" + getItemFolder(toolType) + "/" + toolType.name().toLowerCase(Locale.ROOT) + "/";
            String[] layerNames = getLayerNames(toolType);
            // Armor icons use the frame's part icon (base_<armor>_frame.png) as the frame layer
            BufferedImage primary = toolType.isArmor()
                    ? readImage("textures/item/base_" + getTextureName(toolType.getPrimaryPart()) + ".png")
                    : readImage(folder + layerNames[0] + ".png");
            BufferedImage secondary = readImage(folder + layerNames[1] + ".png");
            BufferedImage tertiary = readImage(folder + layerNames[2] + ".png");
            if (primary != null && secondary != null && tertiary != null) {
                layers.put(toolType, new ToolLayers(primary, secondary, tertiary));
            }
        }
        return layers;
    }

    /***
     * Loads the worn armor layers. Worn armor is only generated if every layer type has all three layers.
     */
    private static Map<String, ToolLayers> loadEquipmentLayers() {
        Map<String, ToolLayers> layers = new HashMap<>();
        for (String layerType : EQUIPMENT_LAYER_TYPES) {
            String folder = "textures/equipment/" + layerType + "/";
            BufferedImage frame = readImage(folder + "frame.png");
            BufferedImage core = readImage(folder + "core.png");
            BufferedImage binding = readImage(folder + "binding.png");
            if (frame != null && core != null && binding != null) {
                layers.put(layerType, new ToolLayers(frame, core, binding));
            }
        }
        return layers;
    }

    private static BufferedImage readImage(String resourcePath) {
        try (InputStream in = plugin.getResource(resourcePath)) {
            return in == null ? null : ImageIO.read(in);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Could not read the texture " + resourcePath, e);
            return null;
        }
    }

    private static Map<PartType, BufferedImage> loadBaseTextures() {
        Map<PartType, BufferedImage> textures = new EnumMap<>(PartType.class);
        for (PartType partType : PartType.values()) {
            BufferedImage image = readImage("textures/item/base_" + getTextureName(partType) + ".png");
            if (image != null) {
                textures.put(partType, image);
            }
        }
        return textures;
    }

    /***
     * @return the name of the part's base texture, without the base_ prefix
     */
    public static String getTextureName(PartType partType) {
        if (partType == PartType.SWORD_BLADE) {
            return "blade";
        }
        return partType.name().toLowerCase(Locale.ROOT);
    }

    public static boolean hasTexture(PartType partType) {
        return BASE_TEXTURES.containsKey(partType);
    }

    public static boolean hasTextures(ToolType toolType) {
        return TOOL_LAYERS.containsKey(toolType);
    }

    /***
     * @return true if the worn armor textures exist for every equipment layer type
     */
    public static boolean hasEquipmentTextures() {
        return EQUIPMENT_LAYERS.keySet().containsAll(EQUIPMENT_LAYER_TYPES);
    }

    /***
     * @return the folder a tool type's item textures and models are in: armor for armor, tool for everything else
     */
    private static String getItemFolder(ToolType toolType) {
        return toolType.isArmor() ? "armor" : "tool";
    }

    /***
     * @return the names of the tool type's primary, secondary and tertiary layers
     */
    private static String[] getLayerNames(ToolType toolType) {
        return toolType.isArmor() ? new String[]{"frame", "core", "binding"} : new String[]{"head", "handle", "binding"};
    }

    private static boolean hasValidId(ToolMaterial material) {
        return material.getId().matches("[a-z0-9_.-]+") && !material.getId().equals("base");
    }

    /***
     * @return true if textures were generated for the material as this part
     */
    private static boolean hasGeneratedTexture(ToolMaterial material, PartType partType) {
        return material.getColors() != null && hasValidId(material) && material.getValidParts().contains(partType);
    }

    private static boolean isEnabled() {
        return plugin.getConfig().getBoolean("resource-pack.generate", true);
    }

    /***
     * @return the custom_model_data string that selects a tool's model, e.g. "iron/wood/gold" for head/handle/binding
     */
    public static String getModelData(ToolMaterial head, ToolMaterial handle, ToolMaterial binding) {
        return head.getId() + "/" + handle.getId() + "/" + binding.getId();
    }

    /***
     * Points the part item at its generated model. Does nothing if the resource pack isn't generated or the part has no texture.
     */
    public static void applyPartModel(ItemStack item, PartType partType, ToolMaterial material) {
        if (!isEnabled() || !hasTexture(partType)) {
            return;
        }
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key(NAMESPACE, "part/" + partType.name().toLowerCase(Locale.ROOT)));
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(material.getId()).build());
    }

    /***
     * Points the tool item at its generated model, and armor at its generated worn textures.
     * Does nothing if the resource pack isn't generated or the textures don't exist.
     */
    public static void applyToolModel(ItemStack item, ToolType toolType, ToolMaterial primary, ToolMaterial secondary, ToolMaterial tertiary) {
        if (!isEnabled()) {
            return;
        }
        if (hasTextures(toolType)) {
            item.setData(DataComponentTypes.ITEM_MODEL, Key.key(NAMESPACE, getItemFolder(toolType) + "/" + toolType.name().toLowerCase(Locale.ROOT)));
            item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(getModelData(primary, secondary, tertiary)).build());
        }
        if (toolType.isArmor() && hasEquipmentTextures()) {
            // Equipment assets can't fall back like item models, so use the uncolored asset if this combination wasn't generated
            boolean generated = hasGeneratedTexture(primary, toolType.getPrimaryPart())
                    && hasGeneratedTexture(secondary, toolType.getSecondaryPart())
                    && hasGeneratedTexture(tertiary, toolType.getTertiaryPart());
            Key asset = Key.key(NAMESPACE, generated ? "armor/" + getModelData(primary, secondary, tertiary) : "armor/base");
            item.setData(DataComponentTypes.EQUIPPABLE, withAsset(item.getData(DataComponentTypes.EQUIPPABLE), toolType, asset));
        }
    }

    /***
     * @return a copy of the base armor's equippable component that uses the given equipment asset
     */
    private static Equippable withAsset(Equippable base, ToolType toolType, Key asset) {
        if (base == null) {
            return Equippable.equippable(toolType.getArmorSlot()).assetId(asset).build();
        }
        return Equippable.equippable(base.slot())
                .equipSound(base.equipSound())
                .assetId(asset)
                .cameraOverlay(base.cameraOverlay())
                .allowedEntities(base.allowedEntities())
                .dispensable(base.dispensable())
                .swappable(base.swappable())
                .damageOnHurt(base.damageOnHurt())
                .equipOnInteract(base.equipOnInteract())
                .canBeSheared(base.canBeSheared())
                .shearSound(base.shearSound())
                .build();
    }
}
