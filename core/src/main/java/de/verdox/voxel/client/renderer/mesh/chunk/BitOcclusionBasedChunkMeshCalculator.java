package de.verdox.voxel.client.renderer.mesh.chunk;

import de.verdox.voxel.client.level.TerrainManager;
import de.verdox.voxel.client.level.chunk.occupancy.OccupancyMask;
import de.verdox.voxel.client.renderer.mesh.BlockRenderer;
import de.verdox.voxel.client.level.chunk.RenderableChunk;
import de.verdox.voxel.client.level.chunk.TerrainChunk;
import de.verdox.voxel.client.renderer.terrain.regions.RegionalizedTerrainManager;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.util.Direction;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class BitOcclusionBasedChunkMeshCalculator implements ChunkMeshCalculator {
    @Override
    public void calculateChunkMesh(TerrainChunk chunk, int lodLevel) {
        long start = System.nanoTime();
        if (chunk.isEmpty()) {
            return;
        }

        if (!chunk.hasNeighborsToAllSides()) {
            throw new IllegalArgumentException("Can only create a chunk mesh for a chunk that has all neighbors");
        }

        RenderableChunk lookupChunk;

        if (lodLevel == 0) {
            lookupChunk = chunk;
        } else {
            lookupChunk = chunk.getLodChunk(lodLevel);
        }

        lookupChunk.getChunkProtoMesh().clear();

        int sx = lookupChunk.getSizeX();
        int sy = lookupChunk.getSizeY();
        int sz = lookupChunk.getSizeZ();


        OccupancyMask occupancyMask = lookupChunk.getChunkOccupancyMask();

        OccupancyMask[] neighOcc = new OccupancyMask[6];
        for (int i = 0; i < Direction.values().length; i++) {
            Direction d = Direction.values()[i];
            TerrainChunk nc = chunk.getNeighborChunk(d);
            if (nc != null) {
                neighOcc[i] = lodLevel == 0 ? nc.getChunkOccupancyMask() : nc.getLodChunk(lodLevel).getChunkOccupancyMask();
            } else {
                // This only happens on view distance edges
                neighOcc[i] = null;
            }
        }


        CompletableFuture[] futures = new CompletableFuture[Direction.values().length];
        boolean calculatedAnything = false;

        for (int dirId = 0; dirId < Direction.values().length; dirId++) {
            //if ((occupancyMask.getSideMask() & (1L << dirId)) != 0) continue;

            Direction d = Direction.values()[dirId];
            OccupancyMask neighborOcclusionMap = neighOcc[dirId];

            if (neighborOcclusionMap != null) {
                //generateBlockFacesForDirection(blockFaces, (byte) lodLevel, d, sx, sy, occupancyMask, neighborOcclusionMap, sz, lookupChunk);
                generateBlockFacesForDirection(chunk.getTerrainManager(), (byte) lodLevel, d, sx, sy, occupancyMask, neighborOcclusionMap, sz, lookupChunk);
                futures[dirId] = CompletableFuture.completedFuture(null);
                calculatedAnything = true;
                //futures[dirId] = CompletableFuture.runAsync(() -> generateBlockFacesForDirection(chunk.getTerrainManager(), blockFaces, (byte) lodLevel, d, sx, sy, occupancyMask, neighborOcclusionMap, sz, lookupChunk), service2);
            } else {
                futures[dirId] = CompletableFuture.completedFuture(null);
            }
        }

        CompletableFuture.allOf(futures).join();
        if(calculatedAnything) {
            long duration = System.nanoTime() - start;
            chunkCalculatorThroughput.add(duration);
        }
    }

    private static void generateBlockFacesForDirection(TerrainManager terrainManager, byte lodLevel, Direction d, int sx, int sy, OccupancyMask occupancyMask, OccupancyMask neighborOcclusionMap, int sz, RenderableChunk lookupChunk) {
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

                        BlockRenderer.saveBlockFaceToProtoMesh(terrainManager,
                                lookupChunk, face,
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
