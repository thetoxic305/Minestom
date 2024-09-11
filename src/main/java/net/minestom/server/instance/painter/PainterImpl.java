package net.minestom.server.instance.painter;

import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.palette.Palette;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static net.minestom.server.utils.chunk.ChunkUtils.getChunkCoordinate;
import static net.minestom.server.utils.chunk.ChunkUtils.toSectionRelativeCoordinate;

record PainterImpl(List<Instruction> instructions) implements Painter {
    public PainterImpl {
        instructions = List.copyOf(instructions);
    }

    static PainterImpl paint(Consumer<ReadableWorld> consumer) {
        WorldImpl world = new WorldImpl();
        consumer.accept(world);
        return new PainterImpl(world.instructions);
    }

    @Override
    public Palette sectionAt(int sectionX, int sectionY, int sectionZ) {
        return sectionAt(instructions, sectionX, sectionY, sectionZ);
    }

    record Bounds(Vec min, Vec max) {
    }

    sealed interface Instruction {
        record SetBlock(int x, int y, int z, Block block) implements Instruction {
        }

        record Cuboid(Vec min, Vec max, Block block) implements Instruction {
        }

        record Fill(Block block) implements Instruction {
        }

        record AreaOperation(Area area, PreparedOperation operation) implements Instruction {
        }
    }

    static final class WorldImpl implements ReadableWorld {
        private final List<Instruction> instructions = new ArrayList<>();

        @Override
        public @UnknownNullability Block getBlock(int x, int y, int z, @NotNull Condition condition) {
            final Palette palette = sectionAt(instructions, getChunkCoordinate(x), getChunkCoordinate(y), getChunkCoordinate(z));
            final int stateId = palette.get(toSectionRelativeCoordinate(x), toSectionRelativeCoordinate(y), toSectionRelativeCoordinate(z));
            return Block.fromStateId(stateId);
        }

        @Override
        public void setBlock(int x, int y, int z, @NotNull Block block) {
            append(new Instruction.SetBlock(x, y, z, block));
        }

        @Override
        public void cuboid(Point min, Point max, Block block) {
            append(new Instruction.Cuboid(Vec.fromPoint(min), Vec.fromPoint(max), block));
        }

        @Override
        public void fill(Block block) {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public void every(Area area, Operation operation) {
            PreparedOperation prepared = PreparedOperation.compile(operation);
            if (prepared == null) return;
            append(new Instruction.AreaOperation(area, prepared));
        }

        void append(Instruction instruction) {
            instructions.add(instruction);
        }
    }

