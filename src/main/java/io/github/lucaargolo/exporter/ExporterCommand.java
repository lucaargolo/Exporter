package io.github.lucaargolo.exporter;

import com.mojang.brigadier.CommandDispatcher;
import io.github.lucaargolo.exporter.arguments.ClientBlockPosArgument;
import io.github.lucaargolo.exporter.arguments.ClientEntityArgument;
import io.github.lucaargolo.exporter.utils.helper.RenderHelper;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class ExporterCommand {

    public static BlockPos FIRST_POS = null;
    public static BlockPos SECOND_POS = null;

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext build) {
        dispatcher.register(ClientCommandManager.literal("exporter")
            .then(ClientCommandManager.literal("wand")
                .executes(context -> {
                    Minecraft minecraft = context.getSource().getClient();
                    ClientPacketListener connection = minecraft.getConnection();
                    if(connection != null) {
                        connection.sendCommand("give @p wooden_hoe{display:{Name:'[{\"text\":\"Exporter Wand\",\"italic\":false}]'},exporter_wand:true} 1");
                    }
                    return 0;
                })
            )
            .then(ClientCommandManager.literal("pos1")
                .then(ClientCommandManager.argument("pos1", ClientBlockPosArgument.blockPos())
                    .executes(context -> {
                        FIRST_POS = ClientBlockPosArgument.getBlockPos(context, "pos1");
                        context.getSource().sendFeedback(Component.literal("Set first position to "+FIRST_POS.toShortString()));
                        return 0;
                    })
                )
            )
            .then(ClientCommandManager.literal("pos2")
                .then(ClientCommandManager.argument("pos2", ClientBlockPosArgument.blockPos())
                    .executes(context -> {
                        SECOND_POS = ClientBlockPosArgument.getBlockPos(context, "pos2");
                        context.getSource().sendFeedback(Component.literal("Set second position to "+SECOND_POS.toShortString()));
                        return 0;
                    })
                )
            )
            .then(ClientCommandManager.literal("box")
                .executes(context -> {
                    if(FIRST_POS == null) {
                        context.getSource().sendError(Component.literal("First pos is not set."));
                        return 0;
                    }
                    if(SECOND_POS == null) {
                        context.getSource().sendError(Component.literal("Second pos is not set."));
                        return 0;
                    }
                    RenderHelper.markBox(BoundingBox.fromCorners(FIRST_POS, SECOND_POS));
                    return 1;
                })
                .then(ClientCommandManager.argument("pos1", ClientBlockPosArgument.blockPos())
                    .then(ClientCommandManager.argument("pos2", ClientBlockPosArgument.blockPos())
                        .executes(context -> {
                            BlockPos pos1 = ClientBlockPosArgument.getBlockPos(context, "pos1");
                            BlockPos pos2 = ClientBlockPosArgument.getBlockPos(context, "pos2");
                            RenderHelper.markBox(BoundingBox.fromCorners(pos1, pos2));
                            return 1;
                        })
                    )
                )
            )
            .then(ClientCommandManager.literal("entity")
                .then(ClientCommandManager.argument("entity", ClientEntityArgument.entity())
                    .executes(context -> {
                        Entity entity = ClientEntityArgument.getEntity(context, "entity");
                        RenderHelper.markEntity(entity.getId());
                        return 1;
                    })
                )
            )
        );

    }

}