package io.github.lucaargolo.exporter.misc;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("unused")
public class ClientEntityArgument implements ArgumentType<ClientEntitySelector> {

    private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");

    final boolean single;
    final boolean playersOnly;

    protected ClientEntityArgument(boolean single, boolean playersOnly) {
        this.single = single;
        this.playersOnly = playersOnly;
    }

    public static ClientEntityArgument entity() {
        return new ClientEntityArgument(true, false);
    }

    public static Entity getEntity(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ClientEntitySelector.class).findSingleEntity(context.getSource());
    }

    public static ClientEntityArgument entities() {
        return new ClientEntityArgument(false, false);
    }

    public static Collection<? extends Entity> getEntities(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        Collection<? extends Entity> collection = getOptionalEntities(context, name);
        if (collection.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        } else {
            return collection;
        }
    }

    public static Collection<? extends Entity> getOptionalEntities(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ClientEntitySelector.class).findEntities(context.getSource());
    }

    public static Collection<AbstractClientPlayer> getOptionalPlayers(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ClientEntitySelector.class).findPlayers(context.getSource());
    }

    public static ClientEntityArgument player() {
        return new ClientEntityArgument(true, true);
    }

    public static AbstractClientPlayer getPlayer(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        return context.getArgument(name, ClientEntitySelector.class).findSinglePlayer(context.getSource());
    }

    public static ClientEntityArgument players() {
        return new ClientEntityArgument(false, true);
    }

    public static Collection<AbstractClientPlayer> getPlayers(CommandContext<FabricClientCommandSource> context, String name) throws CommandSyntaxException {
        List<AbstractClientPlayer> list = context.getArgument(name, ClientEntitySelector.class).findPlayers(context.getSource());
        if (list.isEmpty()) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        } else {
            return list;
        }
    }

    public ClientEntitySelector parse(StringReader reader) throws CommandSyntaxException {
        int i = 0;
        EntitySelectorParser entitySelectorParser = new EntitySelectorParser(reader);
        ClientEntitySelector entitySelector = new ClientEntitySelector(entitySelectorParser.parse());
        if (entitySelector.getMaxResults() > 1 && this.single) {
            if (this.playersOnly) {
                reader.setCursor(0);
                throw EntityArgument.ERROR_NOT_SINGLE_PLAYER.createWithContext(reader);
            } else {
                reader.setCursor(0);
                throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.createWithContext(reader);
            }
        } else if (entitySelector.includesEntities() && this.playersOnly && !entitySelector.isSelfSelector()) {
            reader.setCursor(0);
            throw EntityArgument.ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(reader);
        } else {
            return entitySelector;
        }
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> commandContext, SuggestionsBuilder suggestionsBuilder) {
        S s = commandContext.getSource();
        if (s instanceof SharedSuggestionProvider sharedSuggestionProvider) {
            StringReader stringReader = new StringReader(suggestionsBuilder.getInput());
            stringReader.setCursor(suggestionsBuilder.getStart());
            EntitySelectorParser entitySelectorParser = new EntitySelectorParser(stringReader, sharedSuggestionProvider.hasPermission(2));

            try {
                entitySelectorParser.parse();
            } catch (CommandSyntaxException ignored) {
            }

            return entitySelectorParser.fillSuggestions(
                    suggestionsBuilder,
                    suggestionsBuilderx -> {
                        Collection<String> collection = sharedSuggestionProvider.getOnlinePlayerNames();
                        Iterable<String> iterable = this.playersOnly
                                ? collection
                                : Iterables.concat(collection, sharedSuggestionProvider.getSelectedEntities());
                        SharedSuggestionProvider.suggest(iterable, suggestionsBuilderx);
                    }
            );
        } else {
            return Suggestions.empty();
        }
    }

    @Override
    public Collection<String> getExamples() {
        return EXAMPLES;
    }

    public static class Info implements ArgumentTypeInfo<ClientEntityArgument, ClientEntityArgument.Info.Template> {
        private static final byte FLAG_SINGLE = 1;
        private static final byte FLAG_PLAYERS_ONLY = 2;

        public void serializeToNetwork(ClientEntityArgument.Info.Template template, FriendlyByteBuf buffer) {
            int i = 0;
            if (template.single) {
                i |= FLAG_SINGLE;
            }

            if (template.playersOnly) {
                i |= FLAG_PLAYERS_ONLY;
            }

            buffer.writeByte(i);
        }

        public ClientEntityArgument.Info.@NotNull Template deserializeFromNetwork(FriendlyByteBuf buffer) {
            byte b = buffer.readByte();
            return new ClientEntityArgument.Info.Template((b & FLAG_SINGLE) != 0, (b & FLAG_PLAYERS_ONLY) != 0);
        }

        public void serializeToJson(ClientEntityArgument.Info.Template template, JsonObject json) {
            json.addProperty("amount", template.single ? "single" : "multiple");
            json.addProperty("type", template.playersOnly ? "players" : "entities");
        }

        public ClientEntityArgument.Info.@NotNull Template unpack(ClientEntityArgument argument) {
            return new ClientEntityArgument.Info.Template(argument.single, argument.playersOnly);
        }

        public final class Template implements ArgumentTypeInfo.Template<ClientEntityArgument> {
            final boolean single;
            final boolean playersOnly;

            Template(boolean single, boolean playersOnly) {
                this.single = single;
                this.playersOnly = playersOnly;
            }

            public @NotNull ClientEntityArgument instantiate(CommandBuildContext context) {
                return new ClientEntityArgument(this.single, this.playersOnly);
            }

            @Override
            public @NotNull ArgumentTypeInfo<ClientEntityArgument, ?> type() {
                return Info.this;
            }
        }
    }
}
