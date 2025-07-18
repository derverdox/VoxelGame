package de.verdox.voxel.client.level.mesh.chunk.calculation;

import de.verdox.voxel.client.level.chunk.occupancy.ChunkOccupancyMask;
import de.verdox.voxel.client.level.chunk.ClientChunk;
import de.verdox.voxel.client.level.chunk.occupancy.FaceMasks;
import de.verdox.voxel.client.level.mesh.block.BlockRenderer;
import de.verdox.voxel.client.level.mesh.chunk.BlockFaceStorage;
import de.verdox.voxel.shared.data.registry.ResourceLocation;
import de.verdox.voxel.shared.data.types.Blocks;
import de.verdox.voxel.shared.level.block.BlockBase;
import de.verdox.voxel.shared.util.Direction;

public class BitOcclusionBasedChunkMeshCalculator implements ChunkMeshCalculator {

    @Override
    public BlockFaceStorage calculateChunkMesh(ClientChunk chunk) {
        if (chunk.isEmpty()) {
            return null;
        }

        int sx = chunk.getWorld().getChunkSizeX();
        int sy = chunk.getWorld().getChunkSizeY();
        int sz = chunk.getWorld().getChunkSizeZ();

        ChunkOccupancyMask com = chunk.getOccupancyMask();

        long[][] occ = com.getOccupancyMask();
        int sideMask = com.getSideOcclusionMask();


        long[][][] neighOcc = new long[6][][];
        for (int i = 0; i < Direction.values().length; i++) {
            Direction d = Direction.values()[i];
            ClientChunk nc = chunk.getWorld()
                .getChunk(chunk.getChunkX() + d.getOffsetX(),
                    chunk.getChunkY() + d.getOffsetY(),
                    chunk.getChunkZ() + d.getOffsetZ());
            if (nc != null) {
                neighOcc[i] = nc.getOccupancyMask().getOccupancyMask();
            } else {
                neighOcc[i] = null;
            }
        }

        BlockFaceStorage result = new BlockFaceStorage(sx, sy, sz);

        // 3) Main mesh loop with early‐outs and inline bit‐ops
        for (int dirId = 0; dirId < Direction.values().length; dirId++) {
            // skip entire face‐direction if fully occluded
            if ((sideMask & (1 << dirId)) != 0) continue;

            Direction d = Direction.values()[dirId];
            long[][] nOcc = neighOcc[dirId];
            int dx = d.getOffsetX(), dy = d.getOffsetY(), dz = d.getOffsetZ();

            for (int x = 0; x < sx; x++) {
                for (int y = 0; y < sy; y++) {
                    long col = occ[x][y];
                    long bits;

                    // inline compute mask for this direction
                    switch (d) {
                        case EAST:  // +X
                            bits = col & ~( (x+1 < sx ? occ[x+1][y] : (nOcc!=null? nOcc[0][y]: 0L)) );
                            break;
                        case WEST:  // -X
                            bits = col & ~( (x-1 >=0 ? occ[x-1][y] : (nOcc!=null? nOcc[sx-1][y]: 0L)) );
                            break;
                        case UP:    // +Y
                            bits = col & ~( (y+1 < sy ? occ[x][y+1] : (nOcc!=null? nOcc[x][0]: 0L)) );
                            break;
                        case DOWN:  // -Y
                            bits = col & ~( (y-1 >=0 ? occ[x][y-1] : (nOcc!=null? nOcc[x][sy-1]: 0L)) );
                            break;
                        case SOUTH: // +Z
                            bits = col & ~(col >>> 1);
                            break;
                        case NORTH: // -Z
                            bits = col & ~(col << 1);
                            break;
                        default:
                            bits = 0L;
                    }

                    // emit faces for each set bit in bits
                    while (bits != 0L) {
                        int z = Long.numberOfTrailingZeros(bits);
                        bits &= ~(1L << z);

                        // boundary neighbor check: if at chunk edge, consult nOcc
                        if ((x + dx < 0 || x + dx >= sx ||
                            y + dy < 0 || y + dy >= sy ||
                            z + dz < 0 || z + dz >= sz)
                            && (nOcc == null
                            || ((nOcc[
                            Math.floorMod(x+dx, sx)
                            ][
                            Math.floorMod(y+dy, sy)
                            ] >>> Math.floorMod(z+dz, sz)) & 1L) != 0L)) {
                            continue;
                        }

                        // actually add face
                        BlockBase block = chunk.getBlockAt(x, y, z);
                        var faces = block.getModel()
                            .getBlockModelType()
                            .findByNormal(dx, dy, dz);
                        for (var face : faces) {
                            ResourceLocation name = block == Blocks.AIR ? null
                                : block.getModel().getTextureOfFace(
                                block.getModel()
                                    .getBlockModelType()
                                    .getNameOfFace(face));
                            result.addFace(BlockRenderer.generateBlockFace(name, face, x, y, z));
                        }
                    }
                }
            }
        }
        return result;
    }

