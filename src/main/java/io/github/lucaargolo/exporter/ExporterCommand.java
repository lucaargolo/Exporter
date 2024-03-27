package io.github.lucaargolo.exporter;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import io.github.lucaargolo.exporter.arguments.ClientBlockPosArgument;
import io.github.lucaargolo.exporter.arguments.ClientEntityArgument;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class ExporterCommand {

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext build) {
        dispatcher.register(ClientCommandManager.literal("exporter")
            .then(ClientCommandManager.argument("pos1", ClientBlockPosArgument.blockPos())
                .then(ClientCommandManager.argument("pos2", ClientBlockPosArgument.blockPos())
                    .executes(context -> {
                        BlockPos pos1 = ClientBlockPosArgument.getBlockPos(context, "pos1");
                        BlockPos pos2 = ClientBlockPosArgument.getBlockPos(context, "pos2");
                        ExporterClient.COMPLETE = false;
                        ExporterClient.MARKED_BOX = BoundingBox.fromCorners(pos1, pos2);
                        ExporterClient.NODES.clear();
                        ExporterClient.MATERIALS.clear();
                        ExporterClient.TEXTURES.clear();
                        return 1;
                    })
                    .then(ClientCommandManager.argument("complete", BoolArgumentType.bool())
                        .executes(context -> {
                            BlockPos pos1 = ClientBlockPosArgument.getBlockPos(context, "pos1");
                            BlockPos pos2 = ClientBlockPosArgument.getBlockPos(context, "pos2");
                            ExporterClient.COMPLETE = BoolArgumentType.getBool(context, "complete");
                            ExporterClient.MARKED_BOX = BoundingBox.fromCorners(pos1, pos2);
                            ExporterClient.NODES.clear();
                            ExporterClient.MATERIALS.clear();
                            ExporterClient.TEXTURES.clear();
                            return 1;
                        })
                    )
                )
            )
            .then(ClientCommandManager.argument("entity", ClientEntityArgument.entity())
                .executes(context -> {
                    Entity entity = ClientEntityArgument.getEntity(context, "entity");
                    ExporterClient.markEntity(entity.getId());
                    ExporterClient.NODES.clear();
                    ExporterClient.MATERIALS.clear();
                    ExporterClient.TEXTURES.clear();
                    return 1;
                })
            )
        );

    }

}