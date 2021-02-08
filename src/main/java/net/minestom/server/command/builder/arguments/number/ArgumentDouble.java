package net.minestom.server.command.builder.arguments.number;

import net.minestom.server.MinecraftServer;
import net.minestom.server.command.builder.exception.ArgumentSyntaxException;
import net.minestom.server.network.packet.server.play.DeclareCommandsPacket;
import org.jetbrains.annotations.NotNull;

public class ArgumentDouble extends ArgumentNumber<Double> {

    public ArgumentDouble(String id) {
        super(id);
        this.min = Double.MIN_VALUE;
        this.max = Double.MAX_VALUE;
    }

    @NotNull
    @Override
    public Double parse(@NotNull String input) throws ArgumentSyntaxException {
        try {
            final double value;
            {
                String parsed = parseValue(input);
                int radix = getRadix(input);
                if (radix != 10) {
                    value = (double) Long.parseLong(parsed, radix);
                } else {
                    value = Double.parseDouble(parsed);
                }
            }

            // Check range
            if (hasMin && value < min) {
                throw new ArgumentSyntaxException("Input is lower than the minimum required value", input, RANGE_ERROR);
            }
            if (hasMax && value > max) {
                throw new ArgumentSyntaxException("Input is higher than the minimum required value", input, RANGE_ERROR);
            }

            return value;
        } catch (NumberFormatException | NullPointerException e) {
            throw new ArgumentSyntaxException("Input is not a number/long", input, NOT_NUMBER_ERROR);
        }
    }

    @NotNull
    @Override
    public DeclareCommandsPacket.Node[] toNodes(boolean executable) {
        DeclareCommandsPacket.Node argumentNode = MinecraftServer.getCommandManager().simpleArgumentNode(this, executable, false);

        argumentNode.parser = "brigadier:double";
        argumentNode.properties = packetWriter -> {
            packetWriter.writeByte(MinecraftServer.getCommandManager().getNumberProperties(this));
            if (this.hasMin())
                packetWriter.writeDouble(this.getMin());
            if (this.hasMax())
                packetWriter.writeDouble(this.getMax());
        };

        return new DeclareCommandsPacket.Node[]{argumentNode};
    }

}
