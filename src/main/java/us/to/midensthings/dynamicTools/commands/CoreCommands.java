package us.to.midensthings.dynamicTools.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import us.to.midensthings.dynamicTools.DynamicTools;
import us.to.midensthings.dynamicTools.gui.DynamicToolsGui;
import us.to.midensthings.dynamicTools.gui.PartAssembler;
import us.to.midensthings.dynamicTools.gui.PartCrafter;
import us.to.midensthings.dynamicTools.tools.Tool;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;
import us.to.midensthings.dynamicTools.tools.ToolType;
import us.to.midensthings.dynamicTools.util.ItemManager;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class CoreCommands {
    private final DynamicTools plugin = DynamicTools.getPlugin(DynamicTools.class);
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
            .then(Commands.literal("open").requires(source -> source.getSender().hasPermission("dynamictools.commands.open"))
                    .then(Commands.literal("partCrafter").executes(ctx -> openForSender(ctx, PartCrafter::new))
                            .then(Commands.argument("player",ArgumentTypes.player()).requires(source -> source.getSender().hasPermission("dynamictools.commands.open.others"))
                                    .executes(ctx -> openForPlayer(ctx, PartCrafter::new, "Part Crafter"))))

                    .then(Commands.literal("partAssembler").executes(ctx -> openForSender(ctx, PartAssembler::new))
                            .then(Commands.argument("player",ArgumentTypes.player()).requires(source -> source.getSender().hasPermission("dynamictools.commands.open.others"))
                                    .executes(ctx -> openForPlayer(ctx, PartAssembler::new, "Part Assembler"))))
            )

            .build();

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
