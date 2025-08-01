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

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

public class BitOcclusionBasedChunkMeshCalculator implements ChunkMeshCalculator {
    @Override
    public void calculateChunkMesh(TerrainFaceStorage.ChunkFaceStorage blockFaces, ClientChunk chunkBase, int lodLevel) {
        if (chunkBase.isEmpty()) {
            return;
        }

        if (!chunkBase.getWorld().hasNeighborsToAllSides(chunkBase)) {
            throw new IllegalArgumentException("Can only create a chunk mesh for a chunk that has all neighbors");
        }

        long start = System.nanoTime();

        ChunkBase<?> lookupChunk;

        if (lodLevel == 0) {
            lookupChunk = chunkBase;
        } else {
            lookupChunk = chunkBase.getLodChunk(1);
        }

        int sx = lookupChunk.getBlockSizeX();
        int sy = lookupChunk.getBlockSizeY();
        int sz = lookupChunk.getBlockSizeZ();


        OccupancyMask occupancyMask = lodLevel == 0 ? chunkBase.getChunkOccupancyMask() : chunkBase
                .getLodChunk(lodLevel).getChunkOccupancyMask();

        OccupancyMask[] neighOcc = new OccupancyMask[6];
        for (int i = 0; i < Direction.values().length; i++) {
            Direction d = Direction.values()[i];
            ClientChunk nc = chunkBase.getWorld().getChunkNeighborNow(chunkBase, d);
            if (nc != null) {
                neighOcc[i] = lodLevel == 0 ? nc.getChunkOccupancyMask() : nc.getLodChunk(lodLevel)
                                                                             .getChunkOccupancyMask();
            } else {
                // This only happens on view distance edges
                neighOcc[i] = null;
            }
        }


        CompletableFuture[] futures = new CompletableFuture[Direction.values().length];

        for (int dirId = 0; dirId < Direction.values().length; dirId++) {
            //if ((occupancyMask.getSideMask() & (1L << dirId)) != 0) continue;

            Direction d = Direction.values()[dirId];
            OccupancyMask neighborOcclusionMap = neighOcc[dirId];

            if (neighborOcclusionMap != null) {
                //generateBlockFacesForDirection(blockFaces, (byte) lodLevel, d, sx, sy, occupancyMask, neighborOcclusionMap, sz, lookupChunk);
                futures[dirId] = CompletableFuture.runAsync(() -> generateBlockFacesForDirection(blockFaces, (byte) lodLevel, d, sx, sy, occupancyMask, neighborOcclusionMap, sz, lookupChunk));
            }
            else {
                futures[dirId] = CompletableFuture.completedFuture(null);
            }
        }

        CompletableFuture.allOf(futures).join();
        long duration = System.nanoTime() - start;
        chunkCalculatorThroughput.add(duration);
    }

    private static void generateBlockFacesForDirection(TerrainFaceStorage.ChunkFaceStorage blockFaces, byte lodLevel, Direction d, int sx, int sy, OccupancyMask occupancyMask, OccupancyMask neighborOcclusionMap, int sz, ChunkBase<?> lookupChunk) {
        int dx = d.getOffsetX(), dy = d.getOffsetY(), dz = d.getOffsetZ();

        Objects.requireNonNull(neighborOcclusionMap, "Can only create a chunk mesh for a chunk that has all neighbors");

        for (int lightLookupX = 0; lightLookupX < sx; lightLookupX += 1) {
            for (int lightLookupY = 0; lightLookupY < sy; lightLookupY += 1) {
                long zColumn = occupancyMask.getZColumn(lightLookupX, lightLookupY);

                long bits = switch (d) {
                    case EAST ->
                            zColumn & ~(lightLookupX + 1 < sx ? occupancyMask.getZColumn(lightLookupX + 1, lightLookupY) : neighborOcclusionMap.getZColumn(0, lightLookupY));
                    case WEST ->
                            zColumn & ~(lightLookupX - 1 >= 0 ? occupancyMask.getZColumn(lightLookupX - 1, lightLookupY) : neighborOcclusionMap.getZColumn(sx - 1, lightLookupY));
                    case UP ->
                            zColumn & ~(lightLookupY + 1 < sy ? occupancyMask.getZColumn(lightLookupX, lightLookupY + 1) : neighborOcclusionMap.getZColumn(lightLookupX, 0));
                    case DOWN ->
                            zColumn & ~(lightLookupY - 1 >= 0 ? occupancyMask.getZColumn(lightLookupX, lightLookupY - 1) : neighborOcclusionMap.getZColumn(lightLookupX, sy - 1));
                    case SOUTH -> zColumn & ~(zColumn >>> 1);
                    case NORTH -> zColumn & ~(zColumn << 1);
                };

                while (bits != 0L) {
                    int lightLookupZ = Long.numberOfTrailingZeros(bits);
                    bits &= ~(1L << lightLookupZ);

                    if ((lightLookupX + dx < 0 ||
                            lightLookupX + dx >= sx ||
                            lightLookupY + dy < 0 ||
                            lightLookupY + dy >= sy ||
                            lightLookupZ + dz < 0 ||
                            lightLookupZ + dz >= sz)) {

                        long neighborColumn = neighborOcclusionMap.getZColumn(lookupChunk.localX(lightLookupX + dx), lookupChunk.localY(lightLookupY + dy));
                        boolean isOccludedByNeighbor = (neighborColumn >>> lookupChunk.localZ(lightLookupZ + dz) & 1L) != 0L;
                        if (isOccludedByNeighbor) {

                            int globalX = lookupChunk.globalX(lightLookupX);
                            int globalY = lookupChunk.globalY(lightLookupY);
                            int globalZ = lookupChunk.globalZ(lightLookupZ);

                            if(globalX == -23 && globalY == 66 && globalZ == -60) {
                                System.out.println("Block at: "+lookupChunk.globalX(lightLookupX)+", "+lookupChunk.globalY(lightLookupY)+", "+lookupChunk.globalZ(lightLookupZ)+" is occluded by neighbor at "+d.name());
                            }
                            continue;
                        }
                    }

                    BlockBase block = lookupChunk.getBlockAt(lightLookupX, lightLookupY, lightLookupZ);
                    var faces = block.getModel().getBlockModelType().findByNormal(dx, dy, dz);


                    if (block.equals(Blocks.AIR)) {
                        throw new IllegalStateException("The occupancy mask produced a coordinate with an AIR block");
                    }

                    for (var face : faces) {
                        ResourceLocation name = block.getModel().getTextureOfFace(
                                block.getModel()
                                     .getBlockModelType()
                                     .getNameOfFace(face));

                        blockFaces.generateFace(lookupChunk, name, face,
                                lodLevel,
                                lightLookupX,
                                lightLookupY,
                                lightLookupZ);
                    }
                }
            }
        }
    }
}
