package net.minestom.server.event;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface EventContext<E extends Event> {
    <T> @NotNull T filter(@NotNull EventFilter<? super E, T> filter);

    void mutate(@NotNull E event);

    @NotNull EventNode<? super E> currentNode();

    int executionCount();

    static <E extends Event> EventContext<E> filters(Map<EventFilter<? super E, ?>, ?> filters) {
        throw new UnsupportedOperationException();
    }
}
