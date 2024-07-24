package net.minestom.server.potion;

import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.StaticProtocolObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public sealed interface PotionType extends StaticProtocolObject, PotionTypes permits PotionTypeImpl {

    NetworkBuffer.Type<PotionType> NETWORK_TYPE = NetworkBuffer.VAR_INT.transform(PotionTypeImpl::getId, PotionType::id);

    static @NotNull Collection<@NotNull PotionType> values() {
        return PotionTypeImpl.values();
    }

    static @Nullable PotionType fromNamespaceId(@NotNull String namespaceID) {
        return PotionTypeImpl.getSafe(namespaceID);
    }

    static @Nullable PotionType fromNamespaceId(@NotNull Key namespaceID) {
        return fromNamespaceId(namespaceID.asString());
    }

    static @Nullable PotionType fromId(int id) {
        return PotionTypeImpl.getId(id);
    }
}
