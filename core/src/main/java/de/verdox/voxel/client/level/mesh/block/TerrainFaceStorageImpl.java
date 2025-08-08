package de.verdox.voxel.client.level.mesh.block;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import de.verdox.voxel.client.assets.TextureAtlasManager;
import de.verdox.voxel.client.level.mesh.block.face.BlockFace;
import de.verdox.voxel.client.level.mesh.block.face.GreedyBlockFace;
import de.verdox.voxel.client.level.mesh.block.face.SingleBlockFace;
import de.verdox.voxel.client.level.mesh.terrain.TerrainManager;
import de.verdox.voxel.client.util.LODUtil;
import de.verdox.voxel.client.util.RegionalLock;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.level.block.BlockModelType;
import de.verdox.voxel.shared.level.chunk.Chunk;
import de.verdox.voxel.shared.util.Direction;
import de.verdox.voxel.shared.util.RegionBounds;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortMap;
import it.unimi.dsi.fastutil.shorts.Short2ShortOpenHashMap;
import lombok.Getter;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

public class TerrainFaceStorageImpl implements TerrainFaceStorage {
    @Getter
    private final TerrainManager terrainManager;
    private final Long2ObjectMap<ChunkFaces> chunkFacesInRegion = new Long2ObjectOpenHashMap<>();
    private final int lodLevel;
    @Getter
    private final RegionalLock regionalLock = new RegionalLock();

    public TerrainFaceStorageImpl(TerrainManager terrainManager, byte lodLevel) {
        this.terrainManager = terrainManager;
        this.lodLevel = lodLevel;
    }

    private RegionBounds getBounds() {
        return terrainManager.getBounds();
    }

    @Override
    public int getScaleX() {
        return terrainManager.getWorld().getChunkSizeX() * getBounds().regionSizeX() / LODUtil.getLodScale(lodLevel);
    }

    @Override
    public int getScaleY() {
        return terrainManager.getWorld().getChunkSizeY() * getBounds().regionSizeY() / LODUtil.getLodScale(lodLevel);
    }

    @Override
    public int getScaleZ() {
        return terrainManager.getWorld().getChunkSizeZ() * getBounds().regionSizeZ() / LODUtil.getLodScale(lodLevel);
    }

    @Override
    public int getAmountFloats() {
        return chunkFacesInRegion.values().stream().mapToInt(value -> value.floatCount.get()).sum();
    }

    @Override
    public int getAmountIndices() {
        return chunkFacesInRegion.values().stream().mapToInt(value -> value.indexCount.get()).sum();
    }

    @Override
    public int getSize() {
        return chunkFacesInRegion.values().stream().mapToInt(value -> value.sizeCount.get()).sum();
    }

    @Override
    public TerrainFaceStorage createGreedyMeshedCopy(int lodLevel) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChunkFaces getOrCreateChunkFaces(int chunkCoordinateInRegionX, int chunkCoordinateInRegionY, int chunkCoordinateInRegionZ) {
        long offsetKey = computeOffsetKey(chunkCoordinateInRegionX, chunkCoordinateInRegionY, chunkCoordinateInRegionZ);

        ChunkFaces chunkFaces;
        if (!chunkFacesInRegion.containsKey(offsetKey)) {
            chunkFaces = new ChunkFaces();
            chunkFacesInRegion.put(offsetKey, chunkFaces);
        } else {
            chunkFaces = chunkFacesInRegion.get(offsetKey);
        }
        return chunkFaces;
    }

    @Override
    public void forEachChunkFace(ChunkFaceStorageConsumer consumer) {
        getRegionalLock().withMasterLock(() -> {
            for (Long2ObjectMap.Entry<ChunkFaces> chunkFacesEntry : this.chunkFacesInRegion.long2ObjectEntrySet()) {
                long offsetKey = chunkFacesEntry.getLongKey();

                int offsetX = Chunk.unpackChunkX(offsetKey) * terrainManager.getWorld().getChunkSizeX();
                int offsetY = Chunk.unpackChunkY(offsetKey) * terrainManager.getWorld().getChunkSizeY();
                int offsetZ = Chunk.unpackChunkZ(offsetKey) * terrainManager.getWorld().getChunkSizeZ();

                ChunkFaces chunkFaces = chunkFacesEntry.getValue();
                consumer.consume(chunkFaces, offsetX, offsetY, offsetZ);
            }
        });
    }

    @Override
    public void collectFaces(float[] vertices, int[] indices, byte lodLevel, TextureAtlas textureAtlas) {
        int vertexOffset = 0;
        int indexOffset = 0;
        int baseVertex = 0;
        int amountVertices = 0;

        for (Long2ObjectMap.Entry<ChunkFaces> chunkFacesEntry : this.chunkFacesInRegion.long2ObjectEntrySet()) {
            long offsetKey = chunkFacesEntry.getLongKey();
            ChunkFaces value = chunkFacesEntry.getValue();

            int offsetX = Chunk.unpackChunkX(offsetKey) * terrainManager.getWorld().getChunkSizeX();
            int offsetY = Chunk.unpackChunkY(offsetKey) * terrainManager.getWorld().getChunkSizeY();
            int offsetZ = Chunk.unpackChunkZ(offsetKey) * terrainManager.getWorld().getChunkSizeZ();

            for (BlockFace face : value) {
                face.appendToBuffers(vertices, null, indices, vertexOffset, indexOffset, baseVertex, textureAtlas, (byte) lodLevel, offsetX, offsetY, offsetZ);

                vertexOffset += face.getVerticesPerFace() * face.getFloatsPerVertex();
                indexOffset += face.getIndicesPerFace();
                baseVertex += face.getVerticesPerFace();
                amountVertices += face.getVerticesPerFace();
            }
        }
    }

