package us.to.midensthings.dynamicTools.modifiers.builtin;

import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.modifiers.EventModifier;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.TypeSets;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/***
 * Gives mobs killed with the tool a chance to drop their head, 10% per level.
 * Mobs with a vanilla head drop that, other mobs drop a player head with the texture set in mob-heads.yml.
 */
public class BeheadingModifier implements EventModifier {
    private static final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);

    private static final double CHANCE_PER_LEVEL = 0.1;

    private static final Map<EntityType, Material> VANILLA_HEADS = new EnumMap<>(Map.of(
            EntityType.ZOMBIE, Material.ZOMBIE_HEAD,
            EntityType.SKELETON, Material.SKELETON_SKULL,
            EntityType.WITHER_SKELETON, Material.WITHER_SKELETON_SKULL,
            EntityType.CREEPER, Material.CREEPER_HEAD,
            EntityType.PIGLIN, Material.PIGLIN_HEAD,
            EntityType.ENDER_DRAGON, Material.DRAGON_HEAD
    ));

    // Heads from mob-heads.yml
    private final Map<EntityType, ItemStack> customHeads = new HashMap<>();

    public BeheadingModifier() {
        loadHeads();
    }

    @Override
    public String getId() {
        return "beheading";
    }

    @Override
    public String getDisplayName() {
        return "<dark_red>Beheading";
    }

    @Override
    public Set<ToolType> getValidToolTypes() {
        return TypeSets.onlyWeapons;
    }

    @EventHandler(ignoreCancelled = true)
    public void onDeath(EntityDeathEvent e) {
        if (!(e.getDamageSource().getCausingEntity() instanceof LivingEntity killer)) {
            return;
        }
        int level = getLevel(killer);
        if (level <= 0 || ThreadLocalRandom.current().nextDouble() >= CHANCE_PER_LEVEL * level) {
            return;
        }

        ItemStack head = createHead(e.getEntity());
        if (head == null) {
            return;
        }
        // Don't drop a second vanilla head if the mob already dropped one, e.g. from a charged creeper
        if (head.getType() != Material.PLAYER_HEAD && e.getDrops().stream().anyMatch(drop -> drop.getType() == head.getType())) {
            return;
        }
        e.getDrops().add(head);
        plugin.debug(killer.getName() + " beheaded a " + e.getEntityType().name());
    }

    /***
     * @return the entity's head, or null if it has no vanilla head and none is set in mob-heads.yml
     */
    private ItemStack createHead(LivingEntity entity) {
        if (entity instanceof Player player) {
            ItemStack head = ItemStack.of(Material.PLAYER_HEAD);
            head.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile(player.getPlayerProfile()));
            return head;
        }
        Material vanillaHead = VANILLA_HEADS.get(entity.getType());
        if (vanillaHead != null) {
            return ItemStack.of(vanillaHead);
        }
        ItemStack customHead = customHeads.get(entity.getType());
        return customHead == null ? null : customHead.clone();
    }

    /***
     * Loads the custom heads from mob-heads.yml, creating the file if it doesn't exist.
     */
    public void loadHeads() {
        File file = new File(plugin.getDataFolder(), "mob-heads.yml");
        if (!file.exists()) {
            plugin.saveResource("mob-heads.yml", false);
        }
        customHeads.clear();

        ConfigurationSection heads = YamlConfiguration.loadConfiguration(file).getConfigurationSection("heads");
        if (heads == null) {
            return;
        }
        for (String id : heads.getKeys(false)) {
            NamespacedKey key = NamespacedKey.fromString(id.toLowerCase(Locale.ROOT));
            EntityType type = key == null ? null : Registry.ENTITY_TYPE.get(key);
            String texture = heads.getString(id + ".texture", "");
            if (type == null) {
                plugin.getLogger().warning("Unknown mob " + id + " in mob-heads.yml");
                continue;
            }
            if (texture.isBlank()) {
                plugin.getLogger().warning("Mob " + id + " in mob-heads.yml has no texture");
                continue;
            }
            customHeads.put(type, createCustomHead(type, texture, heads.getString(id + ".name")));
        }
        plugin.debug("Loaded " + customHeads.size() + " custom mob heads");
    }

    private static ItemStack createCustomHead(EntityType type, String texture, String name) {
        ItemStack head = ItemStack.of(Material.PLAYER_HEAD);
        head.setData(DataComponentTypes.PROFILE, ResolvableProfile.resolvableProfile()
                // The same UUID for every head of a mob so they stack
                .uuid(UUID.nameUUIDFromBytes(("dynamictools:head/" + type.getKey()).getBytes(StandardCharsets.UTF_8)))
                .addProperty(new ProfileProperty("textures", toTextureValue(texture)))
                .build());

        Component headName = name == null
                ? Component.translatable(type.translationKey()).append(Component.text(" Head"))
                : MiniMessage.miniMessage().deserialize(name).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        head.setData(DataComponentTypes.ITEM_NAME, headName);
        return head;
    }

    /***
     * @return the base64 textures property for a texture ID, or the texture unchanged if it's already base64
     */
    private static String toTextureValue(String texture) {
        String trimmed = texture.trim();
        if (!trimmed.matches("[0-9a-fA-F]{32,}")) {
            return trimmed;
        }
        String json = "{\"textures\":{\"SKIN\":{\"url\":\"http://textures.minecraft.net/texture/" + trimmed.toLowerCase(Locale.ROOT) + "\"}}}";
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }
}
