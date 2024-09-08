package net.minestom.server.entity.attribute;

import net.minestom.server.registry.Registry;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

record AttributeImpl(@NotNull Registry.AttributeEntry registry) implements Attribute {
    private static final Registry.Container<Attribute> CONTAINER = Registry.createStaticContainer(
            Registry.loadRegistry(Registry.Resource.ATTRIBUTES, Registry.AttributeEntry::new).stream().<Attribute>map(AttributeImpl::new).toList());

    static Attribute get(@NotNull String namespace) {
        return CONTAINER.get(namespace);
    }

    static Attribute getSafe(@NotNull String namespace) {
        return CONTAINER.getSafe(namespace);
    }

    static Attribute getId(int id) {
        return CONTAINER.getId(id);
    }

    static Collection<Attribute> values() {
        return CONTAINER.values();
    }

    @Override
    public String toString() {
        return name();
    }
}
