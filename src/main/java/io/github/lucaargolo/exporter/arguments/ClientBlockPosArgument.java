package io.github.lucaargolo.exporter.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class ClientBlockPosArgument implements ArgumentType<ClientCoordinates> {
	private static final Collection<String> EXAMPLES = Arrays.asList("0 0 0", "~ ~ ~", "^ ^ ^", "^1 ^ ^-5", "~0.5 ~1 ~-5");

	public static ClientBlockPosArgument blockPos() {
		return new ClientBlockPosArgument();
	}

	public static BlockPos getBlockPos(CommandContext<FabricClientCommandSource> context, String name) {
		return context.getArgument(name, ClientCoordinates.class).getBlockPos(context.getSource());
	}

	public ClientCoordinates parse(StringReader reader) throws CommandSyntaxException {
		return (reader.canRead() && reader.peek() == '^' ? ClientLocalCoordinates.parse(reader) : ClientWorldCoordinates.parseInt(reader));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
		if (!(commandContext.getSource() instanceof SharedSuggestionProvider)) {
			return Suggestions.empty();
		} else {
			String string = suggestionsBuilder.getRemaining();
			Collection<SharedSuggestionProvider.TextCoordinates> collection;
			if (!string.isEmpty() && string.charAt(0) == '^') {
				collection = Collections.singleton(SharedSuggestionProvider.TextCoordinates.DEFAULT_LOCAL);
			} else {
				collection = ((SharedSuggestionProvider)commandContext.getSource()).getRelevantCoordinates();
			}

			return SharedSuggestionProvider.suggestCoordinates(string, collection, suggestionsBuilder, Commands.createValidator(this::parse));
		}
	}

	@Override
	public Collection<String> getExamples() {
		return EXAMPLES;
	}
}
