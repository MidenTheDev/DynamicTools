package us.to.midensthings.dynamicTools.resourcepack;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.CustomModelData;
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
 * models/item/part/&lt;material&gt;/&lt;part&gt;.json                  one model per part of each material
 * models/item/tool/&lt;tool&gt;/&lt;head&gt;/&lt;handle&gt;/&lt;binding&gt;.json  the tool layers stacked into a finished tool
 * items/part/&lt;part&gt;.json, items/tool/&lt;tool&gt;.json             pick the model from the item's custom_model_data string
 * </pre>
 * Parts and tools of materials without colors use the uncolored textures in the base folders.
 * Tool types without all three layers aren't generated and keep their vanilla look.
 */
public class ResourcePackGenerator {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    public static final String NAMESPACE = "dynamictools";

    // Everything in these folders is generated, so files in them that aren't generated any more get deleted
    private static final List<String> GENERATED_FOLDERS = List.of(
            "textures/item/part", "textures/item/tool", "models/item/part", "models/item/tool", "items/part", "items/tool");

    /***
     * The grayscale layers of a finished tool, positioned where each part sits on the tool.
     */
    private record ToolLayers(BufferedImage head, BufferedImage handle, BufferedImage binding) {}

    private static final Gson GSON = new Gson();
    // Part item icons, from textures/item/base_<part>.png
    private static final Map<PartType, BufferedImage> BASE_TEXTURES = loadBaseTextures();
    // Finished tool layers, from textures/item/tool/<tool>/head.png, handle.png and binding.png
    private static final Map<ToolType, ToolLayers> TOOL_LAYERS = loadToolLayers();

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
            if (!material.getId().matches("[a-z0-9_.-]+") || material.getId().equals("base")) {
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
            addTexture(texturePath, TextureRecolorer.recolor(baseTexture, material.getColors()));
            String model = addModel("part/" + material.getId() + "/" + part, "minecraft:item/generated", texturePath);
            cases.add(selectCase(material.getId(), model));
        }
        addItemDefinition("part/" + part, cases, baseModel);
    }

    private void generateTool(ToolType toolType, List<ToolMaterial> materials) throws IOException {
        String tool = toolType.name().toLowerCase(Locale.ROOT);
        ToolLayers layers = TOOL_LAYERS.get(toolType);

        // Recolor each layer for every material that can make that part
        addTexture("tool/" + tool + "/base/head", layers.head());
        addTexture("tool/" + tool + "/base/handle", layers.handle());
        addTexture("tool/" + tool + "/base/binding", layers.binding());
        for (ToolMaterial material : materialsFor(materials, toolType.getPrimaryPart())) {
            addTexture("tool/" + tool + "/" + material.getId() + "/head", TextureRecolorer.recolor(layers.head(), material.getColors()));
        }
        for (ToolMaterial material : materialsFor(materials, toolType.getSecondaryPart())) {
            addTexture("tool/" + tool + "/" + material.getId() + "/handle", TextureRecolorer.recolor(layers.handle(), material.getColors()));
        }
        for (ToolMaterial material : materialsFor(materials, toolType.getTertiaryPart())) {
            addTexture("tool/" + tool + "/" + material.getId() + "/binding", TextureRecolorer.recolor(layers.binding(), material.getColors()));
        }

        // The handle is drawn at the back, then the head, then the binding on top
        String baseModel = addModel("tool/" + tool + "/base", "minecraft:item/handheld",
                "tool/" + tool + "/base/handle", "tool/" + tool + "/base/head", "tool/" + tool + "/base/binding");

        JsonArray cases = new JsonArray();
        for (ToolMaterial head : materialsFor(materials, toolType.getPrimaryPart())) {
            for (ToolMaterial handle : materialsFor(materials, toolType.getSecondaryPart())) {
                for (ToolMaterial binding : materialsFor(materials, toolType.getTertiaryPart())) {
                    String combination = getModelData(head, handle, binding);
                    String model = addModel("tool/" + tool + "/" + combination, "minecraft:item/handheld",
                            "tool/" + tool + "/" + handle.getId() + "/handle",
                            "tool/" + tool + "/" + head.getId() + "/head",
                            "tool/" + tool + "/" + binding.getId() + "/binding");
                    cases.add(selectCase(combination, model));
                }
            }
        }
        addItemDefinition("tool/" + tool, cases, baseModel);
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
     * Loads the layers finished tools are made of. A tool type only gets a model if it has all three layers.
     */
    private static Map<ToolType, ToolLayers> loadToolLayers() {
        Map<ToolType, ToolLayers> layers = new EnumMap<>(ToolType.class);
        for (ToolType toolType : ToolType.values()) {
            String folder = "textures/item/tool/" + toolType.name().toLowerCase(Locale.ROOT) + "/";
            BufferedImage head = readImage(folder + "head.png");
            BufferedImage handle = readImage(folder + "handle.png");
            BufferedImage binding = readImage(folder + "binding.png");
            if (head != null && handle != null && binding != null) {
                layers.put(toolType, new ToolLayers(head, handle, binding));
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
     * Points the tool item at its generated model. Does nothing if the resource pack isn't generated or the tool has no textures.
     */
    public static void applyToolModel(ItemStack item, ToolType toolType, ToolMaterial head, ToolMaterial handle, ToolMaterial binding) {
        if (!isEnabled() || !hasTextures(toolType)) {
            return;
        }
        item.setData(DataComponentTypes.ITEM_MODEL, Key.key(NAMESPACE, "tool/" + toolType.name().toLowerCase(Locale.ROOT)));
        item.setData(DataComponentTypes.CUSTOM_MODEL_DATA, CustomModelData.customModelData().addString(getModelData(head, handle, binding)).build());
    }
}