    protected void calculateFacesForCoordinates(ClientChunk chunk, BlockFaceStorage result, ChunkOccupancyMask[] neighborMasks, Direction faceDir, int localX, int localY, long occlusionBitsAtCoordinates, int chunkSizeX, int chunkSizeY, int chunkSizeZ) {
        while (occlusionBitsAtCoordinates != 0L) {
            int localZ = Long.numberOfTrailingZeros(occlusionBitsAtCoordinates);
            occlusionBitsAtCoordinates &= ~(1L << localZ);

            boolean atBoundary = isAtBoundary(faceDir.getId(), localX, chunkSizeX, localY, chunkSizeY, localZ, chunkSizeZ);

            if (atBoundary) {
                ChunkOccupancyMask neighborMaskForDirection = neighborMasks[faceDir.getId()];

                int localXInNeighborChunk = (localX + faceDir.getOffsetX() + chunkSizeX) % chunkSizeX;
                int localYInNeighborChunk = (localY + faceDir.getOffsetY() + chunkSizeY) % chunkSizeY;
                int localZInNeighborChunk = (localZ + faceDir.getOffsetZ() + chunkSizeZ) % chunkSizeZ;

                // If the chunk is not loaded we treat it as fully occluded.
                if (neighborMaskForDirection == null || neighborMaskForDirection.isOpaque(localXInNeighborChunk, localYInNeighborChunk, localZInNeighborChunk)) {
                    continue;
                }
            }

            BlockBase block = chunk.getBlockAt(localX, localY, localZ);

            var faces = block.getModel().getBlockModelType().findByNormal(faceDir.getOffsetX(), faceDir.getOffsetY(), faceDir.getOffsetZ());
            for (var face : faces) {
                ResourceLocation tex = block == Blocks.AIR ? null : block.getModel().getTextureOfFace(block.getModel().getBlockModelType().getNameOfFace(face));
                result.addFace(BlockRenderer.generateBlockFace(tex, face, localX, localY, localZ));
            }
        }
    }

    private boolean isAtBoundary(int faceDir, int localX, int chunkSizeX, int localY, int chunkSizeY, int localZ, int chunkSizeZ) {
        return (faceDir == 0 && localX == 0)
            || (faceDir == 1 && localX == chunkSizeX - 1)
            || (faceDir == 2 && localY == 0)
            || (faceDir == 3 && localY == chunkSizeY - 1)
            || (faceDir == 4 && localZ == 0)
            || (faceDir == 5 && localZ == chunkSizeZ - 1);
    }

    private static void extractNeighborOccupancyMask(ClientChunk chunk, Direction faceDir, ChunkOccupancyMask[] neighborMasks) {
        int relativeChunkX = chunk.getChunkX() + faceDir.getOffsetX();
        int relativeChunkY = chunk.getChunkY() + faceDir.getOffsetY();
        int relativeChunkZ = chunk.getChunkZ() + faceDir.getOffsetZ();
        ClientChunk relativeChunk = chunk.getWorld().getChunk(relativeChunkX, relativeChunkY, relativeChunkZ);
        if (relativeChunk != null) {
            relativeChunk.getOccupancyMask().initFromChunk(relativeChunk);
            neighborMasks[faceDir.getId()] = relativeChunk.getOccupancyMask();
        }
    }
}
