package io.github.lucaargolo.exporter;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

public class ExporterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext build, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("exporter")
            .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                    .executes(context -> {
                        BlockPos pos1 = BlockPosArgument.getBlockPos(context, "pos1");
                        BlockPos pos2 = BlockPosArgument.getBlockPos(context, "pos2");
                        ExporterClient.MARKED_BOX = BoundingBox.fromCorners(pos1, pos2);
                        ExporterClient.NODES.clear();
                        return 1;
                    })
                )
            )
            .then(Commands.argument("entity", EntityArgument.entity())
                .executes(context -> {
                    Entity entity = EntityArgument.getEntity(context, "entity");
                    ExporterClient.markEntity(entity.getId());
                    ExporterClient.NODES.clear();
                    return 1;
                })
            )
        );

    }

}