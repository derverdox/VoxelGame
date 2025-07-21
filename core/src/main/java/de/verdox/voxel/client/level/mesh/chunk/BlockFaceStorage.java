package de.verdox.voxel.client.level.mesh.chunk;

import de.verdox.voxel.client.level.mesh.block.face.BlockFace;
import de.verdox.voxel.shared.util.Direction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;

@Getter
public class BlockFaceStorage implements Iterable<BlockFace> {
    // TODO: RAM OPTIMIZATION
    //  Too much ram used when many Block Faces are created
    private final Int2ObjectMap<List<BlockFace>> upFaces = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<List<BlockFace>> downFaces = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<List<BlockFace>> eastFaces = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<List<BlockFace>> westFaces = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<List<BlockFace>> southFaces = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<List<BlockFace>> northFaces = new Int2ObjectOpenHashMap<>();

    private final int scaleX;
    private final int scaleY;
    private final int scaleZ;

    private int size = 0;
    @Getter
    private int vertices, indices;

    public void clear() {
        upFaces.clear();
        downFaces.clear();
        eastFaces.clear();
        westFaces.clear();
        southFaces.clear();
        northFaces.clear();
        size = 0;
        vertices = 0;
        indices = 0;
    }

    public BlockFaceStorage(int scaleX, int scaleY, int scaleZ) {
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.scaleZ = scaleZ;
    }

    public Int2ObjectMap<List<BlockFace>> getByDirection(Direction direction) {
        return switch (direction) {
            case WEST -> westFaces;
            case EAST -> eastFaces;
            case DOWN -> downFaces;
            case UP -> upFaces;
            case NORTH -> northFaces;
            case SOUTH -> southFaces;
        };
    }

    /**
     * Applies the greedy meshing algorithm to all face lists, merging adjacent faces
     * of the same direction, texture, and orientation into larger quads.
     */
    private final ExecutorService greedyMeshingService = Executors.newThreadPerTaskExecutor(Thread.ofVirtual().name("Greedy Meshing Calculator Job - %d", 0).factory());