    @Override
    public boolean hasFacesForChunk(int chunkCoordinateInRegionX, int chunkCoordinateInRegionY, int chunkCoordinateInRegionZ) {
        long offsetKey = Chunk.computeChunkKey(chunkCoordinateInRegionX, chunkCoordinateInRegionY, chunkCoordinateInRegionZ);
        return chunkFacesInRegion.containsKey(offsetKey);
    }

    public class ChunkFaces implements ChunkFaceStorage, Iterable<BlockFace> {
        private final Object2ObjectMap<Direction, FacesOfDirection> directions = new Object2ObjectOpenHashMap<>(6);

        private final AtomicInteger floatCount = new AtomicInteger();
        private final AtomicInteger sizeCount = new AtomicInteger();
        private final AtomicInteger indexCount = new AtomicInteger();

        public ChunkFaces() {
        }

        public void addBlockFace(BlockFace blockFace) {
            FacesOfDirection facesOfDirection = directions.getOrDefault(blockFace.getDirection(), null);

            if (facesOfDirection == null) {
                synchronized (directions) {
                    facesOfDirection = directions.getOrDefault(blockFace.getDirection(), null);
                    if (facesOfDirection == null) {
                        facesOfDirection = new FacesOfDirection(blockFace.getDirection());
                        directions.put(blockFace.getDirection(), facesOfDirection);
                    }
                }
            }
            facesOfDirection.addBlockFace(blockFace);
        }

        public void removeBlockFace(Direction direction, short u, short v, short w) {
            if (!directions.containsKey(direction)) {
                return;
            }
            FacesOfDirection facesOfDirection = directions.get(direction);
            facesOfDirection.removeBlockFace(u, v, w);

            if (facesOfDirection.isEmpty()) {
                synchronized (this) {
                    if (facesOfDirection.isEmpty()) directions.remove(direction);
                }
            }
        }

        public boolean isEmpty() {
            return directions.isEmpty();
        }

        @Override
        public void forEachFace(BlockFacesConsumer consumer) {
            for (FacesOfDirection value : directions.values()) {
                value.forEachFace(consumer);
            }
        }

        @Override
        public void collectFaces(float[] vertices, int[] indices, byte lodLevel, TextureAtlas textureAtlas, int offsetXInBlocks, int offsetYInBlocks, int offsetZInBlocks) {
            int vertexOffset = 0;
            int indexOffset = 0;
            int baseVertex = 0;

            for (ChunkFaces value : chunkFacesInRegion.values()) {
                for (BlockFace face : value) {
                    face.appendToBuffers(vertices, null, indices, vertexOffset, indexOffset, baseVertex, TextureAtlasManager.getInstance().getBlockTextureAtlas(), lodLevel, offsetXInBlocks, offsetYInBlocks, offsetZInBlocks);

                    vertexOffset += face.getVerticesPerFace() * face.getFloatsPerVertex();
                    indexOffset += face.getIndicesPerFace();
                    baseVertex += face.getVerticesPerFace();
                }
            }
        }

        @Override
        public int getAmountFloats() {
            return floatCount.get();
        }

        @Override
        public int getAmountVertices() {
            return 0;
        }

        @Override
        public int getAmountIndices() {
            return indexCount.get();
        }

        @Override
        public int getSize() {
            return sizeCount.get();
        }

        @Override
        public Iterator<BlockFace> iterator() {
            return directions.values().stream().flatMap(blockFaces -> blockFaces.slicesPerW.values().stream()
                            .flatMap(blockFaces1 -> blockFaces1.blockFaces
                                    .values().stream()))
                    .iterator();
        }

        private class FacesOfDirection implements Iterable<BlockFace> {
            private final Direction direction;
            private final Short2ObjectMap<FaceSlice> slicesPerW = new Short2ObjectOpenHashMap<>();

            public FacesOfDirection(Direction direction) {
                this.direction = direction;
            }

            public void addBlockFace(BlockFace blockFace) {
                short w = (short) blockFace.getWCoord(direction);

                FaceSlice faceSlice = slicesPerW.getOrDefault(w, null);

                if (faceSlice == null) {
                    synchronized (slicesPerW) {
                        faceSlice = slicesPerW.getOrDefault(w, null);
                        if (faceSlice == null) {
                            faceSlice = new FaceSlice(w);
                            slicesPerW.put(w, faceSlice);
                        }
                    }
                }
                faceSlice.setBlockFace(blockFace);
            }

