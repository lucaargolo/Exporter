package io.github.lucaargolo.exporter;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;

public class ExporterCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext build, Commands.CommandSelection selection) {
        dispatcher.register(Commands.literal("exporter")
            .then(Commands.argument("entity", EntityArgument.entity())
                .executes(context -> {
                    Entity entity = EntityArgument.getEntity(context, "entity");
                    ExporterClient.MARKED_ENTITY = entity.getId();
                    ExporterClient.CAPTURED_IMAGES.clear();
                    ExporterClient.VERTICE_COUNT.clear();
                    ExporterClient.CAPTURED_VERTICES.clear();
                    ExporterClient.CAPTURED_TRIANGLES.clear();
                    ExporterClient.CAPTURED_UVS.clear();
                    return 1;
                })
            )
        );

    }

}