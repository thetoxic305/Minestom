package net.minestom.server.gamedata.tags;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.Material;
import net.minestom.server.registry.*;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Represents a group of items, blocks, fluids, entity types or function.
 * Immutable by design
 */
public final class Tag implements ProtocolObject, Keyed {
    private final Key key;
    private final Set<Key> values;

    /**
     * Creates a new empty tag. This does not cache the tag.
     */
    public Tag(@NotNull Key key) {
        this.key = key;
        this.values = new HashSet<>();
    }

    /**
     * Creates a new tag with the given values. This does not cache the tag.
     */
    public Tag(@NotNull Key key, @NotNull Set<Key> values) {
        this.key = key;
        this.values = new HashSet<>(values);
    }

    /**
     * Checks whether the given id in inside this tag.
     *
     * @param id the id to check against
     * @return 'true' iif this tag contains the given id
     */
    public boolean contains(@NotNull Key id) {
        return values.contains(id);
    }

    /**
     * Returns an immutable set of values present in this tag
     *
     * @return immutable set of values present in this tag
     */
    public @NotNull Set<Key> getValues() {
        return Collections.unmodifiableSet(values);
    }

    @Contract(pure = true)
    public @NotNull String name() {
        return key().asString();
    }

    @Override
    @Contract(pure = true)
    public @NotNull Key key() {
        return key;
    }

    @Override
    public String toString() {
        return "#" + name.asString();
    }

    public enum BasicType {
        BLOCKS("minecraft:block", Registry.Resource.BLOCK_TAGS,
                (name, registries) -> Objects.requireNonNull(Block.fromKey(name)).id()),
        ITEMS("minecraft:item", Registry.Resource.ITEM_TAGS,
                (name, registries) -> Objects.requireNonNull(Material.fromKey(name)).id()),
        FLUIDS("minecraft:fluid", Registry.Resource.FLUID_TAGS,
                (name, registries) -> FluidRegistries.getFluid(name).ordinal()),
        ENTITY_TYPES("minecraft:entity_type", Registry.Resource.ENTITY_TYPE_TAGS,
                (name, registries) -> Objects.requireNonNull(EntityType.fromKey(name)).id()),
        GAME_EVENTS("minecraft:game_event", Registry.Resource.GAMEPLAY_TAGS,
                (name, registries) -> FluidRegistries.getFluid(name).ordinal()),
        SOUND_EVENTS("minecraft:sound_event", null, null), // Seems not to be included in server data
        POTION_EFFECTS("minecraft:potion_effect", null, null), // Seems not to be included in server data

        //todo this is cursed. it does not update as the registry changes. Fix later.
        ENCHANTMENTS("minecraft:enchantment", Registry.Resource.ENCHANTMENT_TAGS,
                (name, registries) -> registries.enchantment().getId(DynamicRegistry.Key.of(name))),
        BIOMES("minecraft:worldgen/biome", Registry.Resource.BIOME_TAGS,
                (name, registries) -> registries.biome().getId(DynamicRegistry.Key.of(name)));

        private final static BasicType[] VALUES = values();
        private final String identifier;
        private final Registry.Resource resource;
        private final BiFunction<String, Registries, Integer> function;

        BasicType(@NotNull String identifier,
                  @Nullable Registry.Resource resource,
                  @Nullable BiFunction<String, Registries, Integer> function) {
            this.identifier = identifier;
            this.resource = resource;
            this.function = function;
        }

        public @NotNull String getIdentifier() {
            return identifier;
        }

        public Registry.Resource getResource() {
            return resource;
        }

        public BiFunction<String, Registries, Integer> getFunction() {
            return function;
        }

        public static @Nullable Tag.BasicType fromIdentifer(@NotNull String identifier) {
            for (BasicType value : VALUES) {
                if (value.identifier.equals(identifier)) {
                    return value;
                }
            }
            return null;
        }
    }
}