            public void removeBlockFace(short u, short v, short w) {
                FaceSlice faceSlice = slicesPerW.getOrDefault(w, null);

                if (faceSlice == null) {
                    return;
                }
                faceSlice.removeBlockFace(u, v);

                if (faceSlice.isEmpty()) {
                    synchronized (slicesPerW) {
                        if (faceSlice.isEmpty()) slicesPerW.remove(w);
                    }
                }
            }

            public void forEachFace(BlockFacesConsumer consumer) {
                for (FaceSlice value : slicesPerW.values()) {

                    for (Short2ShortMap.Entry entry : value.facesInSlice.short2ShortEntrySet()) {
                        short packedCoordinate = entry.getShortKey();
                        short faceIndex = entry.getShortValue();

                        int u = value.extractU(packedCoordinate);
                        int v = value.extractV(packedCoordinate);

                        BlockFace blockFace = value.blockFaces.get(faceIndex);

                        consumer.consume(
                                blockFace,
                                blockFace.getBlockXInChunk(), blockFace.getBlockYInChunk(), blockFace.getBlockZInChunk()
                        );
                    }
                }
            }

            public boolean isEmpty() {
                return slicesPerW.isEmpty();
            }

            @Override
            public Iterator<BlockFace> iterator() {
                return slicesPerW.values().stream().flatMap(blockFaces -> blockFaces.blockFaces.values().stream())
                        .iterator();
            }

            @Getter
            private class FaceSlice implements Iterable<BlockFace> {
                private short w;
                private final Short2ShortMap facesInSlice = new Short2ShortOpenHashMap();
                private final Short2ObjectMap<BlockFace> blockFaces = new Short2ObjectOpenHashMap<>();
                private short idCounter = Short.MIN_VALUE;

                public FaceSlice(short w) {
                    this.w = w;
                }

                public boolean isEmpty() {
                    return facesInSlice.isEmpty();
                }

                public void setBlockFace(BlockFace blockFace) {
                    short freshId = ++idCounter;

                    int u = blockFace.getUCoord(direction);
                    int v = blockFace.getVCoord(direction);

                    //lock.writeLock().lock();
                    try {
                        if (blockFace instanceof GreedyBlockFace greedyBlockFace) {
                            int deltaU = greedyBlockFace.getDeltaU();
                            int deltaV = greedyBlockFace.getDeltaV();

                            for (int du = u; du <= deltaU; du++) {
                                for (int dv = v; dv <= deltaV; dv++) {
                                    removeBlockFace(du, dv);
                                    short idx = computeIndex(du, dv);
                                    facesInSlice.put(idx, freshId);

                                }
                            }
                        } else if (blockFace instanceof SingleBlockFace) {
                            short startIdx = computeIndex(u, v);
                            removeBlockFace(u, v);
                            facesInSlice.put(startIdx, freshId);
                        }


                        floatCount.addAndGet(blockFace.getFloatsPerVertex() * blockFace.getVerticesPerFace());
                        indexCount.addAndGet(blockFace.getIndicesPerFace());
                        sizeCount.incrementAndGet();
                        blockFaces.put(freshId, blockFace);

                    } finally {
                        //lock.writeLock().unlock();
                    }
                }

                public void removeBlockFace(int u, int v) {
                    //lock.writeLock().lock();
                    try {
                        short idx = computeIndex(u, v);

                        if (!facesInSlice.containsKey(idx)) {
                            return;
                        }
                        short prev = facesInSlice.remove(idx);

                        if (prev != Short.MIN_VALUE) {
                            BlockFace old = blockFaces.remove(prev);

                            if (old instanceof GreedyBlockFace g) {
                                int u0 = g.getUCoord(direction), v0 = g.getVCoord(direction);
                                for (int du = u0; du <= g.getDeltaU(); du++) {
                                    for (int dv = v0; dv <= g.getDeltaV(); dv++) {
                                        facesInSlice.remove(computeIndex(du, dv));
                                    }
                                }
                            }

                            floatCount.addAndGet(-old.getFloatsPerVertex() * old.getVerticesPerFace());
                            indexCount.addAndGet(-old.getIndicesPerFace());
                            sizeCount.decrementAndGet();
                        }
                    } finally {
                        //lock.writeLock().unlock();
                    }
                }

                private short computeIndex(int u, int v) {
                    return (short) (u + getSizeU(direction) * v);
                }


                private int extractU(short packed) {
                    int idx = packed & 0xFFFF;
                    int sizeU = getSizeU(direction);
                    return idx % sizeU;
                }

                private int extractV(short packed) {
                    int idx = packed & 0xFFFF;
                    int sizeU = getSizeU(direction);
                    return idx / sizeU;
                }

                @Override
                public Iterator<BlockFace> iterator() {
                    return blockFaces.values().iterator();
                }
            }
        }
    }

    private long computeOffsetKey(int chunkCoordinateInRegionX, int chunkCoordinateInRegionY, int chunkCoordinateInRegionZ) {
        return Chunk.computeChunkKey(chunkCoordinateInRegionX, chunkCoordinateInRegionY, chunkCoordinateInRegionZ);
    }
}
