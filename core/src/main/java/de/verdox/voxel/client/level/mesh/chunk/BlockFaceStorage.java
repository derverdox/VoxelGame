package de.verdox.voxel.client.level.mesh.chunk;

import com.badlogic.gdx.math.Vector3;
import de.verdox.voxel.client.level.ClientWorld;
import de.verdox.voxel.client.level.mesh.block.BlockFace;
import de.verdox.voxel.shared.util.Direction;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import lombok.Getter;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Getter
public class BlockFaceStorage implements Iterable<BlockFace> {
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

    public void clear() {
        upFaces.clear();
        downFaces.clear();
        eastFaces.clear();
        westFaces.clear();
        southFaces.clear();
        northFaces.clear();
        size = 0;
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
     *
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
            int u = getUCoord(f, dir);
            int v = getVCoord(f, dir);
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
                    if (used[u][quadV] || !areCompatible(faceAtCoordinate, neighbor, dir)) {
                        break;
                    }
                    quadHeight++;
                    used[u][quadV] = true;
                }

                for (int quadU = u + 1; quadU < sizeU; quadU++) {
                    boolean canExtend = true;
                    for (int i = 0; i < quadHeight; i++) {
                        BlockFace neighbor = grid[quadU][v + i];
                        if (used[quadU][v + i] || !areCompatible(faceAtCoordinate, neighbor, dir)) {
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
                    } else {
                        System.out.println("Could not add " + faceAtCoordinate);
                    }
                }
            }
        }
        return merged;
    }

    /**
     * Builds a new BlockFace quad from the base face and dimensions,
     * correctly applying U/V offsets to position the merged quad,
     * and ensuring proper winding for normals.
     */
    public static BlockFace createQuad(BlockFace base, Direction dir,
                                       int u, int v, int w, int h) {
        // 1) Base face origin (corner1) already lies on the face plane
        float x = base.corner1X;
        float y = base.corner1Y;
        float z = base.corner1Z;

        // 2) Determine U and V axis vectors for this face direction
        Vector3 uVec, vVec;
        if (dir.getOffsetY() != 0) {
            // UP or DOWN => XZ plane
            uVec = new Vector3(1, 0, 0);
            vVec = new Vector3(0, 0, 1);
        } else if (dir.getOffsetX() != 0) {
            // EAST or WEST => YZ plane
            uVec = new Vector3(0, 0, 1);
            vVec = new Vector3(0, 1, 0);
        } else {
            // NORTH or SOUTH => XY plane
            uVec = new Vector3(1, 0, 0);
            vVec = new Vector3(0, 1, 0);
        }

        // 3) Compute quad origin shifted by (u,v) in grid units
        Vector3 origin = new Vector3(x, y, z)
            .mulAdd(uVec, u)
            .mulAdd(vVec, v);

        // 4) Compute the four corners of the merged quad
        Vector3 c1 = new Vector3(origin);
        Vector3 c2 = new Vector3(origin).mulAdd(uVec, w);
        Vector3 c3 = new Vector3(c2).mulAdd(vVec, h);
        Vector3 c4 = new Vector3(origin).mulAdd(vVec, h);

/*        // 6) Handle winding for UP/DOWN faces
        if (dir == Direction.UP) {
            // CCW when viewed from above: c1, c4, c3, c2
            return new BlockFace(
                c1x, c1y, c1z,
                c4x, c4y, c4z,
                c3x, c3y, c3z,
                c2x, c2y, c2z,
                dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ(),
                base.textureId
            );
        } else if (dir == Direction.DOWN) {
            // CCW when viewed from below: c1, c2, c3, c4
            return new BlockFace(
                c1x, c1y, c1z,
                c2x, c2y, c2z,
                c3x, c3y, c3z,
                c4x, c4y, c4z,
                dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ(),
                base.textureId
            );
        }*/

        // 7) Default CCW for other directions: c1, c2, c3, c4
        return new BlockFace(
            base.blockXInChunk, base.blockYInChunk, base.blockZInChunk,
            c1.x, c1.y, c1.z,
            c2.x, c2.y, c2.z,
            c3.x, c3.y, c3.z,
            c4.x, c4.y, c4.z,
            dir.getOffsetX(), dir.getOffsetY(), dir.getOffsetZ(),
            base.textureId
        );
    }

    /**
     * Extracts U coordinate index from a face based on direction.
     */
    public static int getUCoord(BlockFace f, Direction dir) {
        return switch (dir) {
            case UP, DOWN, NORTH, SOUTH -> f.getLocalX();
            case EAST, WEST -> f.getLocalZ();
        };
    }

    /**
     * Extracts V coordinate index from a face based on direction.
     */
    public static int getVCoord(BlockFace f, Direction dir) {
        return switch (dir) {
            case UP, DOWN -> f.getLocalZ();
            case EAST, WEST, NORTH, SOUTH -> f.getLocalY();
        };
    }

    /**
     * Checks whether two BlockFaces are eligible for merging:
     * same texture and same normal.
     */
    private boolean areCompatible(BlockFace a, BlockFace b, Direction dir) {
        if (a == null || b == null) {
            return false;
        }

        return a.textureId.equals(b.textureId)
            && a.normalX == b.normalX
            && a.normalY == b.normalY
            && a.normalZ == b.normalZ && getWCoord(a, dir) == getWCoord(b, dir);
    }

    private int getWCoord(BlockFace f, Direction dir) {
        return (int) (f.corner1X * dir.getOffsetX()
            + f.corner1Y * dir.getOffsetY()
            + f.corner1Z * dir.getOffsetZ());
    }


    public int size() {
        return size;
    }

    public void addFace(BlockFace rawBlockFace) {
        Direction direction = Direction.fromOffsets((int) rawBlockFace.normalX, (int) rawBlockFace.normalY, (int) rawBlockFace.normalZ);
        int w = getWCoord(rawBlockFace, direction);

        var map = getByDirection(direction);
        if (map == null) {
            return;
        }

        map.computeIfAbsent(w, integer -> new ArrayList<>(1024)).add(rawBlockFace);
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
