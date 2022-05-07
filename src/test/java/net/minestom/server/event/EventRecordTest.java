package net.minestom.server.event;

import net.minestom.server.coordinate.Pos;
import net.minestom.server.event.trait.ItemEvent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventRecordTest {

    record ActionAt(Pos position) implements ItemEvent {
        @Override
        public @NotNull ItemStack getItemStack() {
            // FIXME: Will be removed, as events will not be responsible for the filters anymore
            throw new UnsupportedOperationException();
        }
    }

    @Test
    public void testCall() {
        var node = EventNode.all("main");
        ItemStack item = ItemStack.of(Material.DIAMOND);
        var action = new ActionAt(Pos.ZERO);
        var mutated = new ActionAt(new Pos(1, 1, 1));
        node.addListener(ActionAt.class, (context, event) -> {
            assertEquals(action, event);

            ItemStack filter = context.filter(EventFilter.ITEM);
            assertEquals(item, filter);
            assertEquals(0, context.executionCount());
            assertEquals(node, context.currentNode());
            context.mutate(mutated);
        });
        ActionAt result = node.call(EventContext.filters(Map.of(EventFilter.ITEM, item)), action);
        assertEquals(mutated, result);
    }

    @Test
    public void currentNode() {
        var node = EventNode.all("main");
        node.addListener(ActionAt.class, (context, event) -> {
            var current = context.currentNode();
            assertEquals(node, current);
        });
        node.call(EventContext.filters(Map.of()), new ActionAt(Pos.ZERO));
    }
}
