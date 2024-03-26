package io.github.lucaargolo.exporter.arguments;

import com.google.common.collect.Lists;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.advancements.critereon.MinMaxBounds;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.util.AbortableIterationConsumer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class ClientEntitySelector {
    public static final BiConsumer<Vec3, List<? extends Entity>> ORDER_ARBITRARY = (vec3, list) -> {};
    private final int maxResults;
    private final boolean includesEntities;
    private final Predicate<Entity> predicate;
    private final MinMaxBounds.Doubles range;
    private final Function<Vec3, Vec3> position;
    @Nullable
    private final AABB aabb;
    private final BiConsumer<Vec3, List<? extends Entity>> order;
    private final boolean currentEntity;
    @Nullable
    private final String playerName;
    @Nullable
    private final UUID entityUUID;
    private final EntityTypeTest<Entity, ?> type;

    public ClientEntitySelector(EntitySelector selector) {
        this.maxResults = selector.maxResults;
        this.includesEntities = selector.includesEntities;
        this.predicate = selector.predicate;
        this.range = selector.range;
        this.position = selector.position;
        this.aabb = selector.aabb;
        this.order = selector.order;
        this.currentEntity = selector.currentEntity;
        this.playerName = selector.playerName;
        this.entityUUID = selector.entityUUID;
        this.type = selector.type;
    }

    public int getMaxResults() {
        return this.maxResults;
    }

    public boolean includesEntities() {
        return this.includesEntities;
    }

    public boolean isSelfSelector() {
        return this.currentEntity;
    }

    public Entity findSingleEntity(FabricClientCommandSource source) throws CommandSyntaxException {
        List<? extends Entity> list = this.findEntities(source);
        if (list.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
        } else if (list.size() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
        } else {
            return list.get(0);
        }
    }

    public List<? extends Entity> findEntities(FabricClientCommandSource source) throws CommandSyntaxException {
        return this.findEntitiesRaw(source).stream().filter(entity -> entity.getType().isEnabled(source.enabledFeatures())).toList();
    }

    private List<? extends Entity> findEntitiesRaw(FabricClientCommandSource source) throws CommandSyntaxException {
        if (!this.includesEntities) {
            return this.findPlayers(source);
        } else if (this.playerName != null) {
            AbstractClientPlayer clientPlayer = source.getWorld().players().stream().filter(p -> p.getPlayerInfo() != null && p.getPlayerInfo().getProfile().getName().equals(this.playerName)).findFirst().orElse(null);
            return clientPlayer == null ? Collections.emptyList() : Lists.newArrayList(clientPlayer);
        } else if (this.entityUUID != null) {
            Entity entity = source.getWorld().getEntities().get(this.entityUUID);
            if (entity != null) {
                return Lists.newArrayList(entity);
            }


            return Collections.emptyList();
        } else {
            Vec3 vec3 = this.position.apply(source.getPosition());
            Predicate<Entity> predicate = this.getPredicate(vec3);
            if (this.currentEntity) {
                return source.getEntity() != null && predicate.test(source.getEntity())
                        ? Lists.newArrayList(source.getEntity())
                        : Collections.emptyList();
            } else {
                List<Entity> list = Lists.<Entity>newArrayList();
                this.addEntities(list, source.getWorld(), vec3, predicate);
                return this.sortAndLimit(vec3, list);
            }
        }
    }

    /**
     * Gets all entities matching this selector, and adds them to the passed list.
     */
    private void addEntities(List<Entity> result, ClientLevel level, Vec3 pos, Predicate<Entity> predicate) {
        int i = this.getResultLimit();
        if (result.size() < i) {
            if (this.aabb != null) {
                level.getEntities(this.type, this.aabb.move(pos), predicate, result, i);
            } else {
                level.getEntities().get(this.type, (entity) -> {
                    if (predicate.test(entity)) {
                        result.add(entity);
                        if (result.size() >= i) {
                            return AbortableIterationConsumer.Continuation.ABORT;
                        }
                    }

                    return AbortableIterationConsumer.Continuation.CONTINUE;
                });
            }
        }
    }

    private int getResultLimit() {
        return this.order == ORDER_ARBITRARY ? this.maxResults : Integer.MAX_VALUE;
    }

    public AbstractClientPlayer findSinglePlayer(FabricClientCommandSource source) throws CommandSyntaxException {
        List<AbstractClientPlayer> list = this.findPlayers(source);
        if (list.size() != 1) {
            throw EntityArgument.NO_PLAYERS_FOUND.create();
        } else {
            return list.get(0);
        }
    }

    public List<AbstractClientPlayer> findPlayers(FabricClientCommandSource source) throws CommandSyntaxException {
        if (this.playerName != null) {
            AbstractClientPlayer clientPlayer = source.getWorld().players().stream().filter(p -> p.getPlayerInfo() != null && p.getPlayerInfo().getProfile().getName().equals(this.playerName)).findFirst().orElse(null);
            return clientPlayer == null ? Collections.emptyList() : Lists.newArrayList(clientPlayer);
        } else if (this.entityUUID != null) {
            AbstractClientPlayer clientPlayer = source.getWorld().players().stream().filter(p -> p.getUUID().equals(this.entityUUID)).findFirst().orElse(null);
            return clientPlayer == null ? Collections.emptyList() : Lists.newArrayList(clientPlayer);
        } else {
            Vec3 vec3 = this.position.apply(source.getPosition());
            Predicate<Entity> predicate = this.getPredicate(vec3);
            if (this.currentEntity) {
                if (source.getEntity() instanceof AbstractClientPlayer clientPlayer2 && predicate.test(clientPlayer2)) {
                    return Lists.newArrayList(clientPlayer2);
                }

                return Collections.emptyList();
            } else {
                int i = this.getResultLimit();
                List<AbstractClientPlayer> list = Lists.newArrayList();

                for(AbstractClientPlayer clientPlayer3 : source.getWorld().players()) {
                    if (predicate.test(clientPlayer3)) {
                        list.add(clientPlayer3);
                        if (list.size() >= i) {
                            return list;
                        }
                    }
                }

                return this.sortAndLimit(vec3, list);
            }
        }
    }

    /**
     * Returns a modified version of the predicate on this selector that also checks the AABB and distance.
     */
    private Predicate<Entity> getPredicate(Vec3 pos) {
        Predicate<Entity> predicate = this.predicate;
        if (this.aabb != null) {
            AABB aABB = this.aabb.move(pos);
            predicate = predicate.and(entity -> aABB.intersects(entity.getBoundingBox()));
        }

        if (!this.range.isAny()) {
            predicate = predicate.and(entity -> this.range.matchesSqr(entity.distanceToSqr(pos)));
        }

        return predicate;
    }

    private <T extends Entity> List<T> sortAndLimit(Vec3 pos, List<T> entities) {
        if (entities.size() > 1) {
            this.order.accept(pos, entities);
        }

        return entities.subList(0, Math.min(this.maxResults, entities.size()));
    }

}