    static void applyInstruction(int sectionX, int sectionY, int sectionZ, Palette palette,
                                 Point offset, Bounds bounds, Instruction instruction) {
        if (!sectionRelevant(instruction, sectionX, sectionY, sectionZ, offset)) return;

        final int minSectionX = sectionX * Chunk.CHUNK_SECTION_SIZE;
        final int maxSectionX = minSectionX + Chunk.CHUNK_SECTION_SIZE;
        final int minSectionY = sectionY * Chunk.CHUNK_SECTION_SIZE;
        final int maxSectionY = minSectionY + Chunk.CHUNK_SECTION_SIZE;
        final int minSectionZ = sectionZ * Chunk.CHUNK_SECTION_SIZE;
        final int maxSectionZ = minSectionZ + Chunk.CHUNK_SECTION_SIZE;

        switch (instruction) {
            case Instruction.SetBlock setBlock -> {
                final int absX = setBlock.x() + offset.blockX();
                final int absY = setBlock.y() + offset.blockY();
                final int absZ = setBlock.z() + offset.blockZ();
                if (absX < minSectionX || absX >= maxSectionX ||
                        absY < minSectionY || absY >= maxSectionY ||
                        absZ < minSectionZ || absZ >= maxSectionZ) return;
                final int localX = toSectionRelativeCoordinate(absX);
                final int localY = toSectionRelativeCoordinate(absY);
                final int localZ = toSectionRelativeCoordinate(absZ);
                palette.set(localX, localY, localZ, setBlock.block().stateId());
            }
            case Instruction.Cuboid cuboid -> {
                for (int x = cuboid.min().blockX(); x < cuboid.max().blockX(); x++) {
                    for (int y = cuboid.min().blockY(); y < cuboid.max().blockY(); y++) {
                        for (int z = cuboid.min().blockZ(); z < cuboid.max().blockZ(); z++) {
                            Block block = cuboid.block();

                            if (x < minSectionX || x >= maxSectionX ||
                                    y < minSectionY || y >= maxSectionY ||
                                    z < minSectionZ || z >= maxSectionZ) continue;

                            final int localX = toSectionRelativeCoordinate(x);
                            final int localY = toSectionRelativeCoordinate(y);
                            final int localZ = toSectionRelativeCoordinate(z);
                            palette.set(localX, localY, localZ, block.stateId());
                        }
                    }
                }
            }
            case Instruction.Fill fill -> {
                assert bounds != null;
                final Point min = bounds.min();
                final Point max = bounds.max();
                final Block block = fill.block();
                for (int x = min.blockX(); x <= max.blockX(); x++) {
                    for (int y = min.blockY(); y <= max.blockY(); y++) {
                        for (int z = min.blockZ(); z <= max.blockZ(); z++) {
                            if (x < minSectionX || x >= maxSectionX ||
                                    y < minSectionY || y >= maxSectionY ||
                                    z < minSectionZ || z >= maxSectionZ) continue;
                            final int localX = toSectionRelativeCoordinate(x);
                            final int localY = toSectionRelativeCoordinate(y);
                            final int localZ = toSectionRelativeCoordinate(z);
                            palette.set(localX, localY, localZ, block.stateId());
                        }
                    }
                }
            }
            case Instruction.AreaOperation areaOperation -> {
                final AreaImpl area = (AreaImpl) areaOperation.area();
                final PreparedOperation operation = areaOperation.operation();
                final HeightProvider heightProvider = area.heightProvider();
                final PosPredicate ratePredicate = area.ratePredicate();
                switch (area.type()) {
                    case COLUMN -> {
                        for (int x = minSectionX; x < maxSectionX; x++) {
                            for (int z = minSectionZ; z < maxSectionZ; z++) {
                                if (ratePredicate != null && !ratePredicate.test(x, 0, z)) continue;
                                final Bounds areaBounds;
                                final Vec columnOffset;
                                if (heightProvider != null) {
                                    final int height = area.heightProvider().test(x, z);
                                    final int minY = Math.min(minSectionY, height);
                                    final int maxY = Math.min(maxSectionY, height);
                                    areaBounds = new Bounds(new Vec(x, minY, z), new Vec(x, maxY, z));
                                    columnOffset = new Vec(x, height, z);
                                } else {
                                    areaBounds = new Bounds(new Vec(x, minSectionY, z), new Vec(x, maxSectionY, z));
                                    columnOffset = new Vec(x, 0, z);
                                }
                                for (Instruction opInstruction : operation.instructions()) {
                                    applyInstruction(sectionX, sectionY, sectionZ, palette,
                                            columnOffset, areaBounds, opInstruction);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    static Palette sectionAt(List<Instruction> instructions, int sectionX, int sectionY, int sectionZ) {
        Palette palette = Palette.blocks();
        for (Instruction instruction : instructions) {
            applyInstruction(sectionX, sectionY, sectionZ, palette, Vec.ZERO, null, instruction);
        }
        return palette;
    }

    static boolean sectionRelevant(Instruction instruction, int sectionX, int sectionY, int sectionZ, Point offset) {
        return switch (instruction) {
            case Instruction.SetBlock setBlock -> getChunkCoordinate(setBlock.x() + offset.blockX()) == sectionX &&
                    getChunkCoordinate(setBlock.y() + offset.blockY()) == sectionY &&
                    getChunkCoordinate(setBlock.z() + offset.blockZ()) == sectionZ;
            case Instruction.Cuboid cuboid -> {
                final Vec min = cuboid.min();
                final Vec max = cuboid.max();

                final int minX = getChunkCoordinate(min.blockX() + offset.blockX());
                final int minY = getChunkCoordinate(min.blockY() + offset.blockY());
                final int minZ = getChunkCoordinate(min.blockZ() + offset.blockZ());

                final int maxX = getChunkCoordinate(max.blockX() + offset.blockX());
                final int maxY = getChunkCoordinate(max.blockY() + offset.blockY());
                final int maxZ = getChunkCoordinate(max.blockZ() + offset.blockZ());

                yield sectionX >= minX && sectionX <= maxX &&
                        sectionY >= minY && sectionY <= maxY &&
                        sectionZ >= minZ && sectionZ <= maxZ;
            }
            case Instruction.Fill ignored -> true;
            case Instruction.AreaOperation ignored -> true;
        };
    }

    record AreaImpl(Type type,
                    HeightProvider heightProvider,
                    PosPredicate ratePredicate) implements Area {
        public AreaImpl(Type type) {
            this(type, null, null);
        }

        @Override
        public Area height(HeightProvider heightProvider) {
            return new AreaImpl(type, heightProvider, ratePredicate);
        }

        @Override
        public Area rate(PosPredicate predicate) {
            return new AreaImpl(type, heightProvider, predicate);
        }

        enum Type {
            COLUMN,
        }
    }
}
