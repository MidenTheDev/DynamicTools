package us.to.midensthings.dynamicTools.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import us.to.midensthings.dynamicTools.tools.Tool;
import us.to.midensthings.dynamicTools.tools.ToolMaterial;
import us.to.midensthings.dynamicTools.tools.ToolType;

public class DebugCommands {

    public LiteralCommandNode<CommandSourceStack> debugCore = Commands.literal("dtdebug").requires(source -> source.getSender().hasPermission("dynamictools.commands.debug"))
            .then(Commands.literal("toolGen1").executes(ctx -> {
                CommandSender sender = ctx.getSource().getSender();
                if (!(sender instanceof Player)) {
                    return Command.SINGLE_SUCCESS;
                }

                Player p = (Player) sender;

                Tool tool = new Tool(ToolType.SWORD, ToolMaterial.fromId("wood"),ToolMaterial.fromId("wood"),ToolMaterial.fromId("wood"));

                p.getInventory().addItem(tool.getItemStack());

                return Command.SINGLE_SUCCESS;
            }))

            .build();

}