    public BlockFaceStorage applyGreedyMeshing() {
        BlockFaceStorage newStorage = new BlockFaceStorage(this.scaleX, this.scaleY, this.scaleZ);

        Set<Future<List<BlockFace>>> futures = new HashSet<>();
        for (Direction direction : Direction.values()) {
            var map = getByDirection(direction);
            if (map == null) {
                continue;
            }
            for (List<BlockFace> blockFacesInSlice : map.values()) {
                //Future<List<BlockFace>> futureBlockFaces = greedyMeshingService.submit(() -> merge2D(blockFacesInSlice, direction, getSizeU(direction), getSizeV(direction)));
                Future<List<BlockFace>> futureBlockFaces = CompletableFuture.completedFuture(merge2D(blockFacesInSlice, direction, getSizeU(direction), getSizeV(direction)));
                futures.add(futureBlockFaces);
            }
        }


        try {
            for (Future<List<BlockFace>> future : futures) {
                for (BlockFace blockFace : future.get()) {
                    newStorage.addFace(blockFace);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException("Fehler beim parallelen Meshen", e);
        }
        return newStorage;
    }

    private int getSizeU(Direction direction) {
        return switch (direction) {
            case WEST, EAST -> this.scaleY;
            case DOWN, UP, NORTH, SOUTH -> this.scaleX;
        };
    }

    private int getSizeV(Direction direction) {
        return switch (direction) {
            case WEST, EAST, DOWN, UP -> this.scaleZ;
            case NORTH, SOUTH -> this.scaleY;
        };
    }

    private List<BlockFace> merge2D(List<BlockFace> faces, Direction dir, int sizeU, int sizeV) {
        BlockFace[][] grid = new BlockFace[sizeU][sizeV];
        boolean[][] used = new boolean[sizeU][sizeV];

        List<BlockFace> merged = new ArrayList<>();

        for (BlockFace f : faces) {
            int u = f.getUCoord(dir);
            int v = f.getVCoord(dir);
            grid[u][v] = f;
        }

        for (int u = 0; u < sizeU; u++) {
            for (int v = 0; v < sizeV; v++) {
                if (used[u][v]) {
                    continue;
                }

                BlockFace faceAtCoordinate = grid[u][v];
                used[u][v] = true;
                if (faceAtCoordinate == null) {
                    continue;
                }

                int quadHeight = 1;
                int quadLength = 1;

                for (int quadV = v + 1; quadV < sizeV; quadV++) {

                    BlockFace neighbor = grid[u][quadV];
                    if (used[u][quadV] || !faceAtCoordinate.isMergeable(neighbor, dir)) {
                        break;
                    }
                    quadHeight++;
                    used[u][quadV] = true;
                }

                for (int quadU = u + 1; quadU < sizeU; quadU++) {
                    boolean canExtend = true;
                    for (int i = 0; i < quadHeight; i++) {
                        BlockFace neighbor = grid[quadU][v + i];
                        if (used[quadU][v + i] || !faceAtCoordinate.isMergeable(neighbor, dir)) {
                            canExtend = false;
                            break;
                        }
                    }

                    if (!canExtend) {
                        break;
                    }

                    quadLength++;
                    for (int i = 0; i < quadHeight; i++) {
                        used[quadU][v + i] = true;
                    }
                }

                if (quadHeight == 1 && quadLength == 1) {
                    merged.add(faceAtCoordinate);
                } else {
                    if (dir.equals(Direction.UP)) {
                        BlockFace newFace = faceAtCoordinate.expandU(quadLength - 1).expandVBackward(quadHeight - 1);
                        merged.add(newFace);
                    } else if (dir.equals(Direction.DOWN)) {
                        BlockFace newFace = faceAtCoordinate.expandU(quadLength - 1).expandV(quadHeight - 1);
                        merged.add(newFace);
                    } else if (dir.equals(Direction.EAST)) {
                        BlockFace newFace = faceAtCoordinate.expandUBackward(quadLength - 1).expandV(quadHeight - 1);
                        merged.add(newFace);
                    } else if (dir.equals(Direction.WEST)) {
                        BlockFace newFace = faceAtCoordinate.expandU(quadLength - 1).expandV(quadHeight - 1);
                        merged.add(newFace);
                    } else if (dir.equals(Direction.NORTH)) {
                        BlockFace newFace = faceAtCoordinate.expandUBackward(quadLength - 1).expandV(quadHeight - 1);
                        merged.add(newFace);
                    } else if (dir.equals(Direction.SOUTH)) {
                        BlockFace newFace = faceAtCoordinate.expandU(quadLength - 1).expandV(quadHeight - 1);
                        merged.add(newFace);
                    }
                }
            }
        }
        return merged;
    }


    public int size() {
        return size;
    }

    public synchronized void addFace(BlockFace rawBlockFace) {
        Direction direction = rawBlockFace.getDirection();
        int w = rawBlockFace.getWCord(direction);

        var map = getByDirection(direction);
        if (map == null) {
            return;
        }

        map.computeIfAbsent(w, integer -> new ArrayList<>(1024)).add(rawBlockFace);
        vertices += rawBlockFace.getFloatsPerVertex() * rawBlockFace.getVerticesPerFace();
        indices += rawBlockFace.getIndicesPerFace();
        size++;
    }

    private class FaceIterator implements Iterator<BlockFace> {
        private final List<Iterator<BlockFace>> iterators;
        private int current = 0;

        FaceIterator() {
            iterators = new ArrayList<>(36);

            for (List<BlockFace> value : upFaces.values()) {
                iterators.add(value.iterator());
            }

            for (List<BlockFace> value : downFaces.values()) {
                iterators.add(value.iterator());
            }

            for (List<BlockFace> value : eastFaces.values()) {
                iterators.add(value.iterator());
            }

            for (List<BlockFace> value : westFaces.values()) {
                iterators.add(value.iterator());
            }

            for (List<BlockFace> value : southFaces.values()) {
                iterators.add(value.iterator());
            }

            for (List<BlockFace> value : northFaces.values()) {
                iterators.add(value.iterator());
            }
        }

        @Override
        public boolean hasNext() {
            // Solange der aktuelle Iterator erschöpft ist, zum nächsten wechseln
            while (current < iterators.size()) {
                if (iterators.get(current).hasNext()) {
                    return true;
                }
                current++;
            }
            return false;
        }

        @Override
        public BlockFace next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return iterators.get(current).next();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Removal not supported");
        }
    }

    @Override
    public Iterator<BlockFace> iterator() {
        return new FaceIterator();
    }
}
