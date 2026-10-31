package us.to.midensthings.dynamicTools.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.gui.DynamicToolsGui;
import us.to.midensthings.dynamicTools.gui.PartAssembler;
import us.to.midensthings.dynamicTools.gui.PartCrafter;
import us.to.midensthings.dynamicTools.modifiers.Modifier;
import us.to.midensthings.dynamicTools.resourcepack.ResourcePackGenerator;
import us.to.midensthings.dynamicTools.tools.Tool;
import us.to.midensthings.dynamicTools.tools.ToolItems;
import us.to.midensthings.dynamicTools.util.RomanNumerals;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.ItemManager;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.function.Function;

public class CoreCommands {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);
    // Only one texture generation at a time, they'd write to the same files
    private static final AtomicBoolean generatingTextures = new AtomicBoolean(false);
    public LiteralCommandNode<CommandSourceStack> coreCommand = Commands.literal("dt")
            .then(Commands.literal("give").requires(source -> source.getSender().hasPermission("dynamictools.commands.give"))
                    .then(Commands.argument("toolType", StringArgumentType.word()).suggests(this::toolTypesSuggestions)
                            .then(Commands.argument("primaryMaterial",StringArgumentType.word()).suggests(this::materialSuggestions).then(Commands.argument("secondaryMaterial",StringArgumentType.word()).suggests(this::materialSuggestions).then(Commands.argument("tertiaryMaterial",StringArgumentType.word()).suggests(this::materialSuggestions).executes(ctx -> {
                                CommandSender sender = ctx.getSource().getSender();
                                if (!(sender instanceof Player)) {
                                    sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Please specify a player!"));
                                    return Command.SINGLE_SUCCESS;
                                }
                                Player p = (Player) sender;
                                return giveCommand(ctx,p,sender,false);
                            })
                                            .then(Commands.argument("player", ArgumentTypes.player()).executes(ctx -> {
                                                final PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
                                                Player p = resolver.resolve(ctx.getSource()).getFirst();
                                                return giveCommand(ctx,p,ctx.getSource().getSender(),false);
                                            }).then(Commands.argument("bypassChecks", BoolArgumentType.bool()).executes(ctx -> {
                                                final PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
                                                Player p = resolver.resolve(ctx.getSource()).getFirst();
                                                boolean bypassChecks = ctx.getArgument("bypassChecks",boolean.class);
                                                return giveCommand(ctx,p,ctx.getSource().getSender(),bypassChecks);
                                            })))

                            )))))
            .then(Commands.literal("addModifier").requires(source -> source.getSender().hasPermission("dynamictools.commands.addmodifier"))
                    .then(Commands.argument("modifier", StringArgumentType.word()).suggests(this::modifierSuggestions)
                            .executes(ctx -> addModifierCommand(ctx, 1, false))
                            .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                    .executes(ctx -> addModifierCommand(ctx, IntegerArgumentType.getInteger(ctx, "level"), false))
                                    .then(Commands.argument("bypassToolTypes", BoolArgumentType.bool())
                                            .executes(ctx -> addModifierCommand(ctx, IntegerArgumentType.getInteger(ctx, "level"),
                                                    BoolArgumentType.getBool(ctx, "bypassToolTypes")))))))
            .then(Commands.literal("reload").requires(source -> source.getSender().hasPermission("dynamictools.commands.reload"))
                    .executes(ctx -> reloadCommand(ctx.getSource().getSender())))
            .then(Commands.literal("generateTextures").requires(source -> source.getSender().hasPermission("dynamictools.commands.generatetextures"))
                    .executes(ctx -> generateTexturesCommand(ctx.getSource().getSender())))
            .then(Commands.literal("open").requires(source -> source.getSender().hasPermission("dynamictools.commands.open"))
                    .then(Commands.literal("partCrafter").executes(ctx -> openForSender(ctx, PartCrafter::new))
                            .then(Commands.argument("player",ArgumentTypes.player()).requires(source -> source.getSender().hasPermission("dynamictools.commands.open.others"))
                                    .executes(ctx -> openForPlayer(ctx, PartCrafter::new, "Part Crafter"))))

                    .then(Commands.literal("partAssembler").executes(ctx -> openForSender(ctx, PartAssembler::new))
                            .then(Commands.argument("player",ArgumentTypes.player()).requires(source -> source.getSender().hasPermission("dynamictools.commands.open.others"))
                                    .executes(ctx -> openForPlayer(ctx, PartAssembler::new, "Part Assembler"))))
            )

            .build();

    /***
     * Adds a modifier to the tool in the sender's main hand. The tool is rebuilt so stat modifiers change its stats,
     * and keeps its damage, enchantments and name. Levels add to the level the tool already has.
     * @param bypassToolTypes add the modifier even if it isn't valid for the tool's type
     */
    private int addModifierCommand(CommandContext<CommandSourceStack> ctx, int level, boolean bypassToolTypes) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player p)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Only players can use this command, it changes the tool in your hand."));
            return Command.SINGLE_SUCCESS;
        }

        String modifierId = StringArgumentType.getString(ctx, "modifier").toLowerCase(Locale.ROOT);
        Modifier modifier = plugin.getModifierRegistry().getModifier(modifierId);
        if (modifier == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Unknown modifier " + modifierId));
            return Command.SINGLE_SUCCESS;
        }

        ItemStack held = p.getInventory().getItemInMainHand();
        if (!ToolItems.isTool(held)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>You need to hold a DynamicTools tool."));
            return Command.SINGLE_SUCCESS;
        }

        Map<Modifier,Integer> bonusModifiers = plugin.getModifierManager().getBonusModifiers(held);
        bonusModifiers.merge(modifier, level, Integer::sum);
        Tool tool = Tool.fromItemStack(held, bonusModifiers);
        if (tool == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>This tool is made from a material that no longer exists, so it can't be changed."));
            return Command.SINGLE_SUCCESS;
        }
        if (!bypassToolTypes && !modifier.getValidToolTypes().contains(tool.getToolType())) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + modifierId + " can't be added to a " + tool.getToolType().getDisplayName()
                    + ". Add true to the end of the command to add it anyway."));
            return Command.SINGLE_SUCCESS;
        }

        tool.copyPlayerChanges(held);
        ItemStack result = tool.getItemStack();
        result.setAmount(held.getAmount());
        p.getInventory().setItemInMainHand(result);

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Added ").append(MiniMessage.miniMessage().deserialize(modifier.getDisplayName()))
                .append(MiniMessage.miniMessage().deserialize("<green> " + RomanNumerals.toRoman(level) + " to your " + tool.getToolType().getDisplayName()
                        + " (now level " + RomanNumerals.toRoman(tool.getModifiers().get(modifier)) + ").")));
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> modifierSuggestions(CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        plugin.getModifierRegistry().getModifiers().stream()
                .map(Modifier::getId)
                .filter(id -> id.startsWith(builder.getRemainingLowerCase()))
                .sorted()
                .forEach(builder::suggest);
        return builder.buildFuture();
    }

    /***
     * Reloads config.yml and materials.yml. Textures are left alone, /dt generateTextures does those.
     */
    private int reloadCommand(CommandSender sender) {
        int materialCount;
        try {
            materialCount = plugin.reloadConfigs();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Could not reload the configs", e);
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not reload, config.yml or materials.yml has an error. Nothing was changed, check the console for details."));
            return Command.SINGLE_SUCCESS;
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Reloaded config.yml, materials.yml and mob-heads.yml (" + materialCount + " materials)."));
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>Textures were not regenerated. Run /dt generateTextures to update the textures for new or changed materials."));
        if (plugin.chooseCustomItemSource() != plugin.getCustomItemSource()) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<yellow>The custom item plugin can only be changed by restarting the server."));
        }
        return Command.SINGLE_SUCCESS;
    }

    /***
     * Regenerates every texture and model off the main thread, then has the custom item plugin rebuild its resource pack.
     */
    private int generateTexturesCommand(CommandSender sender) {
        if (!plugin.getConfig().getBoolean("resource-pack.generate", true)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Resource pack generation is turned off. Set resource-pack.generate to true in config.yml to use this."));
            return Command.SINGLE_SUCCESS;
        }
        Path assetsFolder = plugin.getResourcePackAssetsFolder();
        if (assetsFolder == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Could not find the custom item plugin's resource pack folder."));
            return Command.SINGLE_SUCCESS;
        }
        if (!generatingTextures.compareAndSet(false, true)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>The textures are already being generated."));
            return Command.SINGLE_SUCCESS;
        }

        // Copy the materials on the main thread so the generator doesn't read the registry while it could change
        List<ToolMaterial> materials = new ArrayList<>(plugin.getToolMaterialRegistry().getMaterials());
        sender.sendMessage(MiniMessage.miniMessage().deserialize("<gray>Generating textures..."));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            int changed;
            try {
                changed = new ResourcePackGenerator().generate(assetsFolder, materials, true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Could not generate the resource pack files", e);
                Bukkit.getScheduler().runTask(plugin, () ->
                        sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Generating the textures failed, check the console for details.")));
                generatingTextures.set(false);
                return;
            }

            // Messages and commands go back on the main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                generatingTextures.set(false);
                String rebuildCommand = plugin.getResourcePackRebuildCommand();
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Wrote " + changed + " texture and model files. Rebuilding the resource pack with /" + rebuildCommand + "..."));
                plugin.debug("Running /" + rebuildCommand + " after generating textures");
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rebuildCommand);
            });
        });
        return Command.SINGLE_SUCCESS;
    }

    private void openGUI(Inventory inv, Player p) {
        p.openInventory(inv);
    }

    private int openForSender(CommandContext<CommandSourceStack> ctx, Function<Player, DynamicToolsGui> gui) {
        CommandSender sender = ctx.getSource().getSender();
        if (!(sender instanceof Player p)) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Please specify a player!"));
            return Command.SINGLE_SUCCESS;
        }
        openGUI(gui.apply(p).getInventory(), p);
        return Command.SINGLE_SUCCESS;
    }

    private int openForPlayer(CommandContext<CommandSourceStack> ctx, Function<Player, DynamicToolsGui> gui, String guiName) throws CommandSyntaxException {
        final PlayerSelectorArgumentResolver resolver = ctx.getArgument("player", PlayerSelectorArgumentResolver.class);
        Player p = resolver.resolve(ctx.getSource()).getFirst();
        openGUI(gui.apply(p).getInventory(), p);
        ctx.getSource().getSender().sendMessage(MiniMessage.miniMessage().deserialize("<green>Opened the " + guiName + " for " + p.getName()));
        return Command.SINGLE_SUCCESS;
    }

    private int giveCommand(CommandContext<CommandSourceStack> ctx, Player p, CommandSender sender, boolean bypassChecks) {
        ToolType toolType;
        try {
            toolType = ToolType.valueOf(StringArgumentType.getString(ctx,"toolType").toUpperCase());
        } catch (IllegalArgumentException e) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid tool type "+ StringArgumentType.getString(ctx,"toolType")));
            return Command.SINGLE_SUCCESS;
        }




        // Head/frame material
        ToolMaterial primaryMaterial = ToolMaterial.fromId(StringArgumentType.getString(ctx,"primaryMaterial").toLowerCase(Locale.ROOT));
        if (primaryMaterial == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid material "+ StringArgumentType.getString(ctx,"primaryMaterial")));
            return Command.SINGLE_SUCCESS;
        }
        // handle/core material
        ToolMaterial secondaryMaterial = ToolMaterial.fromId(StringArgumentType.getString(ctx,"secondaryMaterial").toLowerCase(Locale.ROOT));
        if (secondaryMaterial == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid material "+ StringArgumentType.getString(ctx,"secondaryMaterial")));
            return Command.SINGLE_SUCCESS;
        }
        // Binding material
        ToolMaterial tertiaryMaterial = ToolMaterial.fromId(StringArgumentType.getString(ctx,"tertiaryMaterial").toLowerCase(Locale.ROOT));
        if (tertiaryMaterial == null) {
            sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Invalid material "+ StringArgumentType.getString(ctx,"tertiaryMaterial")));
            return Command.SINGLE_SUCCESS;
        }

        if (!bypassChecks) {
            if (!primaryMaterial.getValidParts().contains(toolType.getPrimaryPart())) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Material " + primaryMaterial.getId()+" is invalid as a priamry material:"));
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + primaryMaterial.getId()+" Cannot be used to make a " + toolType.getPrimaryPart().name()));
                return Command.SINGLE_SUCCESS;
            }
            if (!secondaryMaterial.getValidParts().contains(toolType.getSecondaryPart())) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Material " + secondaryMaterial.getId()+" is invalid as a secondary material:"));
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + secondaryMaterial.getId()+" Cannot be used to make a " + toolType.getSecondaryPart().name()));
                return Command.SINGLE_SUCCESS;
            }
            if (!tertiaryMaterial.getValidParts().contains(toolType.getTertiaryPart())) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>Material " + tertiaryMaterial.getId()+" is invalid as a tertiary material:"));
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<red>" + tertiaryMaterial.getId()+" Cannot be used to make a " + toolType.getTertiaryPart().name()));
                return Command.SINGLE_SUCCESS;
            }
        }

        Tool tool = new Tool(toolType,primaryMaterial,secondaryMaterial,tertiaryMaterial);
        ItemManager.addOrDropItem(tool.getItemStack(),p);
        if (sender instanceof Player senderPlayer) {
            if (senderPlayer == p) {
                sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Gave you a "+ tool.generateDisplayNameString()));
                return Command.SINGLE_SUCCESS;
            }
        }

        sender.sendMessage(MiniMessage.miniMessage().deserialize("<green>Gave player "+p.getName()+" a "+ tool.generateDisplayNameString()));

        return Command.SINGLE_SUCCESS;
    }



    private CompletableFuture<Suggestions> toolTypesSuggestions(CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        Arrays.stream(ToolType.values())
                .filter(entry -> entry.toString().toLowerCase().startsWith(builder.getRemainingLowerCase()))
                .forEach(remainingType -> builder.suggest(remainingType.toString()));

        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> materialSuggestions(CommandContext<CommandSourceStack> ctx, final SuggestionsBuilder builder) {
        plugin.getToolMaterialRegistry().getMaterials().stream()
                .filter(entry -> entry.getId().toLowerCase().startsWith(builder.getRemainingLowerCase()))
                .forEach(remainingType -> {
                    builder.suggest(remainingType.getId());
                });

        return builder.buildFuture();
    }

}
