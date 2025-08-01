package de.verdox.voxel.client.level.mesh.chunk.calculation;

import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.client.level.mesh.block.TerrainFaceStorage;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.level.chunk.ChunkBase;
import de.verdox.voxel.shared.util.Benchmark;
import de.verdox.voxel.shared.util.Direction;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class BitOcclusionBasedChunkMeshCalculator implements ChunkMeshCalculator {
    private static final long startTime = System.nanoTime();
    private static final AtomicLong chunksProcessed = new AtomicLong();

    @Override
    public void calculateChunkMesh(TerrainFaceStorage.ChunkFaceStorage blockFaces, ClientChunk chunkBase, int lodLevel) {
        if (chunkBase.isEmpty()) {
            return;
        }
        long start = System.nanoTime();

        ChunkBase<?> lookupChunk;
        int sx;
        int sy;
        int sz;

        if(lodLevel == 0) {
            lookupChunk = chunkBase;
            sx = chunkBase.getBlockSizeX();
            sy = chunkBase.getBlockSizeY();
            sz = chunkBase.getBlockSizeZ();
        }
        else {
            lookupChunk = chunkBase.getLodChunk(1);
            sx = lookupChunk.getBlockSizeX();
            sy = lookupChunk.getBlockSizeY();
            sz = lookupChunk.getBlockSizeZ();
        }


        OccupancyMask occupancyMask = lodLevel == 0 ? chunkBase.getChunkOccupancyMask() : chunkBase.getLodChunk(lodLevel).getChunkOccupancyMask();

        OccupancyMask[] neighOcc = new OccupancyMask[6];
        for (int i = 0; i < Direction.values().length; i++) {
            Direction d = Direction.values()[i];
            ClientChunk nc = chunkBase.getWorld().getChunk(chunkBase.getChunkX() + d.getOffsetX(), chunkBase.getChunkY() + d.getOffsetY(), chunkBase.getChunkZ() + d.getOffsetZ());
            if (nc != null) {
                neighOcc[i] = lodLevel == 0 ? nc.getChunkOccupancyMask() : nc.getLodChunk(lodLevel).getChunkOccupancyMask();
            } else {
                neighOcc[i] = null;
            }
        }

        // 3) Main mesh loop with early‐outs and inline bit‐ops

        CompletableFuture[] futures = new CompletableFuture[Direction.values().length];

        for (int dirId = 0; dirId < Direction.values().length; dirId++) {
            // skip entire face‐direction if fully occluded
            //if ((occupancyMask.getSideMask() & (1L << dirId)) != 0) continue;

            Direction d = Direction.values()[dirId];
            OccupancyMask neighborOcclusionMap = neighOcc[dirId];

            futures[dirId] = CompletableFuture.runAsync(() -> {

                int dx = d.getOffsetX(), dy = d.getOffsetY(), dz = d.getOffsetZ();

                for (int lightLookupX = 0; lightLookupX < sx; lightLookupX += 1) {
                    for (int lightLookupY = 0; lightLookupY < sy; lightLookupY += 1) {
                        long zColumn = occupancyMask.getZColumn(lightLookupX, lightLookupY);

                        long bits = switch (d) {
                            case EAST ->
                                    zColumn & ~((lightLookupX + 1 < sx ? occupancyMask.getZColumn(lightLookupX + 1, lightLookupY) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(0, lightLookupY) : 0L)));
                            case WEST ->
                                    zColumn & ~((lightLookupX - 1 >= 0 ? occupancyMask.getZColumn(lightLookupX - 1, lightLookupY) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(sx - 1, lightLookupY) : 0L)));
                            case UP ->
                                    zColumn & ~((lightLookupY + 1 < sy ? occupancyMask.getZColumn(lightLookupX, lightLookupY + 1) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(lightLookupX, 0) : 0L)));
                            case DOWN ->
                                    zColumn & ~((lightLookupY - 1 >= 0 ? occupancyMask.getZColumn(lightLookupX, lightLookupY - 1) : (neighborOcclusionMap != null ? neighborOcclusionMap.getZColumn(lightLookupX, sy - 1) : 0L)));
                            case SOUTH -> zColumn & ~(zColumn >>> 1);
                            case NORTH -> zColumn & ~(zColumn << 1);
                        };

                        // emit lod0 for each set bit in bits

                        while (bits != 0L) {
                            int lightLookupZ = Long.numberOfTrailingZeros(bits);
                            bits &= ~(1L << lightLookupZ);

                            // boundary neighbor check: if at chunk edge, consult nOcc
                            if ((lightLookupX + dx < 0 ||
                                    lightLookupX + dx >= sx ||
                                    lightLookupY + dy < 0 ||
                                    lightLookupY + dy >= sy ||
                                    lightLookupZ + dz < 0 ||
                                    lightLookupZ + dz >= sz)
                                    && (neighborOcclusionMap == null ||
                                    ((neighborOcclusionMap.getZColumn(lookupChunk.localX(lightLookupX + dx), lookupChunk.localY(lightLookupY + dy))
                                            >>> lookupChunk.localZ(lightLookupZ + dz)) & 1L) != 0L)) {
                                continue;
                            }

                            // actually add face
                            BlockBase block = lookupChunk.getBlockAt(lightLookupX, lightLookupY, lightLookupZ);
                            var faces = block.getModel().getBlockModelType().findByNormal(dx, dy, dz);


                            if (block.equals(Blocks.AIR)) {
                                continue;
                            }

                            for (var face : faces) {
                                ResourceLocation name = block.getModel().getTextureOfFace(
                                        block.getModel()
                                             .getBlockModelType()
                                             .getNameOfFace(face));

                                blockFaces.generateFace(lookupChunk, name, face,
                                        (byte) lodLevel,
                                        lightLookupX,
                                        lightLookupY,
                                        lightLookupZ);
                            }
                        }
                    }
                }
            });
        }

        CompletableFuture.allOf(futures).join();
        long duration = System.nanoTime() - start;
        chunkCalculatorThroughput.add(duration);
    }
}
